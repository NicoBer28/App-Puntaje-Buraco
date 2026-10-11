// Reglas para vincular un perfil anterior al corte (fase 4 de docs/PLAN_AUTENTICACION.md).
import { after, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { hashDeCredencial, pruebaDeClave } from "../migracion/credenciales.mjs";
import { altaDePerfil, comoPersona, crearEntorno, persona } from "./comun.mjs";

const entorno = await crearEntorno();
const ana = persona("Ana");
const beto = persona("Beto");
const CLAVE = "1234";

/** Deja los datos como los escribe el script de migración, que no pasa por las reglas. */
function migrar(documentos) {
    return entorno.withSecurityRulesDisabled(async (contexto) => {
        const db = contexto.firestore();
        for (const [ruta, datos] of Object.entries(documentos)) await db.doc(ruta).set(datos);
    });
}

/** Un perfil migrado: sin dueño, con su nombre reservado y el hash de la contraseña que tenía. */
function perfilMigrado(quien, clave = CLAVE) {
    return {
        [`perfiles/${quien.idPerfil}`]: { nombre: quien.nombre },
        [`nombres/${quien.nombre.toLowerCase()}`]: { perfil: quien.idPerfil },
        [`credencialesViejas/${quien.idPerfil}`]: { hash: hashDeCredencial(quien.idPerfil, clave), usuario: quien.nombre.toLowerCase() },
    };
}

/**
 * Las tres escrituras que la app hace juntas al vincular: `cuenta` se queda con `perfil`
 * demostrando que sabe `clave`. `cambios` permite alterar o quitar (con `null`) alguna.
 */
function vincular(db, cuenta, perfil, clave, cambios = {}) {
    const escrituras = {
        [`perfiles/${perfil.idPerfil}`]: { uid: cuenta.uid },
        [`cuentas/${cuenta.uid}`]: { perfil: perfil.idPerfil, pruebaClaveVieja: pruebaDeClave(perfil.idPerfil, clave) },
        [`mails/${cuenta.mail}`]: { perfil: perfil.idPerfil, uid: cuenta.uid },
        ...cambios,
    };
    const lote = db.batch();
    for (const [ruta, datos] of Object.entries(escrituras)) {
        if (datos === null) continue;
        if (ruta.startsWith("perfiles/")) lote.update(db.doc(ruta), datos);
        else lote.set(db.doc(ruta), datos);
    }
    return lote.commit();
}

beforeEach(async () => {
    await entorno.clearFirestore();
    await migrar(perfilMigrado(ana));
});
after(() => entorno.cleanup());

describe("vincular un perfil anterior al corte", () => {
    test("con la contraseña que tenía, la cuenta se queda con el perfil", async () => {
        const db = comoPersona(entorno, ana);
        await assertSucceeds(vincular(db, ana, ana, CLAVE));
        await assertSucceeds(db.doc("cuentas/uid-ana").get());
        // Ya es su dueña: puede leer su lista de amigos.
        await assertSucceeds(db.collection("perfiles/perfil-ana/amigos").get());
    });

    test("lo puede vincular cualquier cuenta que sepa la contraseña, con otro mail", async () => {
        await assertSucceeds(vincular(comoPersona(entorno, beto), beto, ana, CLAVE));
    });

    test("con otra contraseña no se puede", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(vincular(db, ana, ana, "4321"));
        await assertFails(vincular(db, ana, ana, ""));
    });

    test("sin la prueba de la contraseña no se puede", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(vincular(db, ana, ana, CLAVE, { "cuentas/uid-ana": { perfil: ana.idPerfil } }));
    });

    test("no alcanza con conocer el hash guardado", async () => {
        const conElHash = { perfil: ana.idPerfil, pruebaClaveVieja: hashDeCredencial(ana.idPerfil, CLAVE) };
        await assertFails(vincular(comoPersona(entorno, ana), ana, ana, CLAVE, { "cuentas/uid-ana": conElHash }));
    });

    test("la prueba de otro perfil con la misma contraseña no sirve", async () => {
        await migrar(perfilMigrado(beto));
        const deBeto = { perfil: ana.idPerfil, pruebaClaveVieja: pruebaDeClave(beto.idPerfil, CLAVE) };
        await assertFails(vincular(comoPersona(entorno, ana), ana, ana, CLAVE, { "cuentas/uid-ana": deBeto }));
    });

    test("sin mail verificado no se puede", async () => {
        const sinVerificar = persona("Ana", { verificada: false });
        await assertFails(vincular(comoPersona(entorno, sinVerificar), sinVerificar, ana, CLAVE));
    });

    for (const faltante of ["cuentas/uid-ana", "mails/ana@test.com"]) {
        test(`si falta ${faltante} se rechaza todo`, async () => {
            await assertFails(vincular(comoPersona(entorno, ana), ana, ana, CLAVE, { [faltante]: null }));
        });
    }

    test("el perfil no puede quedar a nombre de otra cuenta ni cambiar en nada más", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(vincular(db, ana, ana, CLAVE, { "perfiles/perfil-ana": { uid: beto.uid } }));
        await assertFails(vincular(db, ana, ana, CLAVE, { "perfiles/perfil-ana": { uid: ana.uid, nombre: "Otra" } }));
        await assertFails(vincular(db, ana, ana, CLAVE, { "perfiles/perfil-ana": { uid: ana.uid, creadoPor: ana.uid } }));
    });

    test("un perfil ya vinculado no se puede volver a vincular", async () => {
        await vincular(comoPersona(entorno, ana), ana, ana, CLAVE);
        await assertFails(vincular(comoPersona(entorno, beto), beto, ana, CLAVE));
    });

    test("una cuenta que ya tiene perfil no puede vincular otro", async () => {
        const db = comoPersona(entorno, beto);
        await altaDePerfil(db, beto);
        await assertFails(vincular(db, beto, ana, CLAVE));
        await assertFails(vincular(db, beto, ana, CLAVE, { "cuentas/uid-beto": null, "mails/beto@test.com": null }));
    });

    test("un perfil sin credencial vieja no se puede vincular", async () => {
        await migrar({
            "perfiles/perfil-zoe": { nombre: "Zoe", creadoPor: beto.uid },
            "perfiles/perfil-leo": { nombre: "Leo" },
        });
        const db = comoPersona(entorno, ana);
        await assertFails(vincular(db, ana, persona("Zoe"), CLAVE));
        await assertFails(vincular(db, ana, persona("Leo"), CLAVE));
    });

    test("nadie lee ni escribe las credenciales viejas", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(db.doc("credencialesViejas/perfil-ana").get());
        await assertFails(db.doc("credencialesViejas/perfil-ana").set({ hash: hashDeCredencial(ana.idPerfil, "0000") }));
        await assertFails(db.doc("credencialesViejas/perfil-ana").delete());
        await vincular(db, ana, ana, CLAVE);
        await assertFails(db.doc("credencialesViejas/perfil-ana").get());
    });
});

describe("el esquema anterior", () => {
    test("queda bloqueado para todos", async () => {
        await migrar({ "users/ana": { Nombre: "Ana", Password: CLAVE }, "doubles/ana|beto": { "Partidas Jugadas": 1 } });
        for (const db of [comoPersona(entorno, ana), entorno.unauthenticatedContext().firestore()]) {
            await assertFails(db.doc("users/ana").get());
            await assertFails(db.doc("users/ana").update({ Password: "0000" }));
            await assertFails(db.doc("users/nueva").set({ Nombre: "Nueva" }));
            await assertFails(db.doc("doubles/ana|beto").get());
            await assertFails(db.doc("users_v2/x").get());
        }
    });
});
