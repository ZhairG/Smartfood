/**
 * Nombre: emailChangeService.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Cambio de correo con código (spec 008, T010, CA-PER-02-10 a 02-12). El usuario con sesión pide un
 * código para su correo nuevo; al confirmarlo, la cuenta y el perfil cambian al instante, sin cerrar la sesión. No
 * conoce Firebase: usa los puertos de `ports.ts` y los mismos límites y ayudantes de la recuperación (spec 003).
 */

import { randomBytes } from "node:crypto";
import { AccountEmails, Clock, EmailChangeMailer, EmailChangeRecord, EmailChangeStore } from "./ports.js";
import {
  CODE_REGEX,
  EMAIL_REGEX,
  RecoveryLimits,
  hashCode,
  normalizeEmail,
  randomCode,
  sameHash,
  toSeconds,
  waitBeforeNextCode,
} from "./recoveryService.js";

/** Razones de error del contrato. La app las clasifica en `FirebaseErrorMapper.mapFunctionsError`. */
export type EmailChangeErrorReason =
  | "INVALID_EMAIL"
  | "SAME_EMAIL"
  | "EMAIL_IN_USE"
  | "REQUIRES_RECENT_LOGIN"
  | "INVALID_CODE"
  | "CODE_EXPIRED"
  | "TOO_MANY_ATTEMPTS"
  | "TOO_MANY_REQUESTS";

/**
 * Falla de negocio del cambio de correo.
 *
 * @param reason Razón del contrato.
 * @param retryAfterSeconds Solo en `TOO_MANY_REQUESTS`: segundos que faltan para poder pedir otro código.
 */
export class EmailChangeError extends Error {
  constructor(
    readonly reason: EmailChangeErrorReason,
    readonly retryAfterSeconds?: number,
  ) {
    super(reason);
    this.name = "EmailChangeError";
  }
}

/** La app confirma la contraseña justo antes de pedir el código; un inicio de sesión más viejo no vale. */
export const RECENT_LOGIN_MS = 5 * 60 * 1000;

export class EmailChangeService {
  constructor(
    private readonly store: EmailChangeStore,
    private readonly accounts: AccountEmails,
    private readonly mailer: EmailChangeMailer,
    private readonly now: Clock = Date.now,
    private readonly generateCode: () => string = randomCode,
  ) {}

  /**
   * Envía un código al correo nuevo (CA-PER-02-10).
   *
   * @param uid Cuenta con sesión.
   * @param authTimeMs Hora del último inicio de sesión o reautenticación, en epoch ms.
   * @param rawEmail Correo nuevo tal como se escribió.
   * @return Segundos que faltan para poder pedir otro código.
   */
  async requestCode(uid: string, authTimeMs: number, rawEmail: string): Promise<number> {
    const now = this.now();
    if (now - authTimeMs > RECENT_LOGIN_MS) throw new EmailChangeError("REQUIRES_RECENT_LOGIN");
    const email = normalizeEmail(rawEmail);
    if (!EMAIL_REGEX.test(email)) throw new EmailChangeError("INVALID_EMAIL");
    const current = await this.accounts.currentEmail(uid);
    if (current !== null && normalizeEmail(current) === email) throw new EmailChangeError("SAME_EMAIL");
    // Solo quien tiene sesión y confirmó su contraseña llega aquí, así que decirlo no expone cuentas (CA-PER-02-11).
    if ((await this.accounts.findUidByEmail(email)) !== null) throw new EmailChangeError("EMAIL_IN_USE");

    const code = this.generateCode();
    const salt = randomBytes(16).toString("hex");
    const outcome = await this.store.update<CodeRequest>(uid, (previous) => {
      const waitMs = waitBeforeNextCode(previous, now);
      if (waitMs > 0) return { result: { retryAfterMs: waitMs } };

      const sameWindow = previous !== null && now - previous.windowStart < RecoveryLimits.WINDOW_MS;
      const next: EmailChangeRecord = {
        newEmail: email,
        codeHash: hashCode(salt, code),
        salt,
        expiresAt: now + RecoveryLimits.CODE_TTL_MS,
        attemptsLeft: RecoveryLimits.MAX_ATTEMPTS,
        lastSentAt: now,
        windowStart: sameWindow ? previous.windowStart : now,
        sentInWindow: (sameWindow ? previous.sentInWindow : 0) + 1,
      };
      return { next, result: { previous, written: next } };
    });

    if ("retryAfterMs" in outcome) throw new EmailChangeError("TOO_MANY_REQUESTS", toSeconds(outcome.retryAfterMs));
    try {
      await this.mailer.sendEmailChangeCode(email, code);
    } catch (error) {
      // Si el envío falla, el intento no cuenta para los límites (como CA-REC-03-07).
      await this.store.update(uid, (current) =>
        current?.codeHash === outcome.written.codeHash ? { next: outcome.previous, result: undefined } : { result: undefined },
      );
      throw error;
    }
    return toSeconds(waitBeforeNextCode(outcome.written, now));
  }

  /**
   * Comprueba el código y cambia el correo de la cuenta y del perfil (CA-PER-02-10 a 02-12).
   *
   * @return Correo nuevo de la cuenta.
   */
  async confirm(uid: string, rawCode: string): Promise<string> {
    const code = rawCode.trim();
    if (!CODE_REGEX.test(code)) throw new EmailChangeError("INVALID_CODE");
    const now = this.now();

    const outcome = await this.store.update<CodeCheck>(uid, (current) => {
      if (!current || current.codeHash === null || current.salt === null) return { result: { error: "INVALID_CODE" } };
      if (now > current.expiresAt) return { result: { error: "CODE_EXPIRED" } };
      if (current.attemptsLeft <= 0) return { result: { error: "TOO_MANY_ATTEMPTS" } };
      if (sameHash(current.codeHash, hashCode(current.salt, code))) return { result: { email: current.newEmail } };

      // Al agotar los intentos, el código se invalida y ni el correcto vale ya.
      const attemptsLeft = current.attemptsLeft - 1;
      const next: EmailChangeRecord =
        attemptsLeft > 0 ? { ...current, attemptsLeft } : { ...current, attemptsLeft, codeHash: null, salt: null };
      return { next, result: { error: attemptsLeft > 0 ? "INVALID_CODE" : "TOO_MANY_ATTEMPTS" } };
    });

    if ("error" in outcome) throw new EmailChangeError(outcome.error);
    // Otra cuenta pudo registrarse con ese correo mientras tanto.
    if (!(await this.accounts.updateEmail(uid, outcome.email))) throw new EmailChangeError("EMAIL_IN_USE");
    await this.accounts.setProfileEmail(uid, outcome.email);
    await this.store.delete(uid);
    return outcome.email;
  }
}

type CodeWritten = { previous: EmailChangeRecord | null; written: EmailChangeRecord };
type CodeRequest = CodeWritten | { retryAfterMs: number };
type CodeCheck = { email: string } | { error: EmailChangeErrorReason };
