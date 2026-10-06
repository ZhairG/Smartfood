/**
 * Nombre: recoveryService.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Reglas de la recuperación de contraseña real (spec 003, plan §5). No conoce Firebase: recibe sus
 * dependencias como puertos (`ports.ts`) y lanza `RecoveryError` con una razón que `index.ts` traduce a
 * `HttpsError`. Responde igual exista o no la cuenta (RF-REC-05).
 */

import { createHash, randomBytes, randomInt, timingSafeEqual } from "node:crypto";
import { isStrongPassword } from "./passwordPolicy.js";
import { Clock, Mailer, ResetRecord, ResetStore, UserDirectory } from "./ports.js";

/** Razones de error del contrato (plan §3). La app las clasifica en `FirebaseErrorMapper`. */
export type RecoveryErrorReason =
  | "INVALID_EMAIL"
  | "INVALID_CODE"
  | "CODE_EXPIRED"
  | "TOO_MANY_ATTEMPTS"
  | "TOO_MANY_REQUESTS"
  | "WEAK_PASSWORD";

/** Falla de negocio del flujo de recuperación. */
export class RecoveryError extends Error {
  constructor(readonly reason: RecoveryErrorReason) {
    super(reason);
    this.name = "RecoveryError";
  }
}

/** Límites del flujo (spec 003, HU-REC-03). */
export const RecoveryLimits = {
  CODE_LENGTH: 4,
  CODE_TTL_MS: 10 * 60 * 1000,
  MAX_ATTEMPTS: 5,
  RESEND_COOLDOWN_MS: 60 * 1000,
  WINDOW_MS: 60 * 60 * 1000,
  MAX_CODES_PER_WINDOW: 5,
} as const;

/** Mismo formato que `ValidateEmailUseCase` de la app. */
const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/;
const CODE_REGEX = new RegExp(`^\\d{${RecoveryLimits.CODE_LENGTH}}$`);

/** Clave del registro: hash del correo normalizado, para no guardarlo en claro como id. */
export function recordKey(email: string): string {
  return sha256Hex(normalizeEmail(email));
}

/** Servicio con los 3 pasos del flujo. */
export class RecoveryService {
  constructor(
    private readonly store: ResetStore,
    private readonly users: UserDirectory,
    private readonly mailer: Mailer,
    private readonly now: Clock = Date.now,
    private readonly generateCode: () => string = randomCode,
  ) {}

  /** Paso 1: genera y envía un código si la cuenta existe; la respuesta es igual si no existe (CA-REC-01-02). */
  async requestCode(rawEmail: string): Promise<void> {
    const email = normalizeEmail(rawEmail);
    if (!EMAIL_REGEX.test(email)) throw new RecoveryError("INVALID_EMAIL");

    const uid = await this.users.findUidByEmail(email);
    const code = this.generateCode();
    const salt = randomBytes(16).toString("hex");
    const now = this.now();

    const allowed = await this.store.update(recordKey(email), (current) => {
      if (current && now - current.lastSentAt < RecoveryLimits.RESEND_COOLDOWN_MS) return { result: false };

      const sameWindow = current !== null && now - current.windowStart < RecoveryLimits.WINDOW_MS;
      const sentInWindow = sameWindow ? current.sentInWindow : 0;
      if (sentInWindow >= RecoveryLimits.MAX_CODES_PER_WINDOW) return { result: false };

      const next: ResetRecord = {
        uid,
        codeHash: hashCode(salt, code),
        salt,
        expiresAt: now + RecoveryLimits.CODE_TTL_MS,
        attemptsLeft: RecoveryLimits.MAX_ATTEMPTS,
        lastSentAt: now,
        windowStart: sameWindow ? current.windowStart : now,
        sentInWindow: sentInWindow + 1,
      };
      return { next, result: true };
    });

    // El límite se aplica igual exista o no la cuenta (CA-REC-03-05).
    if (!allowed) throw new RecoveryError("TOO_MANY_REQUESTS");
    if (uid !== null) await this.mailer.sendRecoveryCode(email, code);
  }

  /** Paso 2: comprueba el código; si falla, descuenta un intento. */
  async verifyCode(rawEmail: string, rawCode: string): Promise<void> {
    await this.checkCode(normalizeEmail(rawEmail), rawCode.trim());
  }

  /** Paso 3: vuelve a comprobar el código, cambia la contraseña, cierra las sesiones e invalida el código. */
  async resetPassword(rawEmail: string, rawCode: string, newPassword: string): Promise<void> {
    // La política va primero para no gastar intentos del código (plan §5).
    if (!isStrongPassword(newPassword)) throw new RecoveryError("WEAK_PASSWORD");

    const email = normalizeEmail(rawEmail);
    const uid = await this.checkCode(email, rawCode.trim());
    await this.users.updatePassword(uid, newPassword);
    await this.users.revokeSessions(uid);
    await this.store.delete(recordKey(email));
  }

  /**
   * Valida el código contra el registro y devuelve el `uid` de la cuenta. El resultado sale de la transacción y el
   * error se lanza después, para que el descuento de intentos quede guardado (plan §4).
   */
  private async checkCode(email: string, code: string): Promise<string> {
    if (!CODE_REGEX.test(code)) throw new RecoveryError("INVALID_CODE");
    const now = this.now();

    const outcome = await this.store.update<CodeCheck>(recordKey(email), (current) => {
      if (!current || current.codeHash === null || current.salt === null) return { result: { error: "INVALID_CODE" } };
      if (now > current.expiresAt) return { result: { error: "CODE_EXPIRED" } };
      if (current.attemptsLeft <= 0) return { result: { error: "TOO_MANY_ATTEMPTS" } };

      // Un correo sin cuenta nunca coincide, pero gasta intentos igual (CA-REC-03-05).
      if (current.uid !== null && sameHash(current.codeHash, hashCode(current.salt, code))) {
        return { result: { uid: current.uid } };
      }

      // Al agotar los intentos, el código se invalida y ni el correcto vale ya (CA-REC-03-02).
      const attemptsLeft = current.attemptsLeft - 1;
      const next: ResetRecord =
        attemptsLeft > 0 ? { ...current, attemptsLeft } : { ...current, attemptsLeft, codeHash: null, salt: null };
      return { next, result: { error: attemptsLeft > 0 ? "INVALID_CODE" : "TOO_MANY_ATTEMPTS" } };
    });

    if ("error" in outcome) throw new RecoveryError(outcome.error);
    return outcome.uid;
  }
}

/** Resultado de comprobar un código: el `uid` de la cuenta o la razón del rechazo. */
type CodeCheck = { uid: string } | { error: RecoveryErrorReason };

function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

/** Código aleatorio de 4 dígitos con un generador criptográfico (RF-REC-01). */
function randomCode(): string {
  return randomInt(0, 10 ** RecoveryLimits.CODE_LENGTH).toString().padStart(RecoveryLimits.CODE_LENGTH, "0");
}

function hashCode(salt: string, code: string): string {
  return sha256Hex(salt + code);
}

function sha256Hex(value: string): string {
  return createHash("sha256").update(value).digest("hex");
}

/** Comparación en tiempo constante, para no filtrar información por el tiempo de respuesta. */
function sameHash(expected: string, actual: string): boolean {
  const a = Buffer.from(expected, "hex");
  const b = Buffer.from(actual, "hex");
  return a.length === b.length && timingSafeEqual(a, b);
}
