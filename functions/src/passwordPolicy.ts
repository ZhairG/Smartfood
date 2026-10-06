/**
 * <build>
 * <b>Nombre:</b> passwordPolicy.ts<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 05/10/2026<br>
 * <b>Descripción:</b> Política de contraseña de RF-AUTH-04, idéntica a `ValidatePasswordUseCase` de la app: 8<br>
 * caracteres o más, al menos una mayúscula y al menos un carácter que no sea letra ni dígito. El servidor la vuelve<br>
 * a aplicar por si alguien salta la app (CA-REC-02-04).<br>
 * </build>
 */

const MIN_LENGTH = 8;
const UPPERCASE = /\p{Lu}/u;
const NON_ALPHANUMERIC = /[^\p{L}\p{Nd}]/u;

/** Indica si [password] cumple la política. */
export function isStrongPassword(password: string): boolean {
  return password.length >= MIN_LENGTH && UPPERCASE.test(password) && NON_ALPHANUMERIC.test(password);
}
