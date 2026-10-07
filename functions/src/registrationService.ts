/**
 * Nombre: registrationService.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Reglas del correo de bienvenida (spec 004, plan §4). No conoce Firebase: recibe sus dependencias como
 * puertos (`ports.ts`). Envía como máximo una bienvenida por cuenta y siempre al correo de la cuenta, nunca al que
 * esté escrito en el perfil.
 */

import { RegisteredAccounts, WelcomeLog, WelcomeMailer } from "./ports.js";

/** Largo máximo del nombre en el saludo y el asunto. */
export const MAX_NAME_LENGTH = 60;

/** Resultado de procesar un perfil nuevo; se escribe en el log. */
export type WelcomeOutcome = "SENT" | "SKIPPED_NO_ACCOUNT" | "SKIPPED_VERIFIED" | "SKIPPED_ALREADY_SENT";

/**
 * Nombre apto para el saludo: sin caracteres de control, con los espacios colapsados y como máximo
 * [MAX_NAME_LENGTH] caracteres. Devuelve "" si el perfil no trae un nombre de texto.
 */
export function displayName(rawName: unknown): string {
  if (typeof rawName !== "string") return "";
  const clean = rawName.replace(/[\p{Cc}\p{Cf}]/gu, " ").replace(/\s+/g, " ").trim();
  return Array.from(clean).slice(0, MAX_NAME_LENGTH).join("").trim();
}

/** Servicio de bienvenida. */
export class WelcomeService {
  constructor(
    private readonly accounts: RegisteredAccounts,
    private readonly log: WelcomeLog,
    private readonly mailer: WelcomeMailer,
  ) {}

  /**
   * Envía la bienvenida de una cuenta cuyo perfil se acaba de crear (RF-MAIL-01).
   *
   * @param uid Identificador de la cuenta (id del documento `users/{uid}`).
   * @param profileName Campo `fullName` del perfil, tal como lo escribió el cliente.
   * @throws El error del enlace o del envío, después de liberar la reserva (CA-MAIL-01-04).
   */
  async onProfileCreated(uid: string, profileName: unknown): Promise<WelcomeOutcome> {
    const account = await this.accounts.find(uid);
    if (account === null) return "SKIPPED_NO_ACCOUNT";
    if (account.emailVerified) return "SKIPPED_VERIFIED";

    if (!(await this.log.claim(uid))) return "SKIPPED_ALREADY_SENT";

    try {
      const link = await this.accounts.verificationLink(account.email);
      await this.mailer.sendWelcome(account.email, displayName(profileName), link);
      return "SENT";
    } catch (error) {
      await this.log.release(uid);
      throw error;
    }
  }
}
