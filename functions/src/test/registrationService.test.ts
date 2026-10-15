/**
 * Nombre: registrationService.test.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de `WelcomeService` y de la plantilla de bienvenida (spec 004) con fakes en memoria, sin
 * Firebase ni librería de mocks. Cada nombre cita el criterio de aceptación que cubre.
 */

import assert from "node:assert/strict";
import { beforeEach, describe, it } from "node:test";
import { escapeHtml, welcomeEmail } from "../emailTemplates.js";
import { RegisteredAccount, RegisteredAccounts, WelcomeLog, WelcomeMailer } from "../ports.js";
import { displayName, MAX_NAME_LENGTH, WelcomeService } from "../registrationService.js";

class FakeAccounts implements RegisteredAccounts {
  readonly accounts = new Map<string, RegisteredAccount>();

  async find(uid: string): Promise<RegisteredAccount | null> {
    return this.accounts.get(uid) ?? null;
  }

  async verificationLink(email: string): Promise<string> {
    return `https://smartfood.test/verificar?correo=${encodeURIComponent(email)}`;
  }
}

class InMemoryWelcomeLog implements WelcomeLog {
  readonly claimed = new Set<string>();

  async claim(uid: string): Promise<boolean> {
    if (this.claimed.has(uid)) return false;
    this.claimed.add(uid);
    return true;
  }

  async release(uid: string): Promise<void> {
    this.claimed.delete(uid);
  }
}

class FakeWelcomeMailer implements WelcomeMailer {
  readonly sent: { email: string; name: string; link: string }[] = [];
  /** Si es `true`, el próximo envío falla como si Gmail rechazara las credenciales. */
  failNext = false;

  async sendWelcome(email: string, name: string, link: string): Promise<void> {
    if (this.failNext) {
      this.failNext = false;
      throw new Error("535 BadCredentials");
    }
    this.sent.push({ email, name, link });
  }
}

const UID = "uid-pepito";
const EMAIL = "pepito@smartfood.com";

let accounts: FakeAccounts;
let log: InMemoryWelcomeLog;
let mailer: FakeWelcomeMailer;
let service: WelcomeService;

beforeEach(() => {
  accounts = new FakeAccounts();
  accounts.accounts.set(UID, { email: EMAIL, emailVerified: false });
  log = new InMemoryWelcomeLog();
  mailer = new FakeWelcomeMailer();
  service = new WelcomeService(accounts, log, mailer);
});

describe("HU-MAIL-01: recibir la bienvenida y verificar mi correo", () => {
  it("CA-MAIL-01-01 una cuenta nueva recibe la bienvenida con su nombre y el enlace de verificacion", async () => {
    const outcome = await service.onProfileCreated(UID, "  Pepito   Pérez ");

    assert.equal(outcome, "SENT");
    assert.deepEqual(mailer.sent, [
      { email: EMAIL, name: "Pepito Pérez", link: "https://smartfood.test/verificar?correo=pepito%40smartfood.com" },
    ]);
  });

  it("CA-MAIL-01-01 la plantilla saluda por el nombre y trae el boton Verificar mi correo con el enlace", () => {
    const email = welcomeEmail("Pepito", "https://smartfood.test/verificar?a=1&b=2");

    assert.equal(email.subject, "¡Bienvenido a SmartFood, Pepito!");
    assert.ok(email.html.includes("¡Hola, Pepito!"));
    assert.ok(email.html.includes('href="https://smartfood.test/verificar?a=1&amp;b=2"'));
    assert.ok(email.html.includes("Verificar mi correo"));
    assert.ok(email.text.includes("https://smartfood.test/verificar?a=1&b=2"));
  });

  it("CA-MAIL-01-01 sin nombre el saludo es generico", async () => {
    await service.onProfileCreated(UID, undefined);

    assert.equal(mailer.sent[0]?.name, "");
    assert.equal(welcomeEmail("", "https://x").subject, "¡Bienvenido a SmartFood!");
  });

  it("CA-MAIL-01-04 si el envio falla libera la reserva y propaga el error", async () => {
    mailer.failNext = true;

    await assert.rejects(service.onProfileCreated(UID, "Pepito"), /BadCredentials/);
    assert.equal(log.claimed.has(UID), false);
  });

  it("CA-MAIL-01-04 despues de un envio fallido un nuevo evento puede enviarla", async () => {
    mailer.failNext = true;
    await assert.rejects(service.onProfileCreated(UID, "Pepito"));

    assert.equal(await service.onProfileCreated(UID, "Pepito"), "SENT");
    assert.equal(mailer.sent.length, 1);
  });
});

describe("HU-MAIL-02: protección contra abuso", () => {
  it("CA-MAIL-02-01 recrear el perfil no envia otra bienvenida", async () => {
    await service.onProfileCreated(UID, "Pepito");

    assert.equal(await service.onProfileCreated(UID, "Pepito"), "SKIPPED_ALREADY_SENT");
    assert.equal(mailer.sent.length, 1);
  });

  it("CA-MAIL-02-02 el correo va a la direccion de la cuenta y no a la del perfil", async () => {
    // El servicio no recibe el correo del perfil: solo el uid y el nombre.
    await service.onProfileCreated(UID, "Pepito");

    assert.equal(mailer.sent[0]?.email, EMAIL);
  });

  it("CA-MAIL-02-03 un nombre con HTML se muestra como texto", () => {
    const email = welcomeEmail('<b>Pepito</b> "&"', "https://x");

    assert.ok(email.html.includes("&lt;b&gt;Pepito&lt;/b&gt; &quot;&amp;&quot;"));
    assert.ok(!email.html.includes("<b>Pepito</b>"));
    assert.equal(escapeHtml(`<a href='x'>&</a>`), "&lt;a href=&#39;x&#39;&gt;&amp;&lt;/a&gt;");
  });

  it("CA-MAIL-02-03 el nombre pierde los saltos de linea y se recorta", () => {
    assert.equal(displayName("Pepito\r\nBcc: otro@correo.com"), "Pepito Bcc: otro@correo.com");
    assert.equal(displayName("a".repeat(MAX_NAME_LENGTH + 20)).length, MAX_NAME_LENGTH);
    assert.equal(displayName(42), "");
  });

  it("CA-MAIL-02-04 una cuenta ya verificada no recibe nada", async () => {
    accounts.accounts.set(UID, { email: EMAIL, emailVerified: true });

    assert.equal(await service.onProfileCreated(UID, "Pepito"), "SKIPPED_VERIFIED");
    assert.equal(mailer.sent.length, 0);
    assert.equal(log.claimed.size, 0);
  });

  it("CA-MAIL-02-04 un perfil sin cuenta no recibe nada", async () => {
    assert.equal(await service.onProfileCreated("uid-inexistente", "Pepito"), "SKIPPED_NO_ACCOUNT");
    assert.equal(mailer.sent.length, 0);
  });
});
