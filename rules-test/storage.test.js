/**
 * Nombre: storage.test.js
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de storage.rules para las fotos de productos (spec 006, RNF-INV-01, RNF-INV-02, plan §3).
 * Se ejecutan contra el emulador de Storage:
 * firebase emulators:exec --only firestore,storage "npm --prefix rules-test test"
 */
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteObject, getBytes, ref, uploadBytes } from 'firebase/storage';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'smartfood-rules-test',
    storage: { rules: readFileSync(new URL('../storage.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearStorage();
});

// Cada prueba usa su propia ruta: así no depende de que clearStorage borre lo que dejó la anterior.
let n = 0;
let ruta;
beforeEach(() => {
  n += 1;
  ruta = `users/u1/inventory/item${n}/foto.jpg`;
});

const storage = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).storage();
const bytes = (kb) => new Uint8Array(kb * 1024);
const jpeg = { contentType: 'image/jpeg' };

describe('fotos de productos (RNF-INV-01, RNF-INV-02)', () => {
  test('RNF-INV-01 el dueño sube, lee y borra un JPEG', async () => {
    await assertSucceeds(uploadBytes(ref(storage('u1'), ruta), bytes(100), jpeg));
    await assertSucceeds(getBytes(ref(storage('u1'), ruta)));
    await assertSucceeds(deleteObject(ref(storage('u1'), ruta)));
  });

  test('RNF-INV-01 otro usuario o alguien sin sesión no puede subir ni leer', async () => {
    await assertFails(uploadBytes(ref(storage('u2'), ruta), bytes(100), jpeg));
    await assertFails(uploadBytes(ref(storage(null), ruta), bytes(100), jpeg));
    await assertSucceeds(uploadBytes(ref(storage('u1'), ruta), bytes(100), jpeg));
    await assertFails(getBytes(ref(storage('u2'), ruta)));
  });

  test('RNF-INV-02 un archivo de 2 MB o más se rechaza', async () => {
    await assertFails(uploadBytes(ref(storage('u1'), ruta), bytes(2048), jpeg));
    await assertSucceeds(uploadBytes(ref(storage('u1'), ruta), bytes(2047), jpeg));
  });

  test('RNF-INV-02 un archivo que no es JPEG se rechaza', async () => {
    await assertFails(uploadBytes(ref(storage('u1'), ruta), bytes(100), { contentType: 'image/png' }));
  });

  test('una foto ya subida no se puede reemplazar', async () => {
    await assertSucceeds(uploadBytes(ref(storage('u1'), ruta), bytes(100), jpeg));
    await assertFails(uploadBytes(ref(storage('u1'), ruta), bytes(100), jpeg));
  });
});
