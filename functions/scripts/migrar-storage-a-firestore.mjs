// Nombre: migrar-storage-a-firestore.mjs
// Autor: SmartFood Team
// Fecha: Noviembre 2026
// Descripción: Copia a Firestore (users/{uid}/media) las fotos del perfil y del inventario que están en Cloud Storage,
// para que la app las siga mostrando en el modo gratuito (spec 012, HU-SPK-06, RF-SPK-07).
//
// Se corre UNA vez, en el computador de alguien con acceso al proyecto, ANTES de quitar la facturación: en el plan
// Spark, Storage ya no responde. No se despliega.
//
//   gcloud auth application-default login                              # credenciales de administrador
//   node functions/scripts/migrar-storage-a-firestore.mjs              # simulación: solo lista (CA-SPK-06-01)
//   node functions/scripts/migrar-storage-a-firestore.mjs --aplicar    # copia y cambia las rutas (CA-SPK-06-02)
//   ... --proyecto <id> --bucket <bucket>                              # otro proyecto o bucket
//
// Es idempotente: lo que ya tiene una ruta users/{uid}/media/… se salta, y nunca borra nada de Storage (CA-SPK-06-03).
// Usa el mismo formato que FirestoreMediaStore en la app: un documento por foto con data, contentType y createdAt.

import { createRequire } from 'node:module';

const require = createRequire(new URL('../package.json', import.meta.url));
const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getStorage } = require('firebase-admin/storage');

const arg = (nombre, porDefecto) => {
  const i = process.argv.indexOf(nombre);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : porDefecto;
};

const PROJECT_ID = arg('--proyecto', 'smartfood-f62a1');
const BUCKET = arg('--bucket', `${PROJECT_ID}.firebasestorage.app`);
const SCHEME = 'smartfood-media';
const MAX_BYTES = 900 * 1024;
const aplicar = process.argv.includes('--aplicar');

initializeApp({ credential: applicationDefault(), projectId: PROJECT_ID, storageBucket: BUCKET });
const db = getFirestore();
const bucket = getStorage().bucket();

const esDeMedia = (uid, ruta) => typeof ruta === 'string' && ruta.startsWith(`users/${uid}/media/`);
const nombreSinExtension = (ruta) => ruta.split('/').pop().replace(/\.[^.]+$/, '');

const totales = { copiadas: 0, saltadas: 0, faltantes: 0, grandes: 0 };

/**
 * Copia la foto de Storage [rutaStorage] a users/{uid}/media/{mediaId} y cambia photoPath y photoUrl de [docRef].
 * El mediaId sale del nombre del archivo, así que una segunda corrida usa el mismo documento.
 */
async function migrar(docRef, uid, mediaId, rutaStorage) {
  const archivo = bucket.file(rutaStorage);
  const [existe] = await archivo.exists();
  if (!existe) {
    totales.faltantes++;
    console.warn(`  falta en Storage: ${rutaStorage}`);
    return;
  }
  const [metadatos] = await archivo.getMetadata();
  const tamano = Number(metadatos.size);
  if (tamano > MAX_BYTES) {
    totales.grandes++;
    console.warn(`  pesa ${tamano} bytes (máximo ${MAX_BYTES}): ${rutaStorage}`);
    return;
  }
  const rutaMedia = `users/${uid}/media/${mediaId}`;
  if (!aplicar) {
    totales.copiadas++;
    console.log(`  copiaría ${rutaStorage} (${tamano} bytes) → ${rutaMedia}`);
    return;
  }
  const [bytes] = await archivo.download();
  await db.doc(rutaMedia).set({ data: bytes, contentType: 'image/jpeg', createdAt: FieldValue.serverTimestamp() });
  await docRef.update({ photoPath: rutaMedia, photoUrl: `${SCHEME}://${rutaMedia}` });
  totales.copiadas++;
  console.log(`  copiada ${rutaStorage} → ${rutaMedia}`);
}

const usuarios = await db.collection('users').get();
console.log(`${aplicar ? 'Copiando' : 'Simulación'} en ${PROJECT_ID} (${BUCKET}): ${usuarios.size} cuentas`);

for (const usuario of usuarios.docs) {
  const uid = usuario.id;
  console.log(`users/${uid}`);

  const rutaPerfil = usuario.get('photoPath');
  if (typeof rutaPerfil === 'string' && !esDeMedia(uid, rutaPerfil)) {
    await migrar(usuario.ref, uid, `profile-${nombreSinExtension(rutaPerfil)}`, rutaPerfil);
  } else {
    totales.saltadas++;
  }

  const productos = await usuario.ref.collection('inventory').get();
  for (const producto of productos.docs) {
    const ruta = producto.get('photoPath');
    if (typeof ruta !== 'string' || esDeMedia(uid, ruta)) {
      totales.saltadas++;
      continue;
    }
    await migrar(producto.ref, uid, `inv-${producto.id}-${nombreSinExtension(ruta)}`, ruta);
  }
}

console.log(
  `\n${aplicar ? 'Copiadas' : 'Por copiar'}: ${totales.copiadas} · saltadas: ${totales.saltadas}` +
    ` · faltan en Storage: ${totales.faltantes} · demasiado grandes: ${totales.grandes}`,
);
if (!aplicar) console.log('Nada cambió. Para copiar de verdad, vuelve a correrlo con --aplicar.');
