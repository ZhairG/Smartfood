"""
Nombre: medir_fluidez.py
Autor: SmartFood Team
Fecha: Noviembre 2026
Descripción: Mide la fluidez de una transición en el teléfono (spec 007, RNF-ANI-01) con `dumpsys gfxinfo framestats`.

Reinicia las estadísticas de la app, ejecuta la acción de adb que se le pasa (por ejemplo, un toque), espera y lee los
cuadros dibujados. Reporta:
  - el primer cuadro: milisegundos entre la acción y el inicio del primer cuadro;
  - el hueco más largo entre dos cuadros seguidos;
  - el porcentaje de cuadros lentos (más de 16,7 ms entre el vsync previsto y el cuadro terminado).

Meta de RNF-ANI-01 (build release en el V2436): primer cuadro <= 50 ms, ningún hueco > 50 ms y <= 5 % de cuadros lentos.

Uso:
    python tools/medir_fluidez.py --accion "input tap 113 1460" --nombre "Pestaña Inventario"
    python tools/medir_fluidez.py --accion "input keyevent 4" --espera 1.5 --nombre "Volver del editor"

Requiere adb en el PATH y un solo dispositivo conectado. Ejecuta solo `adb shell` sobre el paquete indicado.
"""
import argparse
import subprocess
import sys
import time

PAQUETE = "com.smartfood.app"
CUADRO_LENTO_MS = 1000 / 60  # 16,7 ms
META_PRIMER_CUADRO_MS = 50
META_HUECO_MS = 50
META_LENTOS_PORCENTAJE = 5.0


def adb(*args: str) -> str:
    """Ejecuta `adb shell` con los argumentos y devuelve la salida."""
    resultado = subprocess.run(["adb", "shell", *args], capture_output=True, text=True, check=True)
    return resultado.stdout


def reloj_del_telefono_ns() -> int:
    """Reloj monotónico del teléfono en nanosegundos (el mismo de los vsync de gfxinfo)."""
    segundos = float(adb("cat", "/proc/uptime").split()[0])
    return int(segundos * 1_000_000_000)


def leer_cuadros(salida: str) -> list[tuple[int, int]]:
    """Devuelve (vsync previsto, cuadro terminado) de cada cuadro válido de la sección PROFILEDATA."""
    cuadros: list[tuple[int, int]] = []
    columnas: list[str] | None = None
    dentro = False
    for linea in salida.splitlines():
        if linea.startswith("---PROFILEDATA---"):
            dentro = not dentro
            columnas = None
            continue
        if not dentro:
            continue
        partes = [p for p in linea.strip().split(",") if p != ""]
        if not partes:
            continue
        if partes[0] == "Flags":
            columnas = partes
            continue
        if columnas is None:
            continue
        fila = dict(zip(columnas, partes))
        if fila.get("Flags", "1") != "0":  # cuadros marcados como inválidos o de prueba
            continue
        cuadros.append((int(fila["IntendedVsync"]), int(fila["FrameCompleted"])))
    return sorted(cuadros)


def medir(accion: str, espera: float) -> dict[str, float]:
    adb("dumpsys", "gfxinfo", PAQUETE, "reset")
    inicio = reloj_del_telefono_ns()
    if accion:
        adb(*accion.split())
    time.sleep(espera)
    cuadros = leer_cuadros(adb("dumpsys", "gfxinfo", PAQUETE, "framestats"))
    cuadros = [c for c in cuadros if c[0] >= inicio]
    if not cuadros:
        raise SystemExit("No se dibujó ningún cuadro después de la acción.")
    primer_cuadro = (cuadros[0][0] - inicio) / 1e6
    huecos = [(b[0] - a[0]) / 1e6 for a, b in zip(cuadros, cuadros[1:])]
    lentos = sum(1 for vsync, fin in cuadros if (fin - vsync) / 1e6 > CUADRO_LENTO_MS)
    return {
        "cuadros": len(cuadros),
        "primer_cuadro_ms": primer_cuadro,
        "hueco_max_ms": max(huecos, default=0.0),
        "lentos_pct": 100 * lentos / len(cuadros),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--accion", default="", help='acción de adb shell, por ejemplo "input tap 113 1460"')
    parser.add_argument("--espera", type=float, default=1.0, help="segundos que se espera después de la acción")
    parser.add_argument("--nombre", default="transición", help="nombre de la transición para el reporte")
    args = parser.parse_args()

    r = medir(args.accion, args.espera)
    cumple = (
        r["primer_cuadro_ms"] <= META_PRIMER_CUADRO_MS
        and r["hueco_max_ms"] <= META_HUECO_MS
        and r["lentos_pct"] <= META_LENTOS_PORCENTAJE
    )
    print(
        f"| {args.nombre} | {r['cuadros']} | {r['primer_cuadro_ms']:.0f} ms | {r['hueco_max_ms']:.0f} ms | "
        f"{r['lentos_pct']:.1f} % | {'Cumple' if cumple else 'No cumple'} |"
    )
    return 0 if cumple else 1


if __name__ == "__main__":
    sys.exit(main())
