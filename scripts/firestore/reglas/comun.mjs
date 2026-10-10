// Utilidades compartidas por los tests de las reglas de seguridad.
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { initializeTestEnvironment } from "@firebase/rules-unit-testing";

const RAIZ_REPO = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");

/** Entorno de pruebas contra el emulador de Firestore, con las reglas del repositorio. */
export function crearEntorno() {
    return initializeTestEnvironment({
        // emulators:exec define el proyecto y la dirección del emulador.
        projectId: process.env.GCLOUD_PROJECT ?? "demo-puntaje-buraco",
        firestore: { rules: readFileSync(path.join(RAIZ_REPO, "firestore.rules"), "utf8") },
    });
}

/** Persona de prueba: su cuenta y el perfil que le correspondería. */
export function persona(nombre, { verificada = true } = {}) {
    const clave = nombre.toLowerCase();
    return { uid: `uid-${clave}`, mail: `${clave}@test.com`, idPerfil: `perfil-${clave}`, nombre, verificada };
}

/** Firestore visto por esa persona, con su mail en el token como lo pone Firebase Auth. */
export function comoPersona(entorno, { uid, mail, verificada }) {
    return entorno.authenticatedContext(uid, { email: mail, email_verified: verificada }).firestore();
}

/**
 * Los cuatro documentos que la app escribe juntos al elegir nombre de usuario. `cambios` permite
 * alterar o quitar (con `null`) alguno para probar que las reglas lo rechazan.
 */
export function altaDePerfil(db, quien, cambios = {}) {
    const documentos = {
        [`perfiles/${quien.idPerfil}`]: { nombre: quien.nombre, uid: quien.uid },
        [`nombres/${quien.nombre.toLowerCase()}`]: { perfil: quien.idPerfil },
        [`cuentas/${quien.uid}`]: { perfil: quien.idPerfil },
        [`mails/${quien.mail}`]: { perfil: quien.idPerfil, uid: quien.uid },
        ...cambios,
    };
    const lote = db.batch();
    for (const [ruta, datos] of Object.entries(documentos)) {
        if (datos !== null) lote.set(db.doc(ruta), datos);
    }
    return lote.commit();
}
