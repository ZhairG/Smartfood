/**
 * Nombre: firestore.test.js
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de firestore.rules para el inventario (spec 006, RNF-INV-01, plan §3) y regresión del
 * perfil (spec 005, CA-LEG-02-02). Se ejecutan contra el emulador de Firestore:
 * firebase emulators:exec --only firestore,storage "npm --prefix rules-test test"
 */
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteField, doc, getDoc, serverTimestamp, setDoc, deleteDoc, Timestamp } from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'smartfood-rules-test',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore();
const item = (uid, id = 'item1') => doc(db(uid), `users/${uid ?? 'u1'}/inventory/${id}`);

/** Documento válido tal como lo escribe FirestoreInventoryRepository al crear. */
const valido = (cambios = {}) => ({
  name: 'Leche Entera',
  nameNormalized: 'leche entera',
  category: 'DAIRY',
  expiresOn: '2026-10-20',
  createdAt: serverTimestamp(),
  updatedAt: serverTimestamp(),
  ...cambios,
});

/** Lee un documento saltándose las reglas (withSecurityRulesDisabled no devuelve el valor del callback). */
async function leerSinReglas(ruta) {
  let snapshot;
  await env.withSecurityRulesDisabled(async (ctx) => {
    snapshot = await getDoc(doc(ctx.firestore(), ruta));
  });
  return snapshot;
}

/** Crea un documento saltándose las reglas, para probar ediciones. */
async function sembrar(datos = {}) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/u1/inventory/item1'), {
      ...valido(),
      createdAt: Timestamp.fromDate(new Date('2026-10-01T10:00:00Z')),
      updatedAt: Timestamp.fromDate(new Date('2026-10-01T10:00:00Z')),
      ...datos,
    });
  });
}

describe('inventario: dueño y privacidad (RNF-INV-01)', () => {
  test('RNF-INV-01 el dueño crea, lee, edita y borra su producto', async () => {
    await assertSucceeds(setDoc(item('u1'), valido({ quantity: 1.5, unit: 'LITERS' })));
    await assertSucceeds(getDoc(item('u1')));
    const snapshot = await leerSinReglas('users/u1/inventory/item1');
    await assertSucceeds(setDoc(item('u1'), valido({ createdAt: snapshot.data().createdAt })));
    await assertSucceeds(deleteDoc(item('u1')));
  });

  test('RNF-INV-01 otro usuario no puede leer, crear ni borrar', async () => {
    await sembrar();
    const ajeno = doc(db('u2'), 'users/u1/inventory/item1');
    await assertFails(getDoc(ajeno));
    await assertFails(setDoc(doc(db('u2'), 'users/u1/inventory/item2'), valido()));
    await assertFails(deleteDoc(ajeno));
  });

  test('RNF-INV-01 sin sesión no hay acceso', async () => {
    await sembrar();
    const anonimo = doc(db(null), 'users/u1/inventory/item1');
    await assertFails(getDoc(anonimo));
    await assertFails(setDoc(doc(db(null), 'users/u1/inventory/item2'), valido()));
  });
});

describe('inventario: validación en el servidor (RNF-INV-01)', () => {
  test('CA-INV-02-03 el nombre vacío o de más de 50 caracteres se rechaza', async () => {
    await assertFails(setDoc(item('u1'), valido({ name: '' })));
    await assertFails(setDoc(item('u1'), valido({ name: 'x'.repeat(51) })));
    await assertSucceeds(setDoc(item('u1'), valido({ name: 'x'.repeat(50) })));
  });

  test('CA-INV-02-05 la categoría debe ser una de las seis', async () => {
    await assertFails(setDoc(item('u1'), valido({ category: 'SNACKS' })));
  });

  test('la fecha debe ser un día "yyyy-MM-dd"', async () => {
    await assertFails(setDoc(item('u1'), valido({ expiresOn: '20/10/2026' })));
    await assertFails(setDoc(item('u1'), valido({ expiresOn: Timestamp.now() })));
  });

  test('CA-INV-02-08 cantidad y unidad van juntas', async () => {
    await assertSucceeds(setDoc(item('u1', 'a'), valido({ quantity: 2, unit: 'UNITS' })));
    await assertFails(setDoc(item('u1', 'b'), valido({ quantity: 2 })));
    await assertFails(setDoc(item('u1', 'c'), valido({ unit: 'UNITS' })));
    await assertFails(setDoc(item('u1', 'd'), valido({ quantity: 2, unit: 'CUPS' })));
  });

  test('CA-INV-02-09 la cantidad debe ser mayor que 0 y como máximo 9999', async () => {
    await assertFails(setDoc(item('u1', 'a'), valido({ quantity: 0, unit: 'GRAMS' })));
    await assertFails(setDoc(item('u1', 'b'), valido({ quantity: 10000, unit: 'GRAMS' })));
    await assertFails(setDoc(item('u1', 'c'), valido({ quantity: '5', unit: 'GRAMS' })));
    await assertSucceeds(setDoc(item('u1', 'd'), valido({ quantity: 9999, unit: 'GRAMS' })));
  });

  test('CA-INV-02-10 el precio es opcional y entero en pesos', async () => {
    await assertSucceeds(setDoc(item('u1', 'a'), valido({ price: 12500 })));
    await assertSucceeds(setDoc(item('u1', 'b'), valido({ price: 99999999 })));
  });

  test('CA-INV-02-11 el precio debe ser entero, mayor que 0 y como máximo 99.999.999', async () => {
    await assertFails(setDoc(item('u1', 'a'), valido({ price: 0 })));
    await assertFails(setDoc(item('u1', 'b'), valido({ price: 100000000 })));
    await assertFails(setDoc(item('u1', 'c'), valido({ price: 12.5 })));
    await assertFails(setDoc(item('u1', 'd'), valido({ price: '12500' })));
  });

  test('la ruta y la URL de la foto van juntas', async () => {
    await assertSucceeds(setDoc(item('u1', 'a'), valido({ photoPath: 'users/u1/inventory/a/f.jpg', photoUrl: 'https://f' })));
    await assertFails(setDoc(item('u1', 'b'), valido({ photoPath: 'users/u1/inventory/b/f.jpg' })));
  });

  test('una clave que no es del modelo se rechaza', async () => {
    await assertFails(setDoc(item('u1'), valido({ brand: 'Alpina' })));
  });
});

describe('inventario: marcas de tiempo', () => {
  test('al crear, createdAt y updatedAt deben ser la hora del servidor', async () => {
    await assertFails(setDoc(item('u1', 'a'), valido({ createdAt: Timestamp.now() })));
    await assertFails(setDoc(item('u1', 'b'), valido({ updatedAt: Timestamp.now() })));
  });

  test('al editar, createdAt no puede cambiar', async () => {
    await sembrar();
    await assertFails(setDoc(item('u1'), valido()));
  });

  test('al editar se puede quitar la cantidad conservando createdAt', async () => {
    await sembrar({ quantity: 3, unit: 'UNITS' });
    const actual = await leerSinReglas('users/u1/inventory/item1');
    await assertSucceeds(setDoc(item('u1'), valido({ createdAt: actual.data().createdAt })));
  });

  test('una actualización parcial que deja la cantidad sin unidad se rechaza', async () => {
    await sembrar({ quantity: 3, unit: 'UNITS' });
    const actual = await leerSinReglas('users/u1/inventory/item1');
    await assertFails(setDoc(item('u1'), { unit: deleteField(), updatedAt: serverTimestamp(),
      createdAt: actual.data().createdAt }, { merge: true }));
  });
});

describe('perfil: edición (spec 008, RNF-PER-01)', () => {
  const perfil = (uid = 'u1') => doc(db(uid), 'users/u1');
  const base = { fullName: 'Ana Pérez', email: 'ana@correo.com', termsVersion: '2026-10', termsAcceptedAt: Timestamp.now(),
    createdAt: Timestamp.now() };
  const sembrarPerfil = async () => env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/u1'), base);
  });
  const editar = (cambios, uid = 'u1') =>
    setDoc(perfil(uid), { ...base, ...cambios, updatedAt: serverTimestamp() }, { merge: false });

  test('CA-PER-02-04 el dueño edita nombre, teléfono, fecha y foto', async () => {
    await sembrarPerfil();
    await assertSucceeds(editar({
      fullName: 'Ana María', phone: '3001234567', birthDate: '2000-05-17',
      photoPath: 'users/u1/profile/a.jpg', photoUrl: 'https://foto',
    }));
  });

  test('CA-PER-02-12 sincronizar el correo solo cambia el correo y updatedAt', async () => {
    await sembrarPerfil();
    await assertSucceeds(editar({ email: 'nuevo@correo.com' }));
  });

  test('RNF-PER-01 la constancia de términos y createdAt no se pueden cambiar', async () => {
    await sembrarPerfil();
    await assertFails(editar({ termsVersion: '2099-01' }));
    await assertFails(editar({ createdAt: Timestamp.fromMillis(0) }));
  });

  test('CA-PER-02-05 CA-PER-02-06 nombre y teléfono inválidos se rechazan', async () => {
    await sembrarPerfil();
    await assertFails(editar({ fullName: 'A' }));
    await assertFails(editar({ fullName: 'x'.repeat(51) }));
    await assertFails(editar({ phone: '+57 300 123 4567' }));
    await assertFails(editar({ phone: '2001234567' }));
    await assertFails(editar({ birthDate: '17/05/2000' }));
  });

  test('RNF-PER-01 campos desconocidos, foto ajena, sin updatedAt del servidor u otro usuario se rechazan', async () => {
    await sembrarPerfil();
    await assertFails(editar({ admin: true }));
    await assertFails(editar({ photoPath: 'users/u2/profile/a.jpg', photoUrl: 'https://foto' }));
    await assertFails(editar({ photoPath: 'users/u1/profile/a.jpg' }));
    await assertFails(setDoc(perfil(), { ...base, fullName: 'Ana María', updatedAt: Timestamp.fromMillis(0) }));
    await assertFails(editar({ fullName: 'Intruso' }, 'u2'));
  });
});

describe('perfil (regresión, spec 005)', () => {
  test('CA-LEG-02-02 el perfil sigue exigiendo la constancia legal', async () => {
    const perfil = doc(db('u1'), 'users/u1');
    await assertFails(setDoc(perfil, { fullName: 'Ana', email: 'ana@correo.com' }));
    await assertSucceeds(setDoc(perfil, {
      fullName: 'Ana', email: 'ana@correo.com', termsVersion: '2026-10', termsAcceptedAt: serverTimestamp(),
    }));
  });
});
