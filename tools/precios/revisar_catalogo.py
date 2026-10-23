"""
Nombre: revisar_catalogo.py
Autor: SmartFood Team
Fecha: Octubre 2026
Descripción: Revisión de calidad de solo lectura del catálogo de recetas (spec 009, T035). Señala lo que conviene mirar
a mano: tiempos que no cuadran con los pasos, calorías fuera de rango, cantidades por porción raras para la cocina
casera, ingredientes que los pasos no nombran (o pasos que nombran ingredientes que la receta no tiene) e ingredientes
sin sinónimos para emparejar el inventario. No cambia nada.

Uso:
    python tools/precios/revisar_catalogo.py
"""
import json
import pathlib
import unicodedata

RAIZ = pathlib.Path(__file__).resolve().parents[2]
ASSETS = RAIZ / "app" / "src" / "main" / "assets"

# Rango razonable por porción, en la unidad base del ingrediente (cocina casera colombiana).
RANGOS = {
    "pollo": (80, 250), "carne": (80, 250), "pescado": (80, 250), "atun": (40, 170),
    "arroz": (40, 150), "pasta": (50, 150), "lentejas": (30, 120), "frijoles": (30, 120), "garbanzos": (30, 120),
    "papa": (60, 300), "yuca": (60, 300), "platano": (60, 300), "harina-maiz": (40, 150),
    "aceite": (0, 20), "sal": (0, 3), "azucar": (0, 25), "huevo": (0.5, 3), "leche": (0, 300), "queso": (10, 120),
}
KCAL = (100, 900)


def norm(texto):
    texto = unicodedata.normalize("NFD", texto.lower())
    return "".join(c for c in texto if unicodedata.category(c) != "Mn")


def raices(ingrediente):
    """Formas con que un paso puede nombrar el ingrediente: nombre, sinónimos y su raíz sin plural."""
    formas = {norm(ingrediente["nombre"])} | {norm(s) for s in ingrediente.get("sinonimos", [])}
    salida = set()
    for f in formas:
        salida.add(f)
        primera = f.split()[0]
        salida.add(primera.rstrip("s") if len(primera) > 4 else primera)
    return salida


def main():
    ingredientes = {i["id"]: i for i in json.loads((ASSETS / "ingredientes.json").read_text(encoding="utf-8"))}
    recetas = json.loads((ASSETS / "recetas.json").read_text(encoding="utf-8"))
    hallazgos = []
    for r in recetas:
        rid = r["id"]
        total = r["prepMin"] + r["coccionMin"] + r["reposoMin"]
        pasos_min = sum(p.get("minutos") or 0 for p in r["pasos"])
        if pasos_min > total + 5:
            hallazgos.append((rid, "tiempo", f"los pasos suman {pasos_min} min y el total es {total} min"))
        if r["coccionMin"] >= 15 and pasos_min == 0:
            hallazgos.append((rid, "tiempo", f"{r['coccionMin']} min de cocción y ningún paso tiene minutos"))
        if not KCAL[0] <= r["kcal"] <= KCAL[1]:
            hallazgos.append((rid, "kcal", f"{r['kcal']} kcal por porción"))
        texto = norm(" ".join([p["titulo"] + " " + p["texto"] for p in r["pasos"]] + r.get("antesDeEmpezar", [])))
        usados = set()
        for x in r["ingredientes"]:
            i = ingredientes[x["id"]]
            usados.add(x["id"])
            por_porcion = x["cantidad"] / r["porciones"]
            if x["id"] in RANGOS:
                lo, hi = RANGOS[x["id"]]
                if not lo <= por_porcion <= hi:
                    hallazgos.append((rid, "cantidad", f"{i['nombre']}: {por_porcion:g} {i['unidad']} por porción (esperado {lo}-{hi})"))
            if not any(f in texto for f in raices(i)):
                hallazgos.append((rid, "pasos", f"ningún paso nombra {i['nombre']}"))
        # Nombres de la receta: "cebolla" dentro de "cebolla larga" no cuenta como otro ingrediente.
        propios = " | ".join(norm(ingredientes[u]["nombre"]) for u in usados)
        for iid, i in ingredientes.items():
            if iid in usados or i.get("basico"):
                continue
            nombre = norm(i["nombre"])
            if len(nombre) > 3 and nombre not in propios and f" {nombre} " in f" {texto} ":
                hallazgos.append((rid, "pasos", f"un paso nombra {i['nombre']}, que no está en la receta"))
    for iid, i in ingredientes.items():
        if not i.get("sinonimos"):
            hallazgos.append((iid, "sinonimos", f"{i['nombre']} no tiene sinónimos para el inventario"))
    for donde, tipo, detalle in hallazgos:
        print(f"[{tipo}] {donde}: {detalle}")
    print(f"\n{len(hallazgos)} hallazgos en {len(recetas)} recetas y {len(ingredientes)} ingredientes.")


if __name__ == "__main__":
    main()
