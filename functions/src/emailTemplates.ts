/**
 * Nombre: emailTemplates.ts
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Plantillas de los correos de SmartFood: código de recuperación (spec 003) y bienvenida con enlace de
 * verificación (spec 004, plan §6). Son funciones puras: no conocen nodemailer ni Firebase y se prueban sin red.
 */

/** Contenido de un correo listo para enviar. */
export interface EmailContent {
  subject: string;
  text: string;
  html: string;
}

/** Escapa los caracteres especiales de HTML para mostrar texto del usuario como texto (CA-MAIL-02-03). */
export function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

/** Correo con el código de recuperación de contraseña (spec 003). */
export function recoveryEmail(code: string): EmailContent {
  return {
    subject: `Tu código de SmartFood es ${code}`,
    text:
      `Tu código para restablecer la contraseña de SmartFood es ${code}.\n\n` +
      "Vence en 10 minutos. Si no lo pediste, ignora este correo: tu contraseña no cambiará.",
    html: emailLayout(`
      <p style="margin:0 0 24px;font-size:15px">Usa este código para restablecer tu contraseña:</p>
      <p style="margin:0 0 24px;font-size:36px;font-weight:bold;letter-spacing:12px">${code}</p>
      <p style="margin:0;font-size:13px;color:#5f6b5a">
        Vence en 10 minutos. Si no lo pediste, ignora este correo: tu contraseña no cambiará.
      </p>`),
  };
}

/**
 * Correo de bienvenida con el enlace de verificación (spec 004, CA-MAIL-01-01).
 *
 * @param name Nombre ya recortado; si viene vacío, el saludo es genérico.
 * @param verificationLink Enlace que verifica el correo al abrirlo.
 */
export function welcomeEmail(name: string, verificationLink: string): EmailContent {
  const greeting = name ? `¡Hola, ${name}!` : "¡Hola!";
  const safeLink = escapeHtml(verificationLink);
  return {
    subject: name ? `¡Bienvenido a SmartFood, ${name}!` : "¡Bienvenido a SmartFood!",
    text:
      `${greeting}\n\n` +
      "Tu cuenta de SmartFood quedó creada. Desde ahora puedes organizar tu inventario, cuidar las fechas de " +
      "vencimiento y aprovechar mejor tu presupuesto.\n\n" +
      `Verifica tu correo abriendo este enlace:\n${verificationLink}\n\n` +
      "Si no creaste esta cuenta, ignora este correo.",
    html: emailLayout(`
      <p style="margin:0 0 12px;font-size:18px;font-weight:bold">${escapeHtml(greeting)}</p>
      <p style="margin:0 0 24px;font-size:15px">
        Tu cuenta quedó creada. Desde ahora puedes organizar tu inventario, cuidar las fechas de vencimiento y
        aprovechar mejor tu presupuesto.
      </p>
      <a href="${safeLink}"
         style="display:inline-block;margin:0 0 24px;padding:14px 28px;border-radius:12px;background:#2e7d32;
                color:#ffffff;font-size:15px;font-weight:bold;text-decoration:none">Verificar mi correo</a>
      <p style="margin:0;font-size:13px;color:#5f6b5a">
        Si no creaste esta cuenta, ignora este correo.
      </p>`),
  };
}

/** Marco común de los correos: fondo, tarjeta y encabezado de marca. */
function emailLayout(body: string): string {
  return `<!doctype html>
<html lang="es">
  <body style="margin:0;padding:24px;background:#f4f7f2;font-family:Arial,Helvetica,sans-serif;color:#1f2a1c">
    <div style="max-width:420px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;text-align:center">
      <h1 style="margin:0 0 8px;font-size:22px;color:#2e7d32">SmartFood</h1>${body}
    </div>
  </body>
</html>`;
}
