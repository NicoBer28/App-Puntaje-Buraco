// Reglas de los reclamos: perfiles creados para otra persona y reservados para su mail (fase 5
// de docs/PLAN_AUTENTICACION.md).
import { after, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { altaDePerfil, altaSinLogin, comoPersona, crearEntorno, escribir, persona } from "./comun.mjs";

const entorno = await crearEntorno();
const ana = persona("Ana");
const beto = persona("Beto");
// Dani y Eva no tienen perfil. A Dani se lo crea Ana; Eva no tiene nada que ver.
const dani = persona("Dani");
const eva = persona("Eva");

const reclamo = (creador, perfil, cambios = {}) => ({
    perfil: perfil.idPerfil,
    creadoPor: creador.uid,
    nombreCreador: creador.nombre,
    ...cambios,
});

/** Lo que la app escribe al crear un usuario para otro: el perfil sin login y su reclamo. */
const altaConReclamo = (creador, nuevo, cambios = {}) =>
    altaSinLogin(creador, nuevo, { [`mails/${nuevo.mail}`]: reclamo(creador, nuevo), ...cambios });

/**
 * Las tres escrituras con las que `cuenta` acepta el perfil que le reservaron. `cambios`
 * permite alterar o quitar (con `null`) alguna.
 */
function aceptar(db, cuenta, perfil, cambios = {}) {
    const escrituras = {
        [`perfiles/${perfil.idPerfil}`]: { uid: cuenta.uid },
        [`cuentas/${cuenta.uid}`]: { perfil: perfil.idPerfil },
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
    for (const quien of [ana, beto]) await altaDePerfil(comoPersona(entorno, quien), quien);
});
after(() => entorno.cleanup());

describe("crear un usuario para otro con su mail", () => {
    test("el perfil y el reclamo se crean juntos", async () => {
        await assertSucceeds(escribir(comoPersona(entorno, ana), altaConReclamo(ana, dani)));
    });

    test("a un perfil creado antes, sin mail, se le puede cargar después", async () => {
        const db = comoPersona(entorno, ana);
        await escribir(db, altaSinLogin(ana, dani));
        await assertSucceeds(db.doc("mails/dani@test.com").set(reclamo(ana, dani)));
    });

    test("el mail va en minúsculas", async () => {
        const conMayusculas = { "mails/dani@test.com": null, "mails/Dani@test.com": reclamo(ana, dani) };
        await assertFails(escribir(comoPersona(entorno, ana), altaConReclamo(ana, dani, conMayusculas)));
    });

    test("el reclamo dice quién lo creó, con su nombre real, y nada más", async () => {
        const db = comoPersona(entorno, ana);
        for (const cambios of [{ creadoPor: beto.uid }, { nombreCreador: "Beto" }, { uid: ana.uid }, { nombreCreador: undefined }]) {
            const alterado = JSON.parse(JSON.stringify(reclamo(ana, dani, cambios)));
            await assertFails(escribir(db, altaConReclamo(ana, dani, { "mails/dani@test.com": alterado })));
        }
    });

    test("no se puede usar un mail que ya tiene cuenta o reclamo", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(escribir(db, altaConReclamo(ana, { ...dani, mail: beto.mail })));
        await assertFails(escribir(db, altaConReclamo(ana, { ...dani, mail: ana.mail })));

        await escribir(db, altaConReclamo(ana, dani));
        const otro = { ...eva, mail: dani.mail };
        await assertFails(escribir(comoPersona(entorno, beto), altaConReclamo(beto, otro)));
    });

    test("solo se reserva un perfil sin dueño creado por uno mismo", async () => {
        await escribir(comoPersona(entorno, ana), altaSinLogin(ana, dani));
        await entorno.withSecurityRulesDisabled((contexto) =>
            contexto.firestore().doc("perfiles/perfil-migrado").set({ nombre: "Migrado" }),
        );
        const db = comoPersona(entorno, beto);
        for (const idPerfil of [dani.idPerfil, ana.idPerfil, beto.idPerfil, "perfil-migrado", "perfil-nadie"]) {
            await assertFails(db.doc("mails/eva@test.com").set(reclamo(beto, { idPerfil })));
        }
    });

    test("hace falta tener perfil propio y mail verificado", async () => {
        await assertFails(escribir(comoPersona(entorno, eva), altaConReclamo(eva, dani)));
        const sinVerificar = persona("Ana", { verificada: false });
        await assertFails(escribir(comoPersona(entorno, sinVerificar), altaConReclamo(ana, dani)));
    });
});

describe("con un perfil reservado para un mail", () => {
    beforeEach(() => escribir(comoPersona(entorno, ana), altaConReclamo(ana, dani)));

    test("el reclamo lo leen quien lo creó y el dueño del mail, y nadie más", async () => {
        await assertSucceeds(comoPersona(entorno, ana).doc("mails/dani@test.com").get());
        await assertSucceeds(comoPersona(entorno, dani).doc("mails/dani@test.com").get());
        await assertFails(comoPersona(entorno, beto).doc("mails/dani@test.com").get());
        await assertFails(comoPersona(entorno, persona("Dani", { verificada: false })).doc("mails/dani@test.com").get());
    });

    test("cualquier usuario verificado ve que un mail está libre", async () => {
        await assertSucceeds(comoPersona(entorno, beto).doc("mails/libre@test.com").get());
        await assertFails(entorno.unauthenticatedContext().firestore().doc("mails/libre@test.com").get());
    });

    test("cada uno lista solo los perfiles y los reclamos que creó", async () => {
        const db = comoPersona(entorno, ana);
        await assertSucceeds(db.collection("perfiles").where("creadoPor", "==", ana.uid).get());
        await assertSucceeds(db.collection("mails").where("creadoPor", "==", ana.uid).get());
        for (const coleccion of ["perfiles", "mails"]) {
            await assertFails(db.collection(coleccion).where("creadoPor", "==", beto.uid).get());
            await assertFails(db.collection(coleccion).get());
        }
    });

    test("quien lo creó corrige el mail: borra el reclamo y crea otro", async () => {
        const corregido = { "mails/dani@test.com": null, "mails/daniel@test.com": reclamo(ana, dani) };
        await assertSucceeds(escribir(comoPersona(entorno, ana), corregido));
    });

    test("un tercero no lo borra ni lo modifica, y quien lo creó no lo modifica", async () => {
        await assertFails(comoPersona(entorno, beto).doc("mails/dani@test.com").delete());
        for (const quien of [ana, beto]) {
            const db = comoPersona(entorno, quien);
            await assertFails(db.doc("mails/dani@test.com").set(reclamo(quien, beto)));
            await assertFails(db.doc("mails/dani@test.com").update({ nombreCreador: "Otro" }));
        }
    });

    test("el dueño del mail acepta: el perfil pasa a ser suyo y el reclamo, el registro de su cuenta", async () => {
        const db = comoPersona(entorno, dani);
        await assertSucceeds(aceptar(db, dani, dani));
        await assertSucceeds(db.collection("perfiles/perfil-dani/amigos").get());
        // Quien lo creó ya no lo tiene a cargo ni ve el mail.
        await assertFails(comoPersona(entorno, ana).doc("mails/dani@test.com").get());
        await assertFails(comoPersona(entorno, ana).collection("perfiles/perfil-dani/amigos").get());
    });

    test("con otro mail no se puede aceptar", async () => {
        await assertFails(aceptar(comoPersona(entorno, eva), eva, dani));
        // Tampoco apuntando al reclamo de otro.
        const conMailAjeno = { "mails/eva@test.com": null, "mails/dani@test.com": { perfil: dani.idPerfil, uid: eva.uid } };
        await assertFails(aceptar(comoPersona(entorno, eva), eva, dani, conMailAjeno));
    });

    test("sin mail verificado no se puede aceptar", async () => {
        const sinVerificar = persona("Dani", { verificada: false });
        await assertFails(aceptar(comoPersona(entorno, sinVerificar), sinVerificar, dani));
    });

    test("aceptar es quedarse con ese perfil, completo: cuenta, mail y nada más", async () => {
        const db = comoPersona(entorno, dani);
        await assertFails(aceptar(db, dani, dani, { "cuentas/uid-dani": null }));
        await assertFails(aceptar(db, dani, dani, { "mails/dani@test.com": null }));
        await assertFails(aceptar(db, dani, dani, { "perfiles/perfil-dani": { uid: dani.uid, nombre: "Otro" } }));
        await assertFails(aceptar(db, dani, dani, { "mails/dani@test.com": { perfil: ana.idPerfil, uid: dani.uid } }));
        const conCreador = { perfil: dani.idPerfil, uid: dani.uid, creadoPor: ana.uid };
        await assertFails(aceptar(db, dani, dani, { "mails/dani@test.com": conCreador }));
        // El reclamo no sirve para quedarse con otro perfil.
        await assertFails(aceptar(db, dani, { idPerfil: beto.idPerfil }));
    });

    test("mientras no responda, no puede crearse un perfil nuevo", async () => {
        const nuevo = { ...dani, idPerfil: "perfil-dani-2", nombre: "Dani2" };
        await assertFails(altaDePerfil(comoPersona(entorno, dani), nuevo));
    });

    test("el dueño del mail lo rechaza y después crea su propio perfil", async () => {
        const db = comoPersona(entorno, dani);
        await assertSucceeds(db.doc("mails/dani@test.com").delete());
        const nuevo = { ...dani, idPerfil: "perfil-dani-2", nombre: "Dani2" };
        await assertSucceeds(altaDePerfil(db, nuevo));
    });

    test("un perfil ya aceptado no se vuelve a reservar ni a tomar", async () => {
        await aceptar(comoPersona(entorno, dani), dani, dani);
        await assertFails(comoPersona(entorno, ana).doc("mails/eva@test.com").set(reclamo(ana, dani)));
        await assertFails(aceptar(comoPersona(entorno, eva), eva, dani));
        // El registro de la cuenta no se borra, ni siquiera por quien había creado el reclamo.
        await assertFails(comoPersona(entorno, ana).doc("mails/dani@test.com").delete());
        await assertFails(comoPersona(entorno, dani).doc("mails/dani@test.com").delete());
    });
});
