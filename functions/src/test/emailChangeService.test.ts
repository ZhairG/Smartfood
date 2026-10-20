/**
 * Nombre: emailChangeService.test.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de `EmailChangeService` (spec 008, T010) con fakes en memoria, sin Firebase ni librería de
 * mocks. Cada nombre cita el criterio de aceptación que cubre.
 */

import assert from "node:assert/strict";
import { beforeEach, describe, it } from "node:test";
import { EmailChangeError, EmailChangeErrorReason, EmailChangeService, RECENT_LOGIN_MS } from "../emailChangeService.js";
import { emailChangeEmail } from "../emailTemplates.js";
import { AccountEmails, EmailChangeMailer, EmailChangeRecord, EmailChangeStore, StoreUpdate } from "../ports.js";
import { RecoveryLimits } from "../recoveryService.js";

class InMemoryEmailChangeStore implements EmailChangeStore {
  readonly records = new Map<string, EmailChangeRecord>();

  async update<T>(uid: string, change: (current: EmailChangeRecord | null) => StoreUpdate<T, EmailChangeRecord>): Promise<T> {
    const { next, result } = change(this.records.get(uid) ?? null);
    if (next === null) this.records.delete(uid);
    else if (next !== undefined) this.records.set(uid, structuredClone(next));
    return result;
  }

  async delete(uid: string): Promise<void> {
    this.records.delete(uid);
  }
}

class FakeAccountEmails implements AccountEmails {
  readonly profileEmails = new Map<string, string>();

  constructor(readonly emails: Map<string, string>) {}

  async findUidByEmail(email: string): Promise<string | null> {
    for (const [uid, value] of this.emails) if (value === email) return uid;
    return null;
  }

  async currentEmail(uid: string): Promise<string | null> {
    return this.emails.get(uid) ?? null;
  }

  async updateEmail(uid: string, email: string): Promise<boolean> {
    if ((await this.findUidByEmail(email)) !== null) return false;
    this.emails.set(uid, email);
    return true;
  }

  async setProfileEmail(uid: string, email: string): Promise<void> {
    this.profileEmails.set(uid, email);
  }
}

class FakeMailer implements EmailChangeMailer {
  readonly sent: { email: string; code: string }[] = [];
  failNext = false;

  async sendEmailChangeCode(email: string, code: string): Promise<void> {
    if (this.failNext) {
      this.failNext = false;
      throw new Error("535 BadCredentials");
    }
    this.sent.push({ email, code });
  }
}

const UID = "uid-ana";
const OTHER_UID = "uid-pepito";
const OLD_EMAIL = "ana@correo.com";
const NEW_EMAIL = "ana.nueva@correo.com";
const USED_EMAIL = "pepito@correo.com";

let store: InMemoryEmailChangeStore;
let accounts: FakeAccountEmails;
let mailer: FakeMailer;
let now: number;
let service: EmailChangeService;

beforeEach(() => {
  store = new InMemoryEmailChangeStore();
  accounts = new FakeAccountEmails(new Map([[UID, OLD_EMAIL], [OTHER_UID, USED_EMAIL]]));
  mailer = new FakeMailer();
  now = Date.UTC(2026, 9, 19, 12);
  service = new EmailChangeService(store, accounts, mailer, () => now, () => "4821");
});

async function rejects(promise: Promise<unknown>, reason: EmailChangeErrorReason): Promise<void> {
  await assert.rejects(promise, (error: unknown) => error instanceof EmailChangeError && error.reason === reason);
}

describe("requestCode", () => {
  it("CA-PER-02-10 envía el código al correo nuevo normalizado y devuelve la espera para reenviar", async () => {
    const wait = await service.requestCode(UID, now, "  Ana.Nueva@Correo.com ");

    assert.deepEqual(mailer.sent, [{ email: NEW_EMAIL, code: "4821" }]);
    assert.equal(wait, RecoveryLimits.RESEND_COOLDOWN_MS / 1000);
    assert.equal(accounts.emails.get(UID), OLD_EMAIL, "la cuenta no cambia hasta confirmar");
  });

  it("CA-PER-02-11 rechaza el correo inválido, el mismo y el que usa otra cuenta, sin enviar nada", async () => {
    await rejects(service.requestCode(UID, now, "no-es-correo"), "INVALID_EMAIL");
    await rejects(service.requestCode(UID, now, "ANA@correo.com"), "SAME_EMAIL");
    await rejects(service.requestCode(UID, now, USED_EMAIL), "EMAIL_IN_USE");
    assert.equal(mailer.sent.length, 0);
  });

  it("CA-PER-02-10 exige un inicio de sesión reciente", async () => {
    await rejects(service.requestCode(UID, now - RECENT_LOGIN_MS - 1, NEW_EMAIL), "REQUIRES_RECENT_LOGIN");
  });

  it("CA-PER-02-10 no deja reenviar antes de 60 s y el envío fallido no cuenta", async () => {
    mailer.failNext = true;
    await assert.rejects(service.requestCode(UID, now, NEW_EMAIL));
    assert.equal(store.records.size, 0, "el fallo restaura el registro");

    await service.requestCode(UID, now, NEW_EMAIL);
    now += 30_000;
    await rejects(service.requestCode(UID, now, NEW_EMAIL), "TOO_MANY_REQUESTS");
  });
});

describe("confirm", () => {
  it("CA-PER-02-12 con el código correcto cambia la cuenta y el perfil y borra el registro", async () => {
    await service.requestCode(UID, now, NEW_EMAIL);

    const email = await service.confirm(UID, " 4821 ");

    assert.equal(email, NEW_EMAIL);
    assert.equal(accounts.emails.get(UID), NEW_EMAIL);
    assert.equal(accounts.profileEmails.get(UID), NEW_EMAIL);
    assert.equal(store.records.size, 0);
  });

  it("CA-PER-02-11 el código incorrecto gasta intentos y al agotarlos ni el correcto vale", async () => {
    await service.requestCode(UID, now, NEW_EMAIL);
    await rejects(service.confirm(UID, "12a4"), "INVALID_CODE");
    for (let i = 1; i < RecoveryLimits.MAX_ATTEMPTS; i++) await rejects(service.confirm(UID, "0000"), "INVALID_CODE");
    await rejects(service.confirm(UID, "0000"), "TOO_MANY_ATTEMPTS");

    await rejects(service.confirm(UID, "4821"), "INVALID_CODE");
    assert.equal(accounts.emails.get(UID), OLD_EMAIL);
  });

  it("CA-PER-02-11 el código vence a los 10 minutos y sin solicitud no hay nada que confirmar", async () => {
    await rejects(service.confirm(UID, "4821"), "INVALID_CODE");
    await service.requestCode(UID, now, NEW_EMAIL);
    now += RecoveryLimits.CODE_TTL_MS + 1;
    await rejects(service.confirm(UID, "4821"), "CODE_EXPIRED");
  });

  it("CA-PER-02-11 si otra cuenta tomó el correo mientras tanto, no cambia nada", async () => {
    await service.requestCode(UID, now, NEW_EMAIL);
    accounts.emails.set("uid-intrusa", NEW_EMAIL);

    await rejects(service.confirm(UID, "4821"), "EMAIL_IN_USE");
    assert.equal(accounts.emails.get(UID), OLD_EMAIL);
    assert.equal(accounts.profileEmails.size, 0);
  });
});

describe("emailChangeEmail", () => {
  it("CA-PER-02-10 el correo va en español con el código", () => {
    const content = emailChangeEmail("4821");
    assert.match(content.subject, /Confirma tu nuevo correo en SmartFood: 4821/);
    assert.match(content.html, /4821/);
    assert.match(content.text, /Vence en 10 minutos/);
  });
});
