/**
 * Nombre: recoveryService.test.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de `RecoveryService` (spec 003) con fakes en memoria, sin Firebase ni librería de mocks.
 * Cada nombre cita el criterio de aceptación que cubre.
 */

import assert from "node:assert/strict";
import { beforeEach, describe, it } from "node:test";
import { gmailCredentials } from "../adapters.js";
import { isStrongPassword } from "../passwordPolicy.js";
import { Mailer, ResetRecord, ResetStore, StoreUpdate, UserDirectory } from "../ports.js";
import { RecoveryError, RecoveryErrorReason, RecoveryLimits, RecoveryService, recordKey } from "../recoveryService.js";

class InMemoryResetStore implements ResetStore {
  readonly records = new Map<string, ResetRecord>();

  async update<T>(key: string, change: (current: ResetRecord | null) => StoreUpdate<T>): Promise<T> {
    const { next, result } = change(this.records.get(key) ?? null);
    if (next === null) this.records.delete(key);
    else if (next !== undefined) this.records.set(key, structuredClone(next));
    return result;
  }

  async delete(key: string): Promise<void> {
    this.records.delete(key);
  }
}

class FakeUserDirectory implements UserDirectory {
  readonly passwords = new Map<string, string>();
  readonly revoked: string[] = [];

  constructor(private readonly accounts: Record<string, string>) {}

  async findUidByEmail(email: string): Promise<string | null> {
    return this.accounts[email] ?? null;
  }

  async updatePassword(uid: string, newPassword: string): Promise<void> {
    this.passwords.set(uid, newPassword);
  }

  async revokeSessions(uid: string): Promise<void> {
    this.revoked.push(uid);
  }
}

class FakeMailer implements Mailer {
  readonly sent: { email: string; code: string }[] = [];
  /** Si es `true`, el próximo envío falla como si Gmail rechazara las credenciales. */
  failNext = false;

  async sendRecoveryCode(email: string, code: string): Promise<void> {
    if (this.failNext) {
      this.failNext = false;
      throw new Error("535 BadCredentials");
    }
    this.sent.push({ email, code });
  }
}

const EMAIL = "pepito@smartfood.com";
const UNKNOWN_EMAIL = "nadie@smartfood.com";
const UID = "uid-pepito";
const NEW_PASSWORD = "NuevaClave#1";

let store: InMemoryResetStore;
let users: FakeUserDirectory;
let mailer: FakeMailer;
let now: number;
let nextCodes: string[];
let service: RecoveryService;

beforeEach(() => {
  store = new InMemoryResetStore();
  users = new FakeUserDirectory({ [EMAIL]: UID });
  mailer = new FakeMailer();
  now = Date.UTC(2026, 9, 5, 12);
  nextCodes = ["4821", "9137", "0056", "7310", "2468", "1357"];
  service = new RecoveryService(store, users, mailer, () => now, () => nextCodes.shift() ?? "0000");
});

/** Comprueba que la promesa falla con la razón esperada. */
async function rejectsWith(promise: Promise<unknown>, reason: RecoveryErrorReason): Promise<void> {
  await assert.rejects(promise, (error: unknown) => error instanceof RecoveryError && error.reason === reason);
}

/** Comprueba que la solicitud se rechaza por los límites e informa los segundos de espera esperados. */
async function rejectsWithWait(promise: Promise<unknown>, retryAfterSeconds: number): Promise<void> {
  await assert.rejects(
    promise,
    (error: unknown) =>
      error instanceof RecoveryError &&
      error.reason === "TOO_MANY_REQUESTS" &&
      error.retryAfterSeconds === retryAfterSeconds,
  );
}

function lastSentCode(): string {
  const last = mailer.sent.at(-1);
  assert.ok(last, "no se envió ningún correo");
  return last.code;
}

describe("HU-REC-01: recibir un código real", () => {
  it("CA-REC-01-01 un correo con cuenta recibe un código de 4 dígitos", async () => {
    await service.requestCode(`  ${EMAIL.toUpperCase()} `);

    assert.deepEqual(mailer.sent, [{ email: EMAIL, code: "4821" }]);
  });

  it("CA-REC-01-01 el registro guarda la huella del código y no el código", async () => {
    await service.requestCode(EMAIL);

    const record = store.records.get(recordKey(EMAIL));
    assert.ok(record);
    assert.equal(record.uid, UID);
    assert.notEqual(record.codeHash, "4821");
    assert.ok(!JSON.stringify(record).includes("4821"));
    assert.ok(!JSON.stringify([...store.records.keys()]).includes(EMAIL));
  });

  it("CA-REC-01-02 un correo sin cuenta responde igual y no envía correo", async () => {
    await service.requestCode(UNKNOWN_EMAIL);

    assert.equal(mailer.sent.length, 0);
  });

  it("CA-REC-01-02 un correo con formato invalido se rechaza", async () => {
    await rejectsWith(service.requestCode("pepito@"), "INVALID_EMAIL");
  });

  it("CA-REC-01-04 con un correo sin cuenta cualquier codigo es incorrecto", async () => {
    await service.requestCode(UNKNOWN_EMAIL);

    await rejectsWith(service.verifyCode(UNKNOWN_EMAIL, "4821"), "INVALID_CODE");
  });

  it("CA-REC-01-04 sin haber pedido codigo el codigo es incorrecto", async () => {
    await rejectsWith(service.verifyCode(EMAIL, "4821"), "INVALID_CODE");
  });

  it("CA-REC-01-05 al reenviar solo vale el ultimo codigo", async () => {
    await service.requestCode(EMAIL);
    const firstCode = lastSentCode();
    now += RecoveryLimits.RESEND_COOLDOWN_MS;
    await service.requestCode(EMAIL);

    await rejectsWith(service.verifyCode(EMAIL, firstCode), "INVALID_CODE");
    await service.verifyCode(EMAIL, lastSentCode());
  });

  it("CA-04-07 el codigo correcto se acepta", async () => {
    await service.requestCode(EMAIL);

    await service.verifyCode(EMAIL, lastSentCode());
  });

  it("CA-04-06 un codigo incompleto se rechaza sin gastar intentos", async () => {
    await service.requestCode(EMAIL);

    await rejectsWith(service.verifyCode(EMAIL, "48"), "INVALID_CODE");
    assert.equal(store.records.get(recordKey(EMAIL))?.attemptsLeft, RecoveryLimits.MAX_ATTEMPTS);
  });
});

describe("HU-REC-02: cambiar la contraseña de verdad", () => {
  it("CA-REC-02-01 con el codigo valido se cambia la contrasena de la cuenta", async () => {
    await service.requestCode(EMAIL);

    await service.resetPassword(EMAIL, lastSentCode(), NEW_PASSWORD);

    assert.equal(users.passwords.get(UID), NEW_PASSWORD);
  });

  it("CA-REC-02-02 tras el cambio se cierran las sesiones y el codigo deja de valer", async () => {
    await service.requestCode(EMAIL);
    const code = lastSentCode();

    await service.resetPassword(EMAIL, code, NEW_PASSWORD);

    assert.deepEqual(users.revoked, [UID]);
    await rejectsWith(service.verifyCode(EMAIL, code), "INVALID_CODE");
  });

  it("CA-REC-02-03 el paso 3 vuelve a validar el codigo", async () => {
    await service.requestCode(EMAIL);

    await rejectsWith(service.resetPassword(EMAIL, "0000", NEW_PASSWORD), "INVALID_CODE");
    assert.equal(users.passwords.size, 0);
  });

  it("CA-REC-02-03 el paso 3 rechaza un codigo vencido", async () => {
    await service.requestCode(EMAIL);
    now += RecoveryLimits.CODE_TTL_MS + 1;

    await rejectsWith(service.resetPassword(EMAIL, lastSentCode(), NEW_PASSWORD), "CODE_EXPIRED");
  });

  it("CA-REC-02-04 una contrasena debil se rechaza sin gastar intentos", async () => {
    await service.requestCode(EMAIL);

    await rejectsWith(service.resetPassword(EMAIL, lastSentCode(), "debil"), "WEAK_PASSWORD");
    assert.equal(users.passwords.size, 0);
    assert.equal(store.records.get(recordKey(EMAIL))?.attemptsLeft, RecoveryLimits.MAX_ATTEMPTS);
  });

  it("CA-REC-02-04 la politica coincide con ValidatePasswordUseCase", () => {
    assert.equal(isStrongPassword("Clave#123"), true);
    assert.equal(isStrongPassword("Cl#1"), false);
    assert.equal(isStrongPassword("clave#123"), false);
    assert.equal(isStrongPassword("Clave1234"), false);
    assert.equal(isStrongPassword("Contraseña Larga"), true);
  });
});

describe("HU-REC-03: protección contra abuso", () => {
  it("CA-REC-03-01 un codigo de mas de 10 minutos vence", async () => {
    await service.requestCode(EMAIL);
    now += RecoveryLimits.CODE_TTL_MS + 1;

    await rejectsWith(service.verifyCode(EMAIL, lastSentCode()), "CODE_EXPIRED");
  });

  it("CA-REC-03-02 al quinto fallo el codigo se invalida incluso el correcto", async () => {
    await service.requestCode(EMAIL);
    const code = lastSentCode();

    for (let attempt = 1; attempt < RecoveryLimits.MAX_ATTEMPTS; attempt++) {
      await rejectsWith(service.verifyCode(EMAIL, "0000"), "INVALID_CODE");
    }
    await rejectsWith(service.verifyCode(EMAIL, "0000"), "TOO_MANY_ATTEMPTS");
    await rejectsWith(service.verifyCode(EMAIL, code), "INVALID_CODE");
  });

  it("CA-REC-03-02 un codigo nuevo restablece los intentos", async () => {
    await service.requestCode(EMAIL);
    for (let attempt = 0; attempt < RecoveryLimits.MAX_ATTEMPTS; attempt++) {
      await assert.rejects(service.verifyCode(EMAIL, "0000"));
    }
    now += RecoveryLimits.RESEND_COOLDOWN_MS;

    await service.requestCode(EMAIL);

    await service.verifyCode(EMAIL, lastSentCode());
  });

  it("CA-REC-03-03 pedir otro codigo antes de 60 segundos se rechaza", async () => {
    await service.requestCode(EMAIL);
    now += RecoveryLimits.RESEND_COOLDOWN_MS - 1;

    await rejectsWith(service.requestCode(EMAIL), "TOO_MANY_REQUESTS");
    assert.equal(mailer.sent.length, 1);
  });

  it("CA-REC-03-04 el sexto codigo de la hora se rechaza y la ventana se reinicia", async () => {
    for (let request = 0; request < RecoveryLimits.MAX_CODES_PER_WINDOW; request++) {
      await service.requestCode(EMAIL);
      now += RecoveryLimits.RESEND_COOLDOWN_MS;
    }

    await rejectsWith(service.requestCode(EMAIL), "TOO_MANY_REQUESTS");

    now += RecoveryLimits.WINDOW_MS;
    await service.requestCode(EMAIL);
    assert.equal(mailer.sent.length, RecoveryLimits.MAX_CODES_PER_WINDOW + 1);
  });

  it("CA-REC-03-05 los limites se aplican igual a un correo sin cuenta", async () => {
    await service.requestCode(UNKNOWN_EMAIL);
    await rejectsWith(service.requestCode(UNKNOWN_EMAIL), "TOO_MANY_REQUESTS");

    for (let attempt = 1; attempt < RecoveryLimits.MAX_ATTEMPTS; attempt++) {
      await rejectsWith(service.verifyCode(UNKNOWN_EMAIL, "4821"), "INVALID_CODE");
    }
    await rejectsWith(service.verifyCode(UNKNOWN_EMAIL, "4821"), "TOO_MANY_ATTEMPTS");
  });
});

describe("Credenciales de Gmail", () => {
  it("CA-REC-01-01 la contrasena de aplicacion se usa sin espacios ni saltos de linea", () => {
    assert.deepEqual(gmailCredentials(" equipo@gmail.com\n", "abcd efgh ijkl mnop\r\n"), {
      user: "equipo@gmail.com",
      pass: "abcdefghijklmnop",
    });
  });
});

describe("HU-REC-04: tiempo de espera para pedir otro código", () => {
  const SECOND = 1000;

  it("CA-REC-04-01 al enviar un codigo informa 60 segundos de espera", async () => {
    assert.equal(await service.requestCode(EMAIL), 60);
  });

  it("CA-REC-04-01 un correo sin cuenta informa la misma espera", async () => {
    assert.equal(await service.requestCode(UNKNOWN_EMAIL), 60);
  });

  it("CA-REC-04-02 el rechazo por la espera de 60 segundos informa lo que falta", async () => {
    await service.requestCode(EMAIL);
    now += 20 * SECOND + 1;

    await rejectsWithWait(service.requestCode(EMAIL), 40);
  });

  it("CA-REC-04-02 el quinto codigo de la hora informa la espera hasta el fin de la ventana", async () => {
    let wait = 0;
    for (let request = 0; request < RecoveryLimits.MAX_CODES_PER_WINDOW; request++) {
      wait = await service.requestCode(EMAIL);
      now += RecoveryLimits.RESEND_COOLDOWN_MS;
    }

    assert.equal(wait, 3600 - 4 * 60);
    await rejectsWithWait(service.requestCode(EMAIL), 3600 - 5 * 60);
  });
});

describe("CA-REC-03-07: envíos fallidos", () => {
  it("CA-REC-03-07 si el correo no se envia el intento no cuenta y se puede pedir enseguida", async () => {
    mailer.failNext = true;

    await assert.rejects(service.requestCode(EMAIL), /BadCredentials/);
    assert.equal(store.records.has(recordKey(EMAIL)), false);

    assert.equal(await service.requestCode(EMAIL), 60);
    assert.equal(mailer.sent.length, 1);
  });

  it("CA-REC-03-07 un envio fallido conserva el codigo anterior y su contador", async () => {
    await service.requestCode(EMAIL);
    const firstCode = lastSentCode();
    now += RecoveryLimits.RESEND_COOLDOWN_MS;
    mailer.failNext = true;

    await assert.rejects(service.requestCode(EMAIL));

    assert.equal(store.records.get(recordKey(EMAIL))?.sentInWindow, 1);
    await service.verifyCode(EMAIL, firstCode);
  });
});
