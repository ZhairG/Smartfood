# Precios de mercado del catálogo de recetas

Herramientas para actualizar los precios por supermercado de `app/src/main/assets/ingredientes.json` (spec 009, T030 y
T034). La fuente de verdad es **`docs/precios/precios.json`**: por ingrediente, la presentación de mercado (`empaque`),
si es básico de despensa y, por tienda (`exito`, `carulla`, `olimpica`, `d1`), el precio y el producto elegido.

## Pasos

1. **Consultar** (borrador). Usa los buscadores públicos de cada tienda; es una consulta puntual, con pausa entre
   pedidos:
   ```bash
   python tools/precios/consultar.py --todos --salida borrador-precios.json
   python tools/precios/consultar.py arroz huevo          # solo algunos
   ```
   Las búsquedas (texto, cómo debe empezar el nombre y palabras a excluir) están en `busquedas.json`. Para un
   ingrediente nuevo, agrega su búsqueda ahí.
2. **Revisar a mano.** El emparejamiento automático se equivoca (pasta de cebolla por cebolla, semillas por calabacín,
   paquetes de varias latas…). Para cada ingrediente y tienda, elige el producto común del mercado, lleva su precio a
   la presentación de referencia si el tamaño es otro y anota el producto real con su precio. Si la tienda no lo
   tiene en línea, déjala fuera: la app usa la más barata y la nombra. Cambia `fecha` en `precios.json`.
3. **Aplicar** al catálogo, que también regenera `docs/precios/precios-<mes>-<año>.md`:
   ```bash
   python tools/precios/aplicar.py
   python tools/precios/aplicar.py --check    # comprueba que el catálogo coincide con la tabla
   ```
4. **Probar** el catálogo:
   ```bash
   ./gradlew :app:testDebugUnitTest --tests "*RecipeCatalogTest" --tests "*MealPlannerCatalogTest"
   ```

Ara no publica su catálogo con precios en línea, así que no se incluye. No hay una API pública de precios por cadena en
Colombia; el SIPSA del DANE publica precios mayoristas, no de supermercado.
