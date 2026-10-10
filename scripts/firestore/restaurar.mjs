#!/usr/bin/env node
/**
 * Carga en el emulador de Firestore un respaldo hecho con respaldar.mjs, para probar con una
 * copia de los datos reales.
 *
 *   firebase emulators:start
 *   FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node restaurar.mjs respaldos/respaldo-….json
 *
 * Solo escribe en el emulador: sin FIRESTORE_EMULATOR_HOST no hace nada. Pisa los documentos
 * que ya existan con la misma ruta y deja los demás como están.
 */
import { readFileSync } from "node:fs";
import { parseArgs } from "node:util";
import { GeoPoint, Timestamp } from "firebase-admin/firestore";
import { conectar, contarPorColeccion, enLotes, usaEmulador } from "./conexion.mjs";

/** Escrituras por lote. */
const TAMANO_LOTE = 400;

const { positionals: [archivo] } = parseArgs({ allowPositionals: true });

if (!usaEmulador || !archivo) {
    console.error("Este script solo restaura en el emulador.\n");
    console.error("  FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node restaurar.mjs respaldos/respaldo-….json");
    process.exit(1);
}

const { db, proyecto } = conectar();
const respaldo = JSON.parse(readFileSync(archivo, "utf8"));
const rutas = Object.keys(respaldo.documentos);

for (const lote of enLotes(rutas, TAMANO_LOTE)) {
    const escrituras = db.batch();
    for (const ruta of lote) escrituras.set(db.doc(ruta), deserializar(respaldo.documentos[ruta]));
    await escrituras.commit();
}

console.log(`\n${rutas.length} documentos de "${respaldo.proyecto}" cargados en el emulador ("${proyecto}")\n`);
console.table(contarPorColeccion(rutas));

/** Reconstruye los tipos propios de Firestore a partir de las marcas `__tipo` de respaldar.mjs. */
function deserializar(valor) {
    if (Array.isArray(valor)) return valor.map(deserializar);
    if (valor === null || typeof valor !== "object") return valor;
    switch (valor.__tipo) {
        case "fecha":
            return new Timestamp(valor.segundos, valor.nanosegundos);
        case "geopunto":
            return new GeoPoint(valor.latitud, valor.longitud);
        case "referencia":
            return db.doc(valor.ruta);
        case "bytes":
            return Buffer.from(valor.base64, "base64");
        case "numero":
            return Number(valor.valor);
        default:
            return Object.fromEntries(Object.entries(valor).map(([campo, v]) => [campo, deserializar(v)]));
    }
}
