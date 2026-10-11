#!/usr/bin/env node
/**
 * Pasa los datos del esquema anterior (users/ y doubles/) al esquema nuevo. Es el segundo paso
 * del corte de la fase 4 de docs/PLAN_AUTENTICACION.md; qué se convierte en qué está en
 * migracion/plan.mjs.
 *
 *   node migrar.mjs --clave /ruta/a/la-clave.json              # muestra qué haría, sin escribir
 *   node migrar.mjs --clave /ruta/a/la-clave.json --escribir   # lo hace
 *
 * La clave también se puede indicar con GOOGLE_APPLICATION_CREDENTIALS. Con
 * FIRESTORE_EMULATOR_HOST definida trabaja sobre el emulador y no hace falta clave.
 *
 * No borra ni modifica users/ ni doubles/. Se puede correr más de una vez: un perfil o una
 * partida que ya existen no se tocan, así que no pisa lo que haya cambiado desde la app. Las
 * estadísticas previas se vuelven a escribir, con el mismo valor mientras los datos viejos no
 * cambien.
 */
import { parseArgs } from "node:util";
import { Timestamp } from "firebase-admin/firestore";
import { conectar, enLotes, leerColecciones, usaEmulador } from "./conexion.mjs";
import { planificarMigracion } from "./migracion/plan.mjs";

/** Documentos que se leen o escriben juntos. */
const TAMANO_LOTE = 300;

const { values: opciones } = parseArgs({
    options: {
        clave: { type: "string" },
        escribir: { type: "boolean", default: false },
    },
});

const conexion = conectar(opciones.clave);
if (!conexion) {
    console.error("Falta la clave de la cuenta de servicio.\n");
    console.error("  node migrar.mjs --clave /ruta/a/la-clave.json [--escribir]");
    process.exit(1);
}
const { db, proyecto } = conexion;

console.log(`Proyecto "${proyecto}"${usaEmulador ? " (emulador)" : ""}\n`);
const plan = planificarMigracion(await leerColecciones(db, ["users", "doubles"]), { fecha: Timestamp.now() });

// Lo que ya está migrado se saltea: desde la app puede haber cambiado (un perfil vinculado a su
// cuenta, una amistad quitada), y volver a escribirlo lo pisaría.
const yaMigrados = await existentes([
    ...plan.perfiles.map((perfil) => `perfiles/${perfil.idPerfil}`),
    ...Object.keys(plan.partidas),
]);
const perfilesNuevos = plan.perfiles.filter((perfil) => !yaMigrados.has(`perfiles/${perfil.idPerfil}`));
const partidasNuevas = Object.keys(plan.partidas).filter((ruta) => !yaMigrados.has(ruta));

const fallidos = [];
if (opciones.escribir) {
    for (const perfil of perfilesNuevos) {
        // Los documentos de un perfil van juntos: o se crean todos o ninguno.
        const lote = db.batch();
        for (const [ruta, datos] of Object.entries(perfil.documentos)) lote.create(db.doc(ruta), datos);
        try {
            await lote.commit();
        } catch (error) {
            fallidos.push(`${perfil.usuario}: ${error.message}`);
        }
    }
    await escribirEnLotes(partidasNuevas.map((ruta) => [ruta, plan.partidas[ruta]]), (lote, referencia, datos) =>
        lote.create(referencia, datos),
    );
    await escribirEnLotes(Object.entries(plan.estadisticas), (lote, referencia, datos) => lote.set(referencia, datos));
}

const amistades = (perfiles) =>
    perfiles.flatMap((perfil) => Object.keys(perfil.documentos)).filter((ruta) => ruta.includes("/amigos/")).length / 2;
const columnaNuevos = opciones.escribir ? "creados" : "a crear";
console.log();
console.table({
    perfiles: { [columnaNuevos]: perfilesNuevos.length - fallidos.length, "ya estaban": plan.perfiles.length - perfilesNuevos.length },
    amistades: { [columnaNuevos]: amistades(perfilesNuevos) },
    partidas: { [columnaNuevos]: partidasNuevas.length, "ya estaban": Object.keys(plan.partidas).length - partidasNuevas.length },
    "estadísticas previas": { [columnaNuevos]: Object.keys(plan.estadisticas).length },
});
console.log(
    `No se migran ${plan.omitidos.usuariosDePrueba} usuarios de prueba ni ${plan.omitidos.partidasDePrueba} partidas en las que juegan.`,
);
imprimirLista("Para revisar", plan.avisos);
imprimirLista("Perfiles que no se pudieron crear", fallidos);

if (!opciones.escribir) console.log("\nNo se escribió nada. Para aplicar los cambios, agregar --escribir.");
if (fallidos.length > 0) process.exitCode = 1;

/** De las rutas indicadas, las de los documentos que ya existen. */
async function existentes(rutas) {
    const encontradas = new Set();
    for (const lote of enLotes(rutas, TAMANO_LOTE)) {
        for (const documento of await db.getAll(...lote.map((ruta) => db.doc(ruta)))) {
            if (documento.exists) encontradas.add(documento.ref.path);
        }
    }
    return encontradas;
}

async function escribirEnLotes(documentos, escribir) {
    for (const grupo of enLotes(documentos, TAMANO_LOTE)) {
        const lote = db.batch();
        for (const [ruta, datos] of grupo) escribir(lote, db.doc(ruta), datos);
        await lote.commit();
    }
}

function imprimirLista(titulo, lineas) {
    if (lineas.length === 0) return;
    console.log(`\n${titulo} (${lineas.length}):`);
    for (const linea of lineas) console.log(`  - ${linea}`);
}
