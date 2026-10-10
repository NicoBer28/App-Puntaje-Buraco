// Reglas de perfiles/, nombres/, cuentas/ y mails/ (fase 1 de docs/PLAN_AUTENTICACION.md).
import { after, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { altaDePerfil, comoPersona, crearEntorno, persona } from "./comun.mjs";

const entorno = await crearEntorno();
const ana = persona("Ana");
const beto = persona("Beto");

beforeEach(() => entorno.clearFirestore());
after(() => entorno.cleanup());

describe("crear el perfil propio", () => {
    test("una cuenta verificada crea perfil, nombre, cuenta y mail juntos", async () => {
        await assertSucceeds(altaDePerfil(comoPersona(entorno, ana), ana));
    });

    test("sin mail verificado no se puede", async () => {
        const sinVerificar = persona("Ana", { verificada: false });
        await assertFails(altaDePerfil(comoPersona(entorno, sinVerificar), sinVerificar));
    });

    test("sin sesión no se puede", async () => {
        await assertFails(altaDePerfil(entorno.unauthenticatedContext().firestore(), ana));
    });

    for (const faltante of ["perfiles/perfil-ana", "nombres/ana", "cuentas/uid-ana", "mails/ana@test.com"]) {
        test(`si falta ${faltante} se rechaza todo`, async () => {
            await assertFails(altaDePerfil(comoPersona(entorno, ana), ana, { [faltante]: null }));
        });
    }

    test("el perfil no puede quedar a nombre de otra cuenta", async () => {
        await assertFails(
            altaDePerfil(comoPersona(entorno, ana), ana, { "perfiles/perfil-ana": { nombre: "Ana", uid: beto.uid } }),
        );
    });

    test("el perfil no admite campos de más", async () => {
        const conExtra = { nombre: "Ana", uid: ana.uid, creadoPor: beto.uid };
        await assertFails(altaDePerfil(comoPersona(entorno, ana), ana, { "perfiles/perfil-ana": conExtra }));
    });

    for (const nombre of ["An", "Anastasia", "Ana!", "Ana B", "ñandu"]) {
        test(`el nombre "${nombre}" no es válido`, async () => {
            const invalida = { ...ana, nombre };
            await assertFails(altaDePerfil(comoPersona(entorno, invalida), invalida));
        });
    }

    test("el nombre se reserva en minúsculas", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(altaDePerfil(db, ana, { "nombres/ana": null, "nombres/Ana": { perfil: ana.idPerfil } }));
    });

    test("no se puede reservar un nombre distinto del que tiene el perfil", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(altaDePerfil(db, ana, { "nombres/ana": null, "nombres/reina": { perfil: ana.idPerfil } }));
    });

    test("un nombre ya reservado no se puede pisar, sin importar mayúsculas", async () => {
        await altaDePerfil(comoPersona(entorno, ana), ana);
        const otraAna = { ...beto, nombre: "ANA" };
        await assertFails(altaDePerfil(comoPersona(entorno, otraAna), otraAna));
    });

    test("una cuenta no puede tener dos perfiles", async () => {
        const db = comoPersona(entorno, ana);
        await altaDePerfil(db, ana);
        const segundo = { ...ana, idPerfil: "perfil-ana-2", nombre: "Anita" };
        await assertFails(altaDePerfil(db, segundo));
        // Tampoco dejando la cuenta y el mail como estaban.
        await assertFails(
            altaDePerfil(db, segundo, { "cuentas/uid-ana": null, "mails/ana@test.com": null }),
        );
    });

    test("no se puede usar el mail de otra persona", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(
            altaDePerfil(db, ana, {
                "mails/ana@test.com": null,
                "mails/beto@test.com": { perfil: ana.idPerfil, uid: ana.uid },
            }),
        );
    });

    test("no se puede vincular la cuenta o el mail a un perfil ajeno", async () => {
        await altaDePerfil(comoPersona(entorno, ana), ana);
        const db = comoPersona(entorno, beto);
        await assertFails(db.doc("cuentas/uid-beto").set({ perfil: ana.idPerfil }));
        await assertFails(db.doc("mails/beto@test.com").set({ perfil: ana.idPerfil, uid: beto.uid }));
        await assertFails(db.doc("nombres/beto").set({ perfil: ana.idPerfil }));
    });
});

describe("con el perfil ya creado", () => {
    beforeEach(() => altaDePerfil(comoPersona(entorno, ana), ana));

    test("otro usuario verificado puede leer el perfil y buscar el nombre", async () => {
        const db = comoPersona(entorno, beto);
        await assertSucceeds(db.doc("perfiles/perfil-ana").get());
        await assertSucceeds(db.doc("nombres/ana").get());
        await assertSucceeds(db.doc("nombres/libre").get());
    });

    test("sin verificar o sin sesión no se lee nada de otros", async () => {
        for (const db of [
            comoPersona(entorno, persona("Beto", { verificada: false })),
            entorno.unauthenticatedContext().firestore(),
        ]) {
            await assertFails(db.doc("perfiles/perfil-ana").get());
            await assertFails(db.doc("nombres/ana").get());
        }
    });

    test("la cuenta y el mail solo los lee su dueño", async () => {
        const propia = comoPersona(entorno, ana);
        await assertSucceeds(propia.doc("cuentas/uid-ana").get());
        await assertSucceeds(propia.doc("mails/ana@test.com").get());

        const ajena = comoPersona(entorno, beto);
        await assertFails(ajena.doc("cuentas/uid-ana").get());
        await assertFails(ajena.doc("mails/ana@test.com").get());
    });

    test("una cuenta sin verificar puede ver si ya tiene perfil", async () => {
        const sinVerificar = persona("Beto", { verificada: false });
        await assertSucceeds(comoPersona(entorno, sinVerificar).doc("cuentas/uid-beto").get());
    });

    test("no se pueden listar perfiles, nombres, cuentas ni mails", async () => {
        const db = comoPersona(entorno, ana);
        for (const coleccion of ["perfiles", "nombres", "cuentas", "mails"]) {
            await assertFails(db.collection(coleccion).get());
        }
    });

    test("nadie modifica ni borra lo ya creado, ni siquiera su dueño", async () => {
        for (const db of [comoPersona(entorno, ana), comoPersona(entorno, beto)]) {
            await assertFails(db.doc("perfiles/perfil-ana").update({ nombre: "Otra" }));
            await assertFails(db.doc("perfiles/perfil-ana").update({ uid: beto.uid }));
            await assertFails(db.doc("nombres/ana").set({ perfil: "perfil-beto" }));
            await assertFails(db.doc("cuentas/uid-ana").set({ perfil: "perfil-beto" }));
            await assertFails(db.doc("mails/ana@test.com").update({ uid: beto.uid }));
            for (const ruta of ["perfiles/perfil-ana", "nombres/ana", "cuentas/uid-ana", "mails/ana@test.com"]) {
                await assertFails(db.doc(ruta).delete());
            }
        }
    });
});

describe("colecciones que no existen en el esquema", () => {
    test("quedan bloqueadas", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(db.doc("credencialesViejas/perfil-ana").get());
        await assertFails(db.doc("cualquiera/doc").set({ a: 1 }));
    });
});
