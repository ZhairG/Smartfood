"""
Nombre: generate_launcher_icon.py
Autor: SmartFood Team
Fecha: Octubre 2026
Descripción: Genera el ícono oficial de la app a partir del logo original (spec 002, HU-UI-06).

    python tools/generate_launcher_icon.py        (requiere Pillow: pip install pillow)

Fuente: app/src/main/res/drawable-nodpi/logo_smartfood_circle.png

Salidas:
  - mipmap-*/ic_launcher_foreground.webp   capa frontal del ícono adaptativo (lienzo de 108 dp)
  - mipmap-*/ic_launcher_monochrome.webp   silueta para íconos temáticos de Android 13+
  - mipmap-*/ic_launcher.webp              ícono cuadrado redondeado para Android 7.x (48 dp)
  - mipmap-*/ic_launcher_round.webp        ícono redondo para Android 7.x (48 dp)
  - ic_launcher-playstore.png              ícono de 512×512 para Google Play

El logo se escala para que TODO píxel visible quede dentro del círculo de 66 dp (zona segura del ícono adaptativo),
así ninguna máscara del launcher lo recorta (CA-UI-06-02). Todas las salidas se obtienen con un único reescalado
LANCZOS desde el original y se guardan sin pérdida.
"""

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app" / "src" / "main"
RES = MAIN / "res"
SOURCE = RES / "drawable-nodpi" / "logo_smartfood_circle.png"

# GreenLight (#F0FDF4): el mismo token que la Splash y @color/smartfood_green_light.
BACKGROUND = (0xF0, 0xFD, 0xF4, 0xFF)

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}

ADAPTIVE_DP = 108       # lienzo completo de cada capa del ícono adaptativo
VISIBLE_DP = 72         # área que muestran las máscaras del launcher
SAFE_RADIUS_DP = 33     # zona segura: círculo de 66 dp
SAFETY_MARGIN = 0.93    # holgura para el halo de suavizado del reescalado (~2 px en mdpi)
LEGACY_DP = 48          # íconos de Android 7.x
PLAY_STORE_PX = 512


def visible_radius(image: Image.Image) -> float:
    """Mayor distancia, en px, de un píxel visible (alfa > 0) al centro de la imagen."""
    alpha = image.getchannel("A")
    width, height = image.size
    cx, cy = (width - 1) / 2, (height - 1) / 2
    data = alpha.load()
    best = 0.0
    for y in range(height):
        for x in range(width):
            if data[x, y] > 0:
                best = max(best, math.hypot(x - cx, y - cy))
    return best


def build_master() -> tuple[Image.Image, float]:
    """
    Capa frontal maestra de 108 dp con el logo a su resolución nativa (sin escalar).

    Devuelve la imagen y su densidad en px por dp.
    """
    logo = Image.open(SOURCE).convert("RGBA")
    logo = logo.crop(logo.getchannel("A").getbbox())

    radius_px = visible_radius(logo)
    px_per_dp = radius_px / (SAFE_RADIUS_DP * SAFETY_MARGIN)
    side = math.ceil(ADAPTIVE_DP * px_per_dp)
    side += side % 2 != logo.width % 2  # misma paridad: el logo queda centrado al píxel

    master = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    master.paste(logo, ((side - logo.width) // 2, (side - logo.height) // 2), logo)
    return master, side / ADAPTIVE_DP


def resize(image: Image.Image, size: int) -> Image.Image:
    return image.resize((size, size), Image.Resampling.LANCZOS)


def assert_inside_safe_zone(layer: Image.Image, name: str) -> None:
    """Falla si algún píxel visible queda fuera del círculo de 66 dp (CA-UI-06-02)."""
    px_per_dp = layer.width / ADAPTIVE_DP
    limit = SAFE_RADIUS_DP * px_per_dp
    radius = visible_radius(layer)
    assert radius <= limit, f"{name}: el logo sale de la zona segura ({radius:.1f} px > {limit:.1f} px)"


def save_webp(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "WEBP", lossless=True, quality=100, method=6)


def monochrome(layer: Image.Image) -> Image.Image:
    """Silueta blanca con el alfa del logo; el sistema la tiñe (Android 13+)."""
    white = Image.new("RGBA", layer.size, (255, 255, 255, 255))
    white.putalpha(layer.getchannel("A"))
    return white


def flattened_visible_area(master: Image.Image) -> Image.Image:
    """Fondo GreenLight + logo, recortado al área visible de 72 dp, como lo muestra el launcher."""
    composed = Image.new("RGBA", master.size, BACKGROUND)
    composed.alpha_composite(master)
    px_per_dp = master.width / ADAPTIVE_DP
    inset = round((ADAPTIVE_DP - VISIBLE_DP) / 2 * px_per_dp)
    return composed.crop((inset, inset, master.width - inset, master.height - inset))


def masked(image: Image.Image, size: int, round_icon: bool) -> Image.Image:
    """Ícono de Android 7.x: círculo o cuadrado redondeado, con 1 dp de margen."""
    scale = 4  # se dibuja la máscara a 4× para que el borde quede suave
    big = size * scale
    margin = big // LEGACY_DP
    mask = Image.new("L", (big, big), 0)
    draw = ImageDraw.Draw(mask)
    box = (margin, margin, big - margin - 1, big - margin - 1)
    if round_icon:
        draw.ellipse(box, fill=255)
    else:
        draw.rounded_rectangle(box, radius=big * 0.18, fill=255)
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)

    icon = resize(image, size)
    icon.putalpha(mask)
    return icon


def main() -> None:
    master, master_px_per_dp = build_master()
    print(f"Logo fuente: {SOURCE.relative_to(ROOT)} -> capa maestra {master.width}px ({master_px_per_dp:.2f} px/dp)")
    visible = flattened_visible_area(master)

    for name, factor in DENSITIES.items():
        folder = RES / f"mipmap-{name}"

        foreground = resize(master, round(ADAPTIVE_DP * factor))
        assert_inside_safe_zone(foreground, f"{name}/ic_launcher_foreground")
        save_webp(foreground, folder / "ic_launcher_foreground.webp")
        save_webp(monochrome(foreground), folder / "ic_launcher_monochrome.webp")

        legacy = round(LEGACY_DP * factor)
        save_webp(masked(visible, legacy, round_icon=False), folder / "ic_launcher.webp")
        save_webp(masked(visible, legacy, round_icon=True), folder / "ic_launcher_round.webp")

        print(f"  mipmap-{name}: capas de {foreground.width}px, íconos de Android 7.x de {legacy}px")

    play_store = resize(visible, PLAY_STORE_PX).convert("RGB")
    play_store.save(MAIN / "ic_launcher-playstore.png", "PNG", optimize=True)
    print(f"  ic_launcher-playstore.png: {PLAY_STORE_PX}px")
    print("Zona segura de 66 dp verificada en todas las densidades.")


if __name__ == "__main__":
    main()
