"""
Nombre: consultar.py
Autor: SmartFood Team
Fecha: Octubre 2026
Descripción: Consulta puntual de precios de los ingredientes del catálogo de recetas en los buscadores públicos de
Éxito, Carulla, Olímpica (catálogo VTEX) y D1 (búsqueda inteligente de VTEX) (spec 009, T034). Escribe un BORRADOR con
los candidatos de cada tienda; el emparejamiento automático se equivoca (pasta de cebolla por cebolla, semillas por
calabacín…), así que el borrador se revisa a mano y lo elegido se copia a `docs/precios/precios.json`.

Uso:
    python tools/precios/consultar.py cebolla tomate        # solo esos ingredientes
    python tools/precios/consultar.py --todos               # todos los de busquedas.json
    python tools/precios/consultar.py --todos --salida borrador.json

Las búsquedas (texto, cómo debe empezar el nombre y palabras a excluir) están en `tools/precios/busquedas.json`.
Hay una pausa entre pedidos para no cargar los sitios.
"""
import argparse
import json
import pathlib
import re
import sys
import time
import urllib.parse
import urllib.request

AQUI = pathlib.Path(__file__).resolve().parent
TABLA = AQUI.parent.parent / "docs" / "precios" / "precios.json"
UA = "Mozilla/5.0 (SmartFood proyecto universitario UTADEO; consulta puntual de precios)"
PAUSA_S = 0.5

# Catálogo VTEX (Éxito, Carulla, Olímpica) y búsqueda inteligente (D1, que en el catálogo devuelve poco).
TIENDAS = {
    "exito": ("https://www.exito.com", "catalogo"),
    "carulla": ("https://www.carulla.com", "catalogo"),
    "olimpica": ("https://www.olimpica.com", "catalogo"),
    "d1": ("https://www.d1.com.co", "inteligente"),
}

TAMANO = re.compile(r"(\d+(?:[.,]\d+)?)\s*(kg|kilo|kilos|gr|g|grs|gramos|ml|cc|l|lt|lts|litro|litros|und|unidades|un|uds)\b")


def normalizar(texto):
    texto = texto.lower()
    for a, b in (("á", "a"), ("é", "e"), ("í", "i"), ("ó", "o"), ("ú", "u"), ("ñ", "n")):
        texto = texto.replace(a, b)
    return texto


def tamano_del_nombre(nombre):
    """Tamaño que dice el nombre, en gramos, mililitros o unidades; `None` si no dice."""
    n = normalizar(nombre)
    for m in TAMANO.finditer(n):
        valor = float(m.group(1).replace(",", "."))
        unidad = m.group(2)
        if unidad in ("kg", "kilo", "kilos"):
            return valor * 1000, "g"
        if unidad in ("gr", "g", "grs", "gramos"):
            return valor, "g"
        if unidad in ("ml", "cc"):
            return valor, "ml"
        if unidad in ("l", "lt", "lts", "litro", "litros"):
            return valor * 1000, "ml"
        return valor, "und"
    if re.search(r"\bx\s*kg\b|por kilo|\bkg\b|a granel", n):
        return 1000.0, "g"
    return None, None


def pedir(url):
    pedido = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    with urllib.request.urlopen(pedido, timeout=30) as respuesta:
        return json.loads(respuesta.read().decode("utf-8"))


def buscar(tienda, texto):
    base, modo = TIENDAS[tienda]
    q = urllib.parse.quote(texto)
    if modo == "catalogo":
        return pedir(f"{base}/api/catalog_system/pub/products/search?ft={q}&_from=0&_to=29")
    return pedir(f"{base}/api/io/_v/api/intelligent-search/product_search/?query={q}&count=30&locale=es-CO").get("products", [])


def candidatos(base, productos, debe, excluir):
    """Productos disponibles cuyo nombre empieza con el ingrediente, con su precio y tamaño."""
    salida = []
    for p in productos:
        nombre = p.get("productName", "")
        n = normalizar(nombre)
        if not any(n.startswith(normalizar(d)) for d in debe) or any(normalizar(x) in n for x in excluir):
            continue
        try:
            item = p["items"][0]
            oferta = item["sellers"][0]["commertialOffer"]
        except (KeyError, IndexError):
            continue
        precio = oferta.get("Price") or 0
        disponible = oferta.get("IsAvailable", oferta.get("AvailableQuantity", 0) > 0)
        if precio <= 0 or not disponible:
            continue
        tamano, unidad = tamano_del_nombre(nombre)
        medida = (item.get("measurementUnit") or "").lower()
        # Productos por peso: VTEX da el precio por kilo y vende en múltiplos de `unitMultiplier`.
        if medida in ("kg", "kgs", "kilo"):
            tamano, unidad = 1000.0, "g"
        salida.append({
            "producto": nombre,
            "precio": round(precio),
            "tamano": tamano,
            "unidad": unidad,
            "url": f"{base}/{p.get('linkText', '')}/p",
        })
    return salida


def main():
    parser = argparse.ArgumentParser(description="Consulta puntual de precios (borrador para revisar a mano).")
    parser.add_argument("ingredientes", nargs="*", help="ids de ingredientes de busquedas.json")
    parser.add_argument("--todos", action="store_true", help="consultar todos los ingredientes")
    parser.add_argument("--salida", default="borrador-precios.json", help="archivo del borrador")
    args = parser.parse_args()

    busquedas = json.loads((AQUI / "busquedas.json").read_text(encoding="utf-8"))
    # Presentación de referencia de la tabla revisada: los candidatos más parecidos van primero.
    tabla = json.loads(TABLA.read_text(encoding="utf-8"))["ingredientes"] if TABLA.exists() else {}
    ids = list(busquedas) if args.todos else args.ingredientes
    desconocidos = [i for i in ids if i not in busquedas]
    if not ids or desconocidos:
        sys.exit(f"Ingredientes sin búsqueda en busquedas.json: {desconocidos or 'ninguno pedido'}")

    borrador = {"aviso": "BORRADOR: revisar a mano antes de copiar a docs/precios/precios.json", "ingredientes": {}}
    for iid in ids:
        b = busquedas[iid]
        por_tienda = {}
        for tienda in TIENDAS:
            try:
                encontrados = candidatos(TIENDAS[tienda][0], buscar(tienda, b["busqueda"]), b["debe_empezar_con"], b["excluir"])
            except Exception as error:  # noqa: BLE001 - un sitio caído no detiene la consulta
                por_tienda[tienda] = {"error": str(error)}
                continue
            referencia = tabla.get(iid, {}).get("empaque", {}).get("cantidad")
            if referencia:
                encontrados.sort(key=lambda c: (abs((c["tamano"] or 0) - referencia) / referencia, c["precio"]))
            por_tienda[tienda] = encontrados[:5]
            time.sleep(PAUSA_S)
        borrador["ingredientes"][iid] = por_tienda
        resumen = {t: (len(v) if isinstance(v, list) else "error") for t, v in por_tienda.items()}
        print(iid, resumen, flush=True)

    pathlib.Path(args.salida).write_text(json.dumps(borrador, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"Borrador en {args.salida}. Revísalo y copia lo elegido a docs/precios/precios.json.")


if __name__ == "__main__":
    main()
