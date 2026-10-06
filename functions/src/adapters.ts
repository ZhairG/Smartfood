/**
 * Nombre: adapters.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementaciones de los puertos de `ports.ts` con Firebase y nodemailer (spec 003, plan §2).
 * Es el único archivo del servidor que conoce Firestore, el Admin SDK de Auth y el SMTP de Gmail.
 */

import * as logger from "firebase-functions/logger";
import type { Auth } from "firebase-admin/auth";
import type { Firestore } from "firebase-admin/firestore";
import nodemailer from "nodemailer";
import { Mailer, ResetRecord, ResetStore, StoreUpdate, UserDirectory } from "./ports.js";

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

/** Cuentas de Firebase Auth a través del Admin SDK. */
export class AdminUserDirectory implements UserDirectory {
  constructor(private readonly auth: Auth) {}

  async findUidByEmail(email: string): Promise<string | null> {
    try {
      return (await this.auth.getUserByEmail(email)).uid;
    } catch (error) {
      if ((error as { code?: string }).code === "auth/user-not-found") return null;
      throw error;
    }
  }

  async updatePassword(uid: string, newPassword: string): Promise<void> {
    await this.auth.updateUser(uid, { password: newPassword });
  }

  async revokeSessions(uid: string): Promise<void> {
    await this.auth.revokeRefreshTokens(uid);
  }
}

/** Envío por Gmail SMTP con una contraseña de aplicación (ADR-REC-04). */
export class GmailMailer implements Mailer {
  constructor(
    private readonly user: string,
    private readonly appPassword: string,
  ) {}

  async sendRecoveryCode(email: string, code: string): Promise<void> {
    const transport = nodemailer.createTransport({
      service: "gmail",
      auth: { user: this.user, pass: this.appPassword },
    });
    await transport.sendMail({
      from: `"SmartFood" <${this.user}>`,
      to: email,
      subject: `Tu código de SmartFood es ${code}`,
      text:
        `Tu código para restablecer la contraseña de SmartFood es ${code}.\n\n` +
        "Vence en 10 minutos. Si no lo pediste, ignora este correo: tu contraseña no cambiará.",
      html: recoveryEmailHtml(code),
    });
  }
}

/** En la Emulator Suite el código se lee en el log en lugar de enviarse (RNF-REC-04). */
export class LoggingMailer implements Mailer {
  async sendRecoveryCode(email: string, code: string): Promise<void> {
    logger.info(`[EMULADOR] Código de recuperación para ${email}: ${code}`);
  }
}

function recoveryEmailHtml(code: string): string {
  return `<!doctype html>
<html lang="es">
  <body style="margin:0;padding:24px;background:#f4f7f2;font-family:Arial,Helvetica,sans-serif;color:#1f2a1c">
    <div style="max-width:420px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;text-align:center">
      <h1 style="margin:0 0 8px;font-size:22px;color:#2e7d32">SmartFood</h1>
      <p style="margin:0 0 24px;font-size:15px">Usa este código para restablecer tu contraseña:</p>
      <p style="margin:0 0 24px;font-size:36px;font-weight:bold;letter-spacing:12px">${code}</p>
      <p style="margin:0;font-size:13px;color:#5f6b5a">
        Vence en 10 minutos. Si no lo pediste, ignora este correo: tu contraseña no cambiará.
      </p>
    </div>
  </body>
</html>`;
}
