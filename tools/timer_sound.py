"""
Nombre: timer_sound.py
Autor: SmartFood Team
Fecha: Octubre 2026
Descripción: Prepara el sonido del temporizador de la cocina guiada (spec 009, CA-RIA-04-04, T020) en
`app/src/main/res/raw/timer_alarm.ogg` a partir de "Alarm clock - 01.ogg" de Wikimedia Commons (CC BY-SA 4.0; fuente
y autor en `docs/licenses/sonidos.md`): un despertador digital que hace "bip-bip-bip-bip" una vez por segundo.
Recorta un ciclo de 1 s que empieza y termina en silencio (la app lo repite sin cortes), lo normaliza y silencia el
ruido de fondo entre los bips. No se edita el .ogg a mano: si cambia la fuente, se vuelve a ejecutar.

Requiere `pip install soundfile numpy`.

Uso:
    python tools/timer_sound.py "Alarm_clock_-_01.ogg"
"""
import pathlib
import sys

import numpy as np
import soundfile as sf

RAIZ = pathlib.Path(__file__).resolve().parents[1]
SALIDA = RAIZ / "app" / "src" / "main" / "res" / "raw" / "timer_alarm.ogg"

INICIO_S = 3.30      # silencio justo antes del segundo grupo de bips
PERIODO_S = 1.00     # período medido por autocorrelación de la envolvente
PICO = 0.9           # nivel final de los bips
UMBRAL = 0.15        # fracción del pico por debajo de la cual se silencia (ruido de fondo)
SUAVIZADO_S = 0.004  # subida y bajada de la compuerta, para que no haga "clic"


def main():
    datos, muestreo = sf.read(sys.argv[1])
    if datos.ndim > 1:
        datos = datos.mean(axis=1)
    ciclo = datos[int(INICIO_S * muestreo): int((INICIO_S + PERIODO_S) * muestreo)].copy()
    ciclo *= PICO / np.abs(ciclo).max()
    # Compuerta de ruido: la envolvente (máximo en ventanas de 10 ms) decide qué se oye; la ganancia sube y baja
    # suave para que los bips no empiecen ni terminen en seco.
    ventana = int(0.01 * muestreo)
    envolvente = np.array([np.abs(ciclo[max(0, i - ventana): i + ventana]).max() for i in range(len(ciclo))])
    ganancia = (envolvente > UMBRAL * PICO).astype(float)
    suave = int(SUAVIZADO_S * muestreo)
    ganancia = np.convolve(ganancia, np.ones(suave) / suave, mode="same")
    ciclo *= ganancia
    ciclo[:suave] = 0.0
    ciclo[-suave:] = 0.0
    sf.write(SALIDA, ciclo, muestreo, format="OGG", subtype="VORBIS")
    print(f"{SALIDA.relative_to(RAIZ)}: {len(ciclo) / muestreo:.2f} s, {SALIDA.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
