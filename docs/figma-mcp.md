# Configurar el MCP de Figma

Pasos para conectar el MCP de Figma en cada entorno. La regla de cuándo usarlos está en
[`AGENTS.md`](../AGENTS.md#si-el-mcp-de-figma-no-está-configurado): si la IA no tiene herramientas de Figma, no
implementa el diseño, identifica el entorno y el modelo en que está corriendo y muestra **solo** los pasos de ese
entorno. Si no puede identificarlo, pregunta cuál es.

Hay dos servidores. Usar el **remoto** salvo que el entorno no permita iniciar sesión con OAuth:

| Servidor | URL | Requisito |
|---|---|---|
| Remoto (recomendado) | `https://mcp.figma.com/mcp` | Cuenta de Figma con acceso al archivo. Las cuentas gratuitas tienen un límite bajo de llamadas al mes. |
| Escritorio | `http://127.0.0.1:3845/mcp` | App de escritorio de Figma abierta, Dev Mode, *Enable desktop MCP server*. Requiere un asiento Dev o Full de un plan pago. |

**Gemini CLI**
1. Ejecutar `gemini mcp add --transport http figma https://mcp.figma.com/mcp` (o agregar en `~/.gemini/settings.json`: `"mcpServers": { "figma": { "httpUrl": "https://mcp.figma.com/mcp" } }`).
2. Abrir `gemini` y ejecutar `/mcp auth figma`; autorizar en el navegador con la cuenta de Figma.
3. Comprobar con `/mcp` que `figma` aparece conectado con sus herramientas.

**Gemini en Android Studio**
1. *Settings → Tools → AI → MCP Servers* → activar *Enable MCP Servers*.
2. En el `mcp.json` que abre, agregar `"mcpServers": { "figma": { "httpUrl": "https://mcp.figma.com/mcp" } }`. Si la versión no permite iniciar sesión, usar el servidor de escritorio: `"httpUrl": "http://127.0.0.1:3845/mcp"` con la app de Figma abierta.
3. Aplicar y comprobar en el chat del agente (modo *Agent*) que aparecen las herramientas de Figma.

**Claude Code**
1. Ejecutar `claude mcp add --transport http figma https://mcp.figma.com/mcp`.
2. En la sesión, ejecutar `/mcp`, elegir `figma` → *Authenticate* y autorizar en el navegador.

**VS Code (GitHub Copilot, modo Agent)**
1. Crear `.vscode/mcp.json` con `{ "servers": { "figma": { "type": "http", "url": "https://mcp.figma.com/mcp" } } }`.
2. Pulsar *Start* sobre el servidor en ese archivo y autorizar en el navegador.
3. En el chat, modo *Agent* → icono de herramientas → comprobar que figuran las de Figma.

**Cursor**
1. *Settings → MCP → Add new MCP server*, o en `~/.cursor/mcp.json`: `{ "mcpServers": { "figma": { "url": "https://mcp.figma.com/mcp" } } }`.
2. Pulsar *Connect* / *Login* junto a `figma` y autorizar en el navegador.

**Otro entorno:** buscar en su configuración la sección "MCP servers", agregar un servidor HTTP con la URL remota y autenticarse. Si el entorno no admite MCP, decirlo y pedir al usuario que exporte el frame (captura + medidas de Dev Mode) mientras tanto.

Después de configurarlo, reiniciar la sesión del agente y pedirle que lea un frame del archivo para confirmar la conexión.
