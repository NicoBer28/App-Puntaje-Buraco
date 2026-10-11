// Conexión a Firestore con el Admin SDK, compartida por los scripts de mantenimiento. El Admin
// SDK no pasa por las reglas de seguridad.
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { cert, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

/** Documentos que se piden juntos al servidor. */
const TAMANO_LOTE = 300;

/** Documentos cuyas subcolecciones se consultan en paralelo. */
const CONSULTAS_EN_PARALELO = 20;

const RAIZ_REPO = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

/** `true` si los scripts van a hablar con el emulador en lugar del proyecto real. */
export const usaEmulador = Boolean(process.env.FIRESTORE_EMULATOR_HOST);

/**
 * Inicializa el Admin SDK. Con FIRESTORE_EMULATOR_HOST definida se conecta al emulador y no hace
 * falta clave; si no, necesita la clave privada de una cuenta de servicio (consola de Firebase →
 * Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada).
 *
 * @param rutaClave ruta al archivo de la clave, o `undefined` para usar GOOGLE_APPLICATION_CREDENTIALS.
 * @returns la base de datos y el id del proyecto, o `null` si falta la clave.
 */
export function conectar(rutaClave = process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    if (usaEmulador) {
        // El emulador separa los datos por proyecto: se usa el mismo que la app, para que los vea.
        const proyecto = process.env.GCLOUD_PROJECT ?? proyectoDelRepo();
        initializeApp({ projectId: proyecto });
        return { db: getFirestore(), proyecto };
    }
    if (!rutaClave) return null;
    const clave = JSON.parse(readFileSync(rutaClave, "utf8"));
    initializeApp({ credential: cert(clave) });
    return { db: getFirestore(), proyecto: clave.project_id };
}

function proyectoDelRepo() {
    return JSON.parse(readFileSync(path.join(RAIZ_REPO, ".firebaserc"), "utf8")).projects.default;
}

/**
 * Lee colecciones completas y, recursivamente, las subcolecciones de cada documento.
 *
 * @param nombres colecciones de primer nivel a leer; sin indicar, todas.
 * @returns un mapa de la ruta de cada documento ("users/ana/partidas/x1") a sus datos.
 */
export async function leerColecciones(db, nombres) {
    const colecciones = nombres ? nombres.map((nombre) => db.collection(nombre)) : await db.listCollections();
    const documentos = {};
    for (const coleccion of colecciones) await leerColeccion(db, coleccion, documentos);
    return documentos;
}

async function leerColeccion(db, coleccion, documentos) {
    // listDocuments() incluye los documentos "vacíos": los que no existen pero tienen
    // subcolecciones. Una consulta común los saltearía, junto con todo lo que tienen debajo.
    const referencias = await coleccion.listDocuments();
    process.stdout.write(`${coleccion.path}: ${referencias.length}\n`);

    for (const lote of enLotes(referencias, TAMANO_LOTE)) {
        for (const snapshot of await db.getAll(...lote)) {
            if (snapshot.exists) documentos[snapshot.ref.path] = snapshot.data();
        }
    }
    for (const lote of enLotes(referencias, CONSULTAS_EN_PARALELO)) {
        const subcolecciones = await Promise.all(lote.map((referencia) => referencia.listCollections()));
        for (const subcoleccion of subcolecciones.flat()) {
            await leerColeccion(db, subcoleccion, documentos);
        }
    }
}

export function enLotes(lista, tamano) {
    const lotes = [];
    for (let i = 0; i < lista.length; i += tamano) lotes.push(lista.slice(i, i + tamano));
    return lotes;
}

/** Agrupa las rutas por colección, sin los ids: "users/ana/partidas/x1" cuenta en "users/…/partidas". */
export function contarPorColeccion(rutas) {
    const cantidades = {};
    for (const ruta of rutas) {
        const coleccion = ruta.split("/").filter((_, i) => i % 2 === 0).join("/…/");
        cantidades[coleccion] = (cantidades[coleccion] ?? 0) + 1;
    }
    return cantidades;
}
