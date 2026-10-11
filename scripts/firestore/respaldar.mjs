#!/usr/bin/env node
/**
 * Descarga todo Firestore (colecciones y subcolecciones) a un archivo JSON.
 *
 * Solo lee: no modifica nada en la base. Usa el Admin SDK, así que no pasa por las reglas de
 * seguridad y necesita la clave privada de una cuenta de servicio (ver conexion.mjs).
 *
 *   npm install
 *   node respaldar.mjs --clave /ruta/a/la-clave.json [--salida carpeta]
 *
 * La clave también se puede indicar con la variable GOOGLE_APPLICATION_CREDENTIALS. Con
 * FIRESTORE_EMULATOR_HOST definida se respalda el emulador y no hace falta clave.
 *
 * El archivo generado incluye todos los datos de los usuarios (hoy, también las contraseñas):
 * no hay que subirlo al repositorio ni compartirlo.
 */
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";
import { DocumentReference, GeoPoint, Timestamp } from "firebase-admin/firestore";
import { conectar, contarPorColeccion, leerColecciones } from "./conexion.mjs";

const CARPETA_SCRIPT = path.dirname(fileURLToPath(import.meta.url));

const { values: opciones } = parseArgs({
    options: {
        clave: { type: "string" },
        salida: { type: "string", default: path.join(CARPETA_SCRIPT, "respaldos") },
    },
});

const conexion = conectar(opciones.clave);
if (!conexion) {
    console.error("Falta la clave de la cuenta de servicio.\n");
    console.error("  node respaldar.mjs --clave /ruta/a/la-clave.json [--salida carpeta]");
    process.exit(1);
}
const { db, proyecto } = conexion;

const documentos = await leerColecciones(db);

const rutas = Object.keys(documentos).sort();
const respaldo = {
    proyecto,
    exportado: new Date().toISOString(),
    cantidad: rutas.length,
    documentos: Object.fromEntries(rutas.map((ruta) => [ruta, serializar(documentos[ruta])])),
};

mkdirSync(opciones.salida, { recursive: true });
const marca = respaldo.exportado.replace(/\.\d+Z$/, "").replace(/[:T]/g, "-");
const archivo = path.join(opciones.salida, `respaldo-${proyecto}-${marca}.json`);
// Solo el usuario actual puede leerlo: contiene datos personales.
writeFileSync(archivo, JSON.stringify(respaldo, null, 2), { mode: 0o600 });

console.log(`\n${rutas.length} documentos de "${proyecto}" guardados en ${archivo}\n`);
console.table(contarPorColeccion(rutas));

/**
 * Convierte los tipos propios de Firestore en objetos JSON con una marca `__tipo`, para poder
 * reconstruirlos al restaurar (ver restaurar.mjs).
 */
function serializar(valor) {
    if (valor instanceof Timestamp) {
        return {
            __tipo: "fecha",
            valor: valor.toDate().toISOString(),
            segundos: valor.seconds,
            nanosegundos: valor.nanoseconds,
        };
    }
    if (valor instanceof GeoPoint) {
        return { __tipo: "geopunto", latitud: valor.latitude, longitud: valor.longitude };
    }
    if (valor instanceof DocumentReference) {
        return { __tipo: "referencia", ruta: valor.path };
    }
    if (valor instanceof Uint8Array) {
        return { __tipo: "bytes", base64: Buffer.from(valor).toString("base64") };
    }
    if (typeof valor === "number" && !Number.isFinite(valor)) {
        // JSON no admite NaN ni infinitos.
        return { __tipo: "numero", valor: String(valor) };
    }
    if (Array.isArray(valor)) {
        return valor.map(serializar);
    }
    if (valor !== null && typeof valor === "object") {
        return Object.fromEntries(Object.entries(valor).map(([campo, v]) => [campo, serializar(v)]));
    }
    return valor;
}
