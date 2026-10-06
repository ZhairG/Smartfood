/**
 * <build>
 * <b>Nombre:</b> ports.ts<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 05/10/2026<br>
 * <b>Descripción:</b> Contratos que necesita el servicio de recuperación (spec 003, plan §2). La lógica de<br>
 * `recoveryService.ts` solo conoce estas interfaces; `adapters.ts` las implementa con Firebase y nodemailer, y las<br>
 * pruebas las implementan en memoria.<br>
 * </build>
 */

/** Registro de `passwordResets/{sha256(correo)}` (plan §4). Las fechas son epoch en milisegundos. */
export interface ResetRecord {
  uid: string | null;
  codeHash: string | null;
  salt: string | null;
  expiresAt: number;
  attemptsLeft: number;
  lastSentAt: number;
  windowStart: number;
  sentInWindow: number;
}

/**
 * Resultado de una modificación atómica: `next` es el nuevo registro (`undefined` = sin cambios,
 * `null` = borrarlo) y `result` lo que devuelve la operación.
 */
export interface StoreUpdate<T> {
  next?: ResetRecord | null;
  result: T;
}

/** Almacén de registros con lectura-modificación-escritura atómica. */
export interface ResetStore {
  update<T>(key: string, change: (current: ResetRecord | null) => StoreUpdate<T>): Promise<T>;
  delete(key: string): Promise<void>;
}

/** Operaciones sobre las cuentas (Admin SDK de Firebase Auth). */
export interface UserDirectory {
  /** `uid` de la cuenta con ese correo, o `null` si no existe. */
  findUidByEmail(email: string): Promise<string | null>;
  updatePassword(uid: string, newPassword: string): Promise<void>;
  /** Cierra las sesiones abiertas en otros dispositivos (CA-REC-02-02). */
  revokeSessions(uid: string): Promise<void>;
}

/** Envío del código al correo del usuario. */
export interface Mailer {
  sendRecoveryCode(email: string, code: string): Promise<void>;
}

/** Hora actual en epoch ms; inyectable para probar vencimientos. */
export type Clock = () => number;
