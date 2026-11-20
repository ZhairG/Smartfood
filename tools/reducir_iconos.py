"""
Nombre: reducir_iconos.py
Autor: SmartFood Team
Fecha: Noviembre 2026
Descripción: Reduce los íconos PNG del Figma (`app/src/main/res/drawable-nodpi/ic_figma_*.png`) a 200×200 px
(spec 007, T023, RNF-ANI-01).

Los íconos se exportaron del Figma en 512×512 (spec 006, T053), pero se dibujan entre 18 y 50 dp: el más grande es el
aviso de los diálogos (`iconDialogSize × 0,5`). `painterResource` decodifica el PNG completo en el hilo principal al
componer, y en el V2436 cada uno tardaba unos 3 ms, varios por cuadro (trazados de Perfetto de la T022). A 200 px
(50 dp × 4, la densidad más alta común) se ven igual de nítidos y pesan unas 6 veces menos al decodificar.

Conserva el canal alfa y el dibujo; usa LANCZOS. Es idempotente: salta los que ya miden 200 px o menos. Si se exporta un
ícono nuevo del Figma como PNG, se vuelve a ejecutar.

Uso:
    python tools/reducir_iconos.py
"""
import pathlib
import sys

from PIL import Image

TAMANO = 200
CARPETA = pathlib.Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "drawable-nodpi"


def main() -> int:
    reducidos = 0
    for ruta in sorted(CARPETA.glob("ic_figma_*.png")):
        with Image.open(ruta) as imagen:
            ancho, alto = imagen.size
            if max(ancho, alto) <= TAMANO:
                continue
            escala = TAMANO / max(ancho, alto)
            nuevo = (round(ancho * escala), round(alto * escala))
            reducida = imagen.convert("RGBA").resize(nuevo, Image.LANCZOS)
        reducida.save(ruta, format="PNG", optimize=True)
        print(f"{ruta.name}: {ancho}x{alto} -> {nuevo[0]}x{nuevo[1]}")
        reducidos += 1
    print(f"Reducidos: {reducidos}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
