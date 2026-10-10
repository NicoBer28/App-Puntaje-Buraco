#!/usr/bin/env node
/**
 * Descarga todo Firestore (colecciones y subcolecciones) a un archivo JSON.
 *
 * Solo lee: no modifica nada en la base. Usa el Admin SDK, así que no pasa por las reglas de
 * seguridad y necesita la clave privada de una cuenta de servicio (consola de Firebase →
 * Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada).
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
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";
import { cert, initializeApp } from "firebase-admin/app";
import { DocumentReference, GeoPoint, Timestamp, getFirestore } from "firebase-admin/firestore";

/** Documentos que se piden juntos al servidor. */
const TAMANO_LOTE = 300;

/** Documentos cuyas subcolecciones se consultan en paralelo. */
const CONSULTAS_EN_PARALELO = 20;

const CARPETA_SCRIPT = path.dirname(fileURLToPath(import.meta.url));

const { values: opciones } = parseArgs({
    options: {
        clave: { type: "string" },
        salida: { type: "string", default: path.join(CARPETA_SCRIPT, "respaldos") },
    },
});

const proyecto = conectar(opciones.clave ?? process.env.GOOGLE_APPLICATION_CREDENTIALS);
const db = getFirestore();

const documentos = {};
for (const coleccion of await db.listCollections()) {
    await leerColeccion(coleccion);
}

const rutas = Object.keys(documentos).sort();
const respaldo = {
    proyecto,
    exportado: new Date().toISOString(),
    cantidad: rutas.length,
    documentos: Object.fromEntries(rutas.map((ruta) => [ruta, documentos[ruta]])),
};

mkdirSync(opciones.salida, { recursive: true });
const marca = respaldo.exportado.replace(/\.\d+Z$/, "").replace(/[:T]/g, "-");
const archivo = path.join(opciones.salida, `respaldo-${proyecto}-${marca}.json`);
// Solo el usuario actual puede leerlo: contiene datos personales.
writeFileSync(archivo, JSON.stringify(respaldo, null, 2), { mode: 0o600 });

console.log(`\n${rutas.length} documentos de "${proyecto}" guardados en ${archivo}\n`);
console.table(contarPorColeccion(rutas));

/** Inicializa el Admin SDK y devuelve el id del proyecto. */
function conectar(rutaClave) {
    if (rutaClave) {
        const clave = JSON.parse(readFileSync(rutaClave, "utf8"));
        initializeApp({ credential: cert(clave) });
        return clave.project_id;
    }
    if (process.env.FIRESTORE_EMULATOR_HOST) {
        const id = process.env.GCLOUD_PROJECT ?? "demo-puntaje-buraco";
        initializeApp({ projectId: id });
        return id;
    }
    console.error("Falta la clave de la cuenta de servicio.\n");
    console.error("  node respaldar.mjs --clave /ruta/a/la-clave.json [--salida carpeta]");
    process.exit(1);
}

/** Lee una colección completa y, recursivamente, las subcolecciones de cada documento. */
async function leerColeccion(coleccion) {
    // listDocuments() incluye los documentos "vacíos": los que no existen pero tienen
    // subcolecciones. Una consulta común los saltearía, junto con todo lo que tienen debajo.
    const referencias = await coleccion.listDocuments();
    process.stdout.write(`${coleccion.path}: ${referencias.length}\n`);

    for (const lote of enLotes(referencias, TAMANO_LOTE)) {
        for (const snapshot of await db.getAll(...lote)) {
            if (snapshot.exists) documentos[snapshot.ref.path] = serializar(snapshot.data());
        }
    }
    for (const lote of enLotes(referencias, CONSULTAS_EN_PARALELO)) {
        const subcolecciones = await Promise.all(lote.map((referencia) => referencia.listCollections()));
        for (const subcoleccion of subcolecciones.flat()) {
            await leerColeccion(subcoleccion);
        }
    }
}

/**
 * Convierte los tipos propios de Firestore en objetos JSON con una marca `__tipo`, para poder
 * reconstruirlos al restaurar.
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

function enLotes(lista, tamano) {
    const lotes = [];
    for (let i = 0; i < lista.length; i += tamano) lotes.push(lista.slice(i, i + tamano));
    return lotes;
}

// Agrupa las rutas por colección, sin los ids: "users/ana/partidas/x1" cuenta en "users/…/partidas".
function contarPorColeccion(rutas) {
    const cantidades = {};
    for (const ruta of rutas) {
        const coleccion = ruta.split("/").filter((_, i) => i % 2 === 0).join("/…/");
        cantidades[coleccion] = (cantidades[coleccion] ?? 0) + 1;
    }
    return cantidades;
}
