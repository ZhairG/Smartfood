/**
 * Nombre: ports.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contratos de los servicios de recuperación (spec 003, plan §2) y de bienvenida (spec 004). La lógica de
 * `recoveryService.ts` y `registrationService.ts` solo conoce estas interfaces; `adapters.ts` las implementa con
 * Firebase y nodemailer, y las pruebas las implementan en memoria.
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
export interface StoreUpdate<T, R = ResetRecord> {
  next?: R | null;
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

/** Datos de una cuenta que necesita la bienvenida (spec 004). */
export interface RegisteredAccount {
  email: string;
  emailVerified: boolean;
}

/** Consulta de cuentas y enlaces de verificación (Admin SDK de Firebase Auth, spec 004). */
export interface RegisteredAccounts {
  /** Cuenta con ese `uid`, o `null` si no existe o no tiene correo. */
  find(uid: string): Promise<RegisteredAccount | null>;
  /** Enlace que verifica el correo al abrirlo (CA-MAIL-01-02). */
  verificationLink(email: string): Promise<string>;
}

/** Registro de bienvenidas enviadas, para enviar como máximo una por cuenta (CA-MAIL-02-01). */
export interface WelcomeLog {
  /** Reserva el envío de forma atómica: `false` si la cuenta ya tenía una bienvenida. */
  claim(uid: string): Promise<boolean>;
  /** Libera la reserva cuando el envío falla. */
  release(uid: string): Promise<void>;
}

/** Envío del correo de bienvenida. `name` puede venir vacío: el saludo es genérico. */
export interface WelcomeMailer {
  sendWelcome(email: string, name: string, verificationLink: string): Promise<void>;
}

/**
 * Registro de `emailChanges/{uid}` (spec 008, T010): el correo nuevo pedido y su código. Las fechas son epoch en ms y
 * los límites son los mismos de la recuperación.
 */
export interface EmailChangeRecord {
  newEmail: string;
  codeHash: string | null;
  salt: string | null;
  expiresAt: number;
  attemptsLeft: number;
  lastSentAt: number;
  windowStart: number;
  sentInWindow: number;
}

/** Almacén de cambios de correo por `uid`, con lectura-modificación-escritura atómica. */
export interface EmailChangeStore {
  update<T>(uid: string, change: (current: EmailChangeRecord | null) => StoreUpdate<T, EmailChangeRecord>): Promise<T>;
  delete(uid: string): Promise<void>;
}

/** Correos de las cuentas y del perfil (Admin SDK y Firestore, spec 008). */
export interface AccountEmails {
  /** `uid` de la cuenta con ese correo, o `null` si no existe. */
  findUidByEmail(email: string): Promise<string | null>;
  /** Correo actual de la cuenta, o `null` si no tiene. */
  currentEmail(uid: string): Promise<string | null>;
  /** Cambia el correo de la cuenta y lo marca verificado; `false` si otra cuenta ya lo usa. */
  updateEmail(uid: string, email: string): Promise<boolean>;
  /** Iguala `users/{uid}.email` al correo nuevo. */
  setProfileEmail(uid: string, email: string): Promise<void>;
}

/** Envío del código al correo nuevo (spec 008). */
export interface EmailChangeMailer {
  sendEmailChangeCode(email: string, code: string): Promise<void>;
}
