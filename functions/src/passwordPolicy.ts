/**
 * Nombre: passwordPolicy.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Política de contraseña de RF-AUTH-04, idéntica a `ValidatePasswordUseCase` de la app:
 * 8 caracteres o más, al menos una mayúscula y al menos un carácter que no sea letra ni dígito.
 * El servidor la vuelve a aplicar por si alguien salta la app (CA-REC-02-04).
 */

const MIN_LENGTH = 8;
const UPPERCASE = /\p{Lu}/u;
const NON_ALPHANUMERIC = /[^\p{L}\p{Nd}]/u;

/** Indica si [password] cumple la política. */
export function isStrongPassword(password: string): boolean {
  return password.length >= MIN_LENGTH && UPPERCASE.test(password) && NON_ALPHANUMERIC.test(password);
}
