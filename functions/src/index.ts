/**
 * Nombre: index.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Funciones de SmartFood. Las 3 callable de la recuperación de contraseña real (spec 003, plan §3) arman
 * el `RecoveryService` con sus adaptadores y traducen `RecoveryError` a `HttpsError` con `details.reason`, que la app
 * clasifica en `FirebaseErrorMapper`. `sendWelcomeEmail` envía la bienvenida al crearse un perfil (spec 004).
 * `requestEmailChangeCode` y `confirmEmailChange` cambian el correo con un código, sin cerrar la sesión (spec 008).
 */

import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { defineSecret } from "firebase-functions/params";
import { setGlobalOptions } from "firebase-functions/v2";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { FunctionsErrorCode, HttpsError, onCall } from "firebase-functions/v2/https";
import {
  AdminAccountEmails,
  AdminUserDirectory,
  FirestoreEmailChangeStore,
  FirestoreResetStore,
  FirestoreWelcomeLog,
  GmailMailer,
  LoggingMailer,
} from "./adapters.js";
import { EmailChangeError, EmailChangeErrorReason, EmailChangeService } from "./emailChangeService.js";
import { EmailChangeMailer, Mailer, WelcomeMailer } from "./ports.js";
import { RecoveryError, RecoveryErrorReason, RecoveryService } from "./recoveryService.js";
import { WelcomeService } from "./registrationService.js";

initializeApp();

// Misma región que Firestore y un tope de instancias para mantener el costo en $0 (RNF-REC-03).
setGlobalOptions({ region: "southamerica-east1", maxInstances: 5 });

/** Credenciales de Gmail en Secret Manager; nunca en el repositorio ni en el APK (RNF-REC-01). */
const smtpUser = defineSecret("SMTP_USER");
const smtpPass = defineSecret("SMTP_PASS");

const HTTPS_CODES: Record<RecoveryErrorReason | EmailChangeErrorReason, FunctionsErrorCode> = {
  INVALID_EMAIL: "invalid-argument",
  INVALID_CODE: "invalid-argument",
  WEAK_PASSWORD: "invalid-argument",
  SAME_EMAIL: "invalid-argument",
  EMAIL_IN_USE: "already-exists",
  REQUIRES_RECENT_LOGIN: "failed-precondition",
  CODE_EXPIRED: "failed-precondition",
  TOO_MANY_ATTEMPTS: "resource-exhausted",
  TOO_MANY_REQUESTS: "resource-exhausted",
};

/** Paso 1: `{ email }`. */
export const requestPasswordResetCode = onCall({ secrets: [smtpUser, smtpPass] }, (request) =>
  run(async () => ({ resendAfterSeconds: await recoveryService().requestCode(stringField(request.data, "email")) })),
);

/** Paso 2: `{ email, code }`. */
export const verifyPasswordResetCode = onCall((request) =>
  run(async () => {
    await recoveryService().verifyCode(stringField(request.data, "email"), stringField(request.data, "code"));
    return {};
  }),
);

/** Paso 3: `{ email, code, newPassword }`. */
export const resetPasswordWithCode = onCall((request) =>
  run(async () => {
    await recoveryService().resetPassword(
      stringField(request.data, "email"),
      stringField(request.data, "code"),
      stringField(request.data, "newPassword"),
    );
    return {};
  }),
);

/** Cambio de correo, paso 1: `{ newEmail }` con sesión. Responde `{ resendAfterSeconds }` (CA-PER-02-10). */
export const requestEmailChangeCode = onCall({ secrets: [smtpUser, smtpPass] }, (request) =>
  run(async () => {
    const auth = signedIn(request.auth);
    // `auth_time` viene en segundos: hora del último inicio de sesión o reautenticación.
    const authTimeMs = Number(auth.token.auth_time ?? 0) * 1000;
    return { resendAfterSeconds: await emailChangeService().requestCode(auth.uid, authTimeMs, stringField(request.data, "newEmail")) };
  }),
);

/** Cambio de correo, paso 2: `{ code }` con sesión. Responde `{ email }` con el correo nuevo (CA-PER-02-12). */
export const confirmEmailChange = onCall((request) =>
  run(async () => ({ email: await emailChangeService().confirm(signedIn(request.auth).uid, stringField(request.data, "code")) })),
);

/**
 * Bienvenida con enlace de verificación al crearse `users/{uid}` (spec 004, ADR-MAIL-01). Sin reintentos: un fallo
 * se registra y libera la reserva, para no mandar correos repetidos (CA-MAIL-01-04, ADR-MAIL-04).
 */
export const sendWelcomeEmail = onDocumentCreated(
  { document: "users/{uid}", secrets: [smtpUser, smtpPass], retry: false },
  async (event) => {
    const uid = event.params.uid;
    try {
      const outcome = await welcomeService().onProfileCreated(uid, event.data?.get("fullName"));
      logger.info(`Bienvenida de ${uid}: ${outcome}`);
    } catch (error) {
      logger.error(`No se pudo enviar la bienvenida de ${uid}`, error);
    }
  },
);

function recoveryService(): RecoveryService {
  return new RecoveryService(new FirestoreResetStore(getFirestore()), new AdminUserDirectory(getAuth()), lazyMailer);
}

function emailChangeService(): EmailChangeService {
  return new EmailChangeService(new FirestoreEmailChangeStore(getFirestore()), new AdminAccountEmails(getAuth(), getFirestore()), lazyMailer);
}

/** Exige sesión: el cambio de correo es siempre de la cuenta que llama. */
function signedIn<A extends { uid: string }>(auth: A | undefined): A {
  if (!auth) throw new HttpsError("unauthenticated", "UNAUTHENTICATED");
  return auth;
}

function welcomeService(): WelcomeService {
  return new WelcomeService(new AdminUserDirectory(getAuth()), new FirestoreWelcomeLog(getFirestore()), lazyMailer);
}

/**
 * Crea el cliente de correo solo al enviar. Solo `requestPasswordResetCode`, `requestEmailChangeCode` y
 * `sendWelcomeEmail` declaran los secretos, así que las otras funciones nunca los leen.
 */
const lazyMailer: Mailer & WelcomeMailer & EmailChangeMailer = {
  sendRecoveryCode: (email, code) => mailer().sendRecoveryCode(email, code),
  sendWelcome: (email, name, link) => mailer().sendWelcome(email, name, link),
  sendEmailChangeCode: (email, code) => mailer().sendEmailChangeCode(email, code),
};

function mailer(): Mailer & WelcomeMailer & EmailChangeMailer {
  return process.env.FUNCTIONS_EMULATOR === "true"
    ? new LoggingMailer()
    : new GmailMailer(smtpUser.value(), smtpPass.value());
}

/**
 * Ejecuta un paso y traduce sus fallas. `TOO_MANY_REQUESTS` incluye `retryAfterSeconds` (RF-REC-07). Las fallas
 * inesperadas se registran y salen como `internal`, sin detalles.
 */
async function run<T extends object>(step: () => Promise<T>): Promise<T> {
  try {
    return await step();
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    if (error instanceof RecoveryError || error instanceof EmailChangeError) {
      const details =
        error.retryAfterSeconds === undefined
          ? { reason: error.reason }
          : { reason: error.reason, retryAfterSeconds: error.retryAfterSeconds };
      throw new HttpsError(HTTPS_CODES[error.reason], error.reason, details);
    }
    logger.error("Fallo inesperado en una función callable", error);
    throw new HttpsError("internal", "INTERNAL");
  }
}

/** Lee un campo de texto de la petición; si falta o no es texto, devuelve "" y el servicio lo rechaza. */
function stringField(data: unknown, name: string): string {
  const value = (data as Record<string, unknown> | null | undefined)?.[name];
  return typeof value === "string" ? value : "";
}
