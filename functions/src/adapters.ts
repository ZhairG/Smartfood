/**
 * Nombre: adapters.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementaciones de los puertos de `ports.ts` con Firebase y nodemailer (spec 003, plan §2; spec 004).
 * Es el único archivo del servidor que conoce Firestore, el Admin SDK de Auth y el SMTP de Gmail.
 */

import * as logger from "firebase-functions/logger";
import type { Auth } from "firebase-admin/auth";
import { FieldValue, type Firestore } from "firebase-admin/firestore";
import nodemailer from "nodemailer";
import { EmailContent, emailChangeEmail, recoveryEmail, welcomeEmail } from "./emailTemplates.js";
import {
  AccountEmails,
  EmailChangeMailer,
  EmailChangeRecord,
  EmailChangeStore,
  Mailer,
  RegisteredAccount,
  RegisteredAccounts,
  ResetRecord,
  ResetStore,
  StoreUpdate,
  UserDirectory,
  WelcomeLog,
  WelcomeMailer,
} from "./ports.js";

/** Colección privada de la recuperación; las reglas niegan todo acceso de los clientes (RNF-REC-02). */
const COLLECTION = "passwordResets";

/** Registros en Firestore, modificados dentro de transacciones (plan §4). */
export class FirestoreResetStore implements ResetStore {
  constructor(private readonly db: Firestore) {}

  update<T>(key: string, change: (current: ResetRecord | null) => StoreUpdate<T>): Promise<T> {
    const ref = this.db.collection(COLLECTION).doc(key);
    return this.db.runTransaction(async (tx) => {
      const snapshot = await tx.get(ref);
      const { next, result } = change(snapshot.exists ? (snapshot.data() as ResetRecord) : null);
      if (next === null) tx.delete(ref);
      else if (next !== undefined) tx.set(ref, next);
      return result;
    });
  }

  async delete(key: string): Promise<void> {
    await this.db.collection(COLLECTION).doc(key).delete();
  }
}

/** Colección privada del cambio de correo (spec 008); las reglas niegan todo acceso de los clientes. */
const EMAIL_CHANGES = "emailChanges";

/** Cambios de correo en Firestore, modificados dentro de transacciones. */
export class FirestoreEmailChangeStore implements EmailChangeStore {
  constructor(private readonly db: Firestore) {}

  update<T>(uid: string, change: (current: EmailChangeRecord | null) => StoreUpdate<T, EmailChangeRecord>): Promise<T> {
    const ref = this.db.collection(EMAIL_CHANGES).doc(uid);
    return this.db.runTransaction(async (tx) => {
      const snapshot = await tx.get(ref);
      const { next, result } = change(snapshot.exists ? (snapshot.data() as EmailChangeRecord) : null);
      if (next === null) tx.delete(ref);
      else if (next !== undefined) tx.set(ref, next);
      return result;
    });
  }

  async delete(uid: string): Promise<void> {
    await this.db.collection(EMAIL_CHANGES).doc(uid).delete();
  }
}

/** Código del Admin SDK cuando otra cuenta ya tiene ese correo. */
const EMAIL_EXISTS = "auth/email-already-exists";

/** Correos de la cuenta (Admin SDK) y del perfil (`users/{uid}`), spec 008. */
export class AdminAccountEmails implements AccountEmails {
  private readonly directory: AdminUserDirectory;

  constructor(
    private readonly auth: Auth,
    private readonly db: Firestore,
  ) {
    this.directory = new AdminUserDirectory(auth);
  }

  findUidByEmail(email: string): Promise<string | null> {
    return this.directory.findUidByEmail(email);
  }

  async currentEmail(uid: string): Promise<string | null> {
    return (await this.directory.find(uid))?.email ?? null;
  }

  async updateEmail(uid: string, email: string): Promise<boolean> {
    try {
      // El código probó que el correo es de la persona: queda verificado.
      await this.auth.updateUser(uid, { email, emailVerified: true });
      return true;
    } catch (error) {
      if ((error as { code?: string }).code === EMAIL_EXISTS) return false;
      throw error;
    }
  }

  async setProfileEmail(uid: string, email: string): Promise<void> {
    await this.db.collection("users").doc(uid).update({ email, updatedAt: FieldValue.serverTimestamp() });
  }
}

/** Código de Firebase Auth cuando no hay una cuenta con ese correo o `uid`. */
const USER_NOT_FOUND = "auth/user-not-found";

/** Cuentas de Firebase Auth a través del Admin SDK. */
export class AdminUserDirectory implements UserDirectory, RegisteredAccounts {
  constructor(private readonly auth: Auth) {}

  async findUidByEmail(email: string): Promise<string | null> {
    try {
      return (await this.auth.getUserByEmail(email)).uid;
    } catch (error) {
      if ((error as { code?: string }).code === USER_NOT_FOUND) return null;
      throw error;
    }
  }

  async find(uid: string): Promise<RegisteredAccount | null> {
    try {
      const user = await this.auth.getUser(uid);
      return user.email ? { email: user.email, emailVerified: user.emailVerified } : null;
    } catch (error) {
      if ((error as { code?: string }).code === USER_NOT_FOUND) return null;
      throw error;
    }
  }

  verificationLink(email: string): Promise<string> {
    return this.auth.generateEmailVerificationLink(email);
  }

  async updatePassword(uid: string, newPassword: string): Promise<void> {
    await this.auth.updateUser(uid, { password: newPassword });
  }

  async revokeSessions(uid: string): Promise<void> {
    await this.auth.revokeRefreshTokens(uid);
  }
}

/** Colección privada de las bienvenidas enviadas (spec 004, RNF-MAIL-04). */
const WELCOME_COLLECTION = "welcomeEmails";

/** Código de gRPC que devuelve `create()` cuando el documento ya existe. */
const ALREADY_EXISTS = 6;

/** Registro de bienvenidas en Firestore. `create()` falla si el documento existe, así la reserva es atómica. */
export class FirestoreWelcomeLog implements WelcomeLog {
  constructor(private readonly db: Firestore) {}

  async claim(uid: string): Promise<boolean> {
    try {
      await this.db.collection(WELCOME_COLLECTION).doc(uid).create({ sentAt: FieldValue.serverTimestamp() });
      return true;
    } catch (error) {
      if ((error as { code?: number }).code === ALREADY_EXISTS) return false;
      throw error;
    }
  }

  async release(uid: string): Promise<void> {
    await this.db.collection(WELCOME_COLLECTION).doc(uid).delete();
  }
}

/**
 * Limpia las credenciales de Gmail tal como se guardaron en Secret Manager. Google muestra la contraseña de
 * aplicación en grupos ("abcd efgh ijkl mnop") y al copiarla se cuelan espacios o saltos de línea, que Gmail
 * rechaza con `535 BadCredentials`.
 */
export function gmailCredentials(user: string, appPassword: string): { user: string; pass: string } {
  return { user: user.trim(), pass: appPassword.replace(/\s+/g, "") };
}

/** Envío por Gmail SMTP con una contraseña de aplicación (ADR-REC-04). */
export class GmailMailer implements Mailer, WelcomeMailer, EmailChangeMailer {
  private readonly auth: { user: string; pass: string };

  constructor(user: string, appPassword: string) {
    this.auth = gmailCredentials(user, appPassword);
  }

  sendRecoveryCode(email: string, code: string): Promise<void> {
    return this.send(email, recoveryEmail(code));
  }

  sendWelcome(email: string, name: string, verificationLink: string): Promise<void> {
    return this.send(email, welcomeEmail(name, verificationLink));
  }

  sendEmailChangeCode(email: string, code: string): Promise<void> {
    return this.send(email, emailChangeEmail(code));
  }

  private async send(to: string, content: EmailContent): Promise<void> {
    const transport = nodemailer.createTransport({ service: "gmail", auth: this.auth });
    await transport.sendMail({ from: `"SmartFood" <${this.auth.user}>`, to, ...content });
  }
}

/** En la Emulator Suite el código y el enlace se leen en el log en lugar de enviarse (RNF-REC-04, RNF-MAIL-02). */
export class LoggingMailer implements Mailer, WelcomeMailer, EmailChangeMailer {
  async sendRecoveryCode(email: string, code: string): Promise<void> {
    logger.info(`[EMULADOR] Código de recuperación para ${email}: ${code}`);
  }

  async sendWelcome(email: string, name: string, verificationLink: string): Promise<void> {
    logger.info(`[EMULADOR] Correo de bienvenida para ${email} (${name || "sin nombre"}): ${verificationLink}`);
  }

  async sendEmailChangeCode(email: string, code: string): Promise<void> {
    logger.info(`[EMULADOR] Código de cambio de correo para ${email}: ${code}`);
  }
}

