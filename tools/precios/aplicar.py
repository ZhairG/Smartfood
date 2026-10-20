"""
Nombre: aplicar.py
Autor: SmartFood Team
Fecha: Octubre 2026
Descripción: Aplica la tabla revisada de precios `docs/precios/precios.json` (fuente de verdad) al catálogo de la app
(spec 009, T034): escribe `empaque`, `precios` y `basico` de cada ingrediente en `app/src/main/assets/ingredientes.json`
sin tocar los demás campos, y regenera `docs/precios/precios-<mes>-<año>.md` con la fecha de la tabla.

Uso:
    python tools/precios/aplicar.py           # aplica y regenera la tabla en Markdown
    python tools/precios/aplicar.py --check   # falla (código 1) si el catálogo no coincide con la tabla

Después de aplicar, correr `RecipeCatalogTest` (`./gradlew :app:testDebugUnitTest --tests "*RecipeCatalogTest"`).
"""
import argparse
import json
import pathlib
import sys

RAIZ = pathlib.Path(__file__).resolve().parents[2]
TABLA = RAIZ / "docs" / "precios" / "precios.json"
CATALOGO = RAIZ / "app" / "src" / "main" / "assets" / "ingredientes.json"
TIENDAS = ("exito", "carulla", "olimpica", "d1")
NOMBRES = {"exito": "Éxito", "carulla": "Carulla", "olimpica": "Olímpica", "d1": "D1"}
MESES = ("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre",
         "noviembre", "diciembre")


def campos(entrada):
    """Campos de precio de un ingrediente, en el orden del catálogo."""
    salida = {
        "empaque": entrada["empaque"],
        "precios": {t: entrada["precios"][t]["precio"] for t in TIENDAS if t in entrada["precios"]},
    }
    if entrada.get("basico"):
        salida["basico"] = True
    return salida


def con_precios(ingrediente, entrada):
    """El ingrediente con los campos de la tabla, sin cambiar el resto ni su orden."""
    base = {k: v for k, v in ingrediente.items() if k not in ("empaque", "precios", "basico")}
    return {**base, **campos(entrada)}


def escribir_catalogo(ingredientes):
    """Mismo formato del archivo: un ingrediente por bloque y cada valor en una línea."""
    lineas = ["["]
    for i, item in enumerate(ingredientes):
        lineas.append("  {")
        claves = list(item)
        for j, clave in enumerate(claves):
            coma = "," if j < len(claves) - 1 else ""
            lineas.append(f'    "{clave}": {json.dumps(item[clave], ensure_ascii=False)}{coma}')
        lineas.append("  }" + ("," if i < len(ingredientes) - 1 else ""))
    lineas.append("]")
    CATALOGO.write_text("\n".join(lineas) + "\n", encoding="utf-8", newline="\n")


def escribir_markdown(tabla):
    anio, mes, dia = tabla["fecha"].split("-")
    nombre = f"precios-{MESES[int(mes) - 1]}-{anio}.md"
    pesos = lambda v: "$" + f"{v:,}".replace(",", ".")  # noqa: E731
    lineas = [
        f"# Precios de referencia de mercado ({MESES[int(mes) - 1]} {anio})",
        "",
        f"Consulta puntual del **{tabla['fecha']}** en los buscadores públicos de cada tienda (www.exito.com, "
        "www.carulla.com, www.olimpica.com y www.d1.com.co). Ara no publica su catálogo con precios en línea y no se "
        "incluye. Spec 009, T030 y T034. **Generado por `tools/precios/aplicar.py` desde `docs/precios/precios.json`; no "
        "editar a mano.**",
        "",
        "- **Presentación:** la que se compra en el mercado. Si la tienda vende otro tamaño, el precio se lleva a la "
        "presentación de referencia y se anota el producto real con su precio entre paréntesis. Las verduras y frutas "
        "sueltas se venden por kilo; las que se venden por unidad usan un peso aproximado por unidad.",
        "- **Vacío:** la tienda no lo tiene en línea; la app usa la tienda más barata y la nombra.",
        "- **Básico de despensa:** no suma al costo de la receta ni al presupuesto del agente.",
        "",
        "| Ingrediente | Presentación | Básico | Éxito | Carulla | Olímpica | D1 |",
        "|---|---|---|---|---|---|---|",
    ]
    for iid, e in tabla["ingredientes"].items():
        celdas = [pesos(e["precios"][t]["precio"]) if t in e["precios"] else "—" for t in TIENDAS]
        lineas.append(f"| {iid} | {e['empaque']['etiqueta']} | {'sí' if e.get('basico') else ''} | " + " | ".join(celdas) + " |")
    lineas += ["", "## Productos usados", ""]
    for iid, e in tabla["ingredientes"].items():
        usados = "; ".join(f"{NOMBRES[t]}: {e['precios'][t]['producto']}" for t in TIENDAS if t in e["precios"])
        lineas.append(f"- **{iid}** ({e['empaque']['etiqueta']}): {usados}")
    (TABLA.parent / nombre).write_text("\n".join(lineas) + "\n", encoding="utf-8", newline="\n")
    return nombre


def main():
    parser = argparse.ArgumentParser(description="Aplica docs/precios/precios.json al catálogo de la app.")
    parser.add_argument("--check", action="store_true", help="solo comprobar que el catálogo coincide")
    args = parser.parse_args()

    tabla = json.loads(TABLA.read_text(encoding="utf-8"))
    ingredientes = json.loads(CATALOGO.read_text(encoding="utf-8"))
    ids = [i["id"] for i in ingredientes]
    faltan = [i for i in ids if i not in tabla["ingredientes"]]
    sobran = [i for i in tabla["ingredientes"] if i not in ids]
    if faltan or sobran:
        sys.exit(f"La tabla y el catálogo no coinciden. Sin precio: {faltan}. Sin ingrediente: {sobran}.")
    sin_precio = [i for i, e in tabla["ingredientes"].items() if not e["precios"]]
    if sin_precio:
        sys.exit(f"Ingredientes sin ningún precio: {sin_precio}")

    nuevos = [con_precios(i, tabla["ingredientes"][i["id"]]) for i in ingredientes]
    if args.check:
        distintos = [a["id"] for a, b in zip(ingredientes, nuevos) if a != b]
        if distintos:
            sys.exit(f"El catálogo no coincide con la tabla en: {distintos}. Corre tools/precios/aplicar.py.")
        print(f"OK: los {len(ids)} ingredientes coinciden con la tabla del {tabla['fecha']}.")
        return
    escribir_catalogo(nuevos)
    nombre = escribir_markdown(tabla)
    print(f"Catálogo actualizado ({len(ids)} ingredientes) y docs/precios/{nombre} regenerado.")


if __name__ == "__main__":
    main()
