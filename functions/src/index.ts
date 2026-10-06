/**
 * Nombre: index.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Funciones callable de la recuperación de contraseña real (spec 003, plan §3). Cada una arma el
 * `RecoveryService` con sus adaptadores y traduce `RecoveryError` a `HttpsError` con `details.reason`, que la app
 * clasifica en `FirebaseErrorMapper`.
 */

import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { defineSecret } from "firebase-functions/params";
import { setGlobalOptions } from "firebase-functions/v2";
import { FunctionsErrorCode, HttpsError, onCall } from "firebase-functions/v2/https";
import { AdminUserDirectory, FirestoreResetStore, GmailMailer, LoggingMailer } from "./adapters.js";
import { Mailer } from "./ports.js";
import { RecoveryError, RecoveryErrorReason, RecoveryService } from "./recoveryService.js";

initializeApp();

// Misma región que Firestore y un tope de instancias para mantener el costo en $0 (RNF-REC-03).
setGlobalOptions({ region: "southamerica-east1", maxInstances: 5 });

/** Credenciales de Gmail en Secret Manager; nunca en el repositorio ni en el APK (RNF-REC-01). */
const smtpUser = defineSecret("SMTP_USER");
const smtpPass = defineSecret("SMTP_PASS");

const HTTPS_CODES: Record<RecoveryErrorReason, FunctionsErrorCode> = {
  INVALID_EMAIL: "invalid-argument",
  INVALID_CODE: "invalid-argument",
  WEAK_PASSWORD: "invalid-argument",
  CODE_EXPIRED: "failed-precondition",
  TOO_MANY_ATTEMPTS: "resource-exhausted",
  TOO_MANY_REQUESTS: "resource-exhausted",
};

/** Paso 1: `{ email }`. */
export const requestPasswordResetCode = onCall({ secrets: [smtpUser, smtpPass] }, (request) =>
  run(() => recoveryService().requestCode(stringField(request.data, "email"))),
);

/** Paso 2: `{ email, code }`. */
export const verifyPasswordResetCode = onCall((request) =>
  run(() => recoveryService().verifyCode(stringField(request.data, "email"), stringField(request.data, "code"))),
);

/** Paso 3: `{ email, code, newPassword }`. */
export const resetPasswordWithCode = onCall((request) =>
  run(() =>
    recoveryService().resetPassword(
      stringField(request.data, "email"),
      stringField(request.data, "code"),
      stringField(request.data, "newPassword"),
    ),
  ),
);

function recoveryService(): RecoveryService {
  return new RecoveryService(new FirestoreResetStore(getFirestore()), new AdminUserDirectory(getAuth()), lazyMailer);
}

/**
 * Crea el cliente de correo solo al enviar. Solo `requestPasswordResetCode` declara los secretos, así que las
 * otras funciones nunca los leen.
 */
const lazyMailer: Mailer = {
  sendRecoveryCode: (email, code) => {
    const mailer =
      process.env.FUNCTIONS_EMULATOR === "true" ? new LoggingMailer() : new GmailMailer(smtpUser.value(), smtpPass.value());
    return mailer.sendRecoveryCode(email, code);
  },
};

/** Ejecuta un paso y traduce sus fallas. Las inesperadas se registran y salen como `internal`, sin detalles. */
async function run(step: () => Promise<void>): Promise<Record<string, never>> {
  try {
    await step();
    return {};
  } catch (error) {
    if (error instanceof RecoveryError) {
      throw new HttpsError(HTTPS_CODES[error.reason], error.reason, { reason: error.reason });
    }
    logger.error("Fallo inesperado en la recuperación de contraseña", error);
    throw new HttpsError("internal", "INTERNAL");
  }
}

/** Lee un campo de texto de la petición; si falta o no es texto, devuelve "" y el servicio lo rechaza. */
function stringField(data: unknown, name: string): string {
  const value = (data as Record<string, unknown> | null | undefined)?.[name];
  return typeof value === "string" ? value : "";
}
