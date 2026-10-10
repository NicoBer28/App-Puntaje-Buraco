// Reglas para renombrar y borrar perfiles (fase 7 de docs/PLAN_AUTENTICACION.md).
import { after, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import {
    altaDePerfil,
    altaSinLogin,
    amistad,
    comoPersona,
    crearEntorno,
    escribir,
    persona,
    rutaAmistad,
} from "./comun.mjs";

const entorno = await crearEntorno();
const ana = persona("Ana");
const beto = persona("Beto");
// Dani no usa la app: su perfil lo creó Ana, reservado para su mail.
const dani = persona("Dani");

const reclamo = (creador, perfil) => ({ perfil: perfil.idPerfil, creadoPor: creador.uid, nombreCreador: creador.nombre });

/** Lo que la app escribe junto al renombrar: el perfil, la reserva nueva y la anterior, que se libera. */
function renombrar(db, perfil, nombreNuevo, cambios = {}) {
    const escrituras = {
        [`perfiles/${perfil.idPerfil}`]: { nombre: nombreNuevo },
        [`nombres/${perfil.nombre.toLowerCase()}`]: null,
        [`nombres/${nombreNuevo.toLowerCase()}`]: { perfil: perfil.idPerfil },
        ...cambios,
    };
    const lote = db.batch();
    for (const [ruta, datos] of Object.entries(escrituras)) {
        if (datos === undefined) continue;
        if (datos === null) lote.delete(db.doc(ruta));
        else if (ruta.startsWith("perfiles/")) lote.update(db.doc(ruta), datos);
        else lote.set(db.doc(ruta), datos);
    }
    return lote.commit();
}

/** Borra juntos los documentos indicados, salvo los que `sin` deja afuera. */
function borrar(db, rutas, sin = []) {
    const lote = db.batch();
    for (const ruta of rutas) if (!sin.includes(ruta)) lote.delete(db.doc(ruta));
    return lote.commit();
}

const sinAmistad = (uno, otro) => ({ [rutaAmistad(uno, otro)]: null, [rutaAmistad(otro, uno)]: null });

beforeEach(async () => {
    await entorno.clearFirestore();
    for (const quien of [ana, beto]) await altaDePerfil(comoPersona(entorno, quien), quien);
    await escribir(comoPersona(entorno, ana), amistad(ana, beto));
    await escribir(comoPersona(entorno, ana), altaSinLogin(ana, dani, { "mails/dani@test.com": reclamo(ana, dani) }));
});
after(() => entorno.cleanup());

describe("renombrar un perfil", () => {
    test("su dueño le cambia el nombre: reserva el nuevo y libera el anterior", async () => {
        const db = comoPersona(entorno, ana);
        await assertSucceeds(renombrar(db, ana, "Anita"));
        // El nombre anterior quedó libre para otra persona.
        const nueva = { ...persona("Eva"), nombre: "Ana" };
        await assertSucceeds(altaDePerfil(comoPersona(entorno, nueva), nueva));
    });

    test("quien creó un perfil sin dueño también lo renombra", async () => {
        await assertSucceeds(renombrar(comoPersona(entorno, ana), dani, "Daniel"));
    });

    test("si solo cambian mayúsculas la reserva es la misma", async () => {
        await assertSucceeds(comoPersona(entorno, ana).doc("perfiles/perfil-ana").update({ nombre: "ANA" }));
    });

    test("nadie renombra un perfil ajeno, ni quien lo creó una vez que tiene dueño", async () => {
        await assertFails(renombrar(comoPersona(entorno, beto), ana, "Anita"));
        await assertFails(renombrar(comoPersona(entorno, beto), dani, "Daniel"));

        const db = comoPersona(entorno, dani);
        const lote = db.batch();
        lote.update(db.doc("perfiles/perfil-dani"), { uid: dani.uid });
        lote.set(db.doc("cuentas/uid-dani"), { perfil: dani.idPerfil });
        lote.set(db.doc("mails/dani@test.com"), { perfil: dani.idPerfil, uid: dani.uid });
        await lote.commit();
        await assertFails(renombrar(comoPersona(entorno, ana), dani, "Daniel"));
        await assertSucceeds(renombrar(db, dani, "Daniel"));
    });

    test("el nombre nuevo tiene que ser válido, estar libre y quedar reservado", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(renombrar(db, ana, "An"));
        await assertFails(renombrar(db, ana, "Ana María"));
        await assertFails(renombrar(db, ana, "Beto"));
        await assertFails(renombrar(db, ana, "Anita", { "nombres/anita": undefined }));
        await assertFails(renombrar(db, ana, "Anita", { "nombres/anita": { perfil: beto.idPerfil } }));
    });

    test("no se puede quedar también con el nombre anterior", async () => {
        await assertFails(renombrar(comoPersona(entorno, ana), ana, "Anita", { "nombres/ana": undefined }));
    });

    test("renombrar no cambia nada más del perfil", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(renombrar(db, ana, "Anita", { "perfiles/perfil-ana": { nombre: "Anita", uid: beto.uid } }));
        await assertFails(renombrar(db, dani, "Daniel", { "perfiles/perfil-dani": { nombre: "Daniel", uid: ana.uid } }));
    });

    test("un nombre reservado no se libera si el perfil se sigue llamando así", async () => {
        await assertFails(comoPersona(entorno, ana).doc("nombres/ana").delete());
        await assertFails(comoPersona(entorno, beto).doc("nombres/ana").delete());
    });

    test("después corrige su nombre en la lista de cada amigo", async () => {
        const db = comoPersona(entorno, ana);
        await renombrar(db, ana, "Anita");
        await assertSucceeds(db.doc(rutaAmistad(beto, ana)).update({ nombre: "Anita" }));
        await assertSucceeds(db.doc(rutaAmistad(dani, ana)).update({ nombre: "Anita" }));
    });

    test("en la lista de un amigo solo se corrige el nombre propio, y por el verdadero", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(db.doc(rutaAmistad(beto, ana)).update({ nombre: "Anita" }));
        await assertFails(db.doc(rutaAmistad(ana, beto)).update({ nombre: "Otro" }));
        await renombrar(db, ana, "Anita");
        await assertFails(comoPersona(entorno, beto).doc(rutaAmistad(beto, ana)).update({ nombre: "Otra" }));
        await assertFails(db.doc(rutaAmistad(beto, ana)).update({ nombre: "Anita", desde: new Date(2020, 0, 1) }));
    });
});

describe("borrar un perfil sin dueño", () => {
    const deDani = ["perfiles/perfil-dani", "nombres/dani", "mails/dani@test.com"];

    test("quien lo creó borra sus amistades y después el perfil, su nombre y su reserva", async () => {
        const db = comoPersona(entorno, ana);
        await assertSucceeds(escribir(db, sinAmistad(ana, dani)));
        await assertSucceeds(borrar(db, deDani));
        // El nombre y el mail quedaron libres.
        await assertSucceeds(escribir(db, altaSinLogin(ana, dani, { "mails/dani@test.com": reclamo(ana, dani) })));
    });

    test("también le quita las amistades con otros", async () => {
        await escribir(comoPersona(entorno, beto), amistad(beto, dani));
        await assertSucceeds(escribir(comoPersona(entorno, ana), sinAmistad(beto, dani)));
    });

    test("nadie más lo borra", async () => {
        await assertFails(borrar(comoPersona(entorno, beto), deDani));
        await assertFails(borrar(comoPersona(entorno, dani), deDani));
    });

    test("el perfil y su nombre se borran juntos", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(borrar(db, deDani, ["nombres/dani"]));
        await assertFails(borrar(db, deDani, ["perfiles/perfil-dani"]));
    });
});

describe("borrar la cuenta propia", () => {
    const deAna = ["perfiles/perfil-ana", "nombres/ana", "cuentas/uid-ana", "mails/ana@test.com"];

    test("su dueño borra el perfil con su nombre, su cuenta y su mail", async () => {
        const db = comoPersona(entorno, ana);
        await assertSucceeds(escribir(db, { ...sinAmistad(ana, beto), ...sinAmistad(ana, dani) }));
        await assertSucceeds(borrar(db, deAna));
        // Puede volver a empezar con el mismo mail y el mismo nombre.
        await assertSucceeds(altaDePerfil(db, ana));
    });

    test("no puede quedar nada a medias", async () => {
        const db = comoPersona(entorno, ana);
        for (const faltante of deAna) await assertFails(borrar(db, deAna, [faltante]));
    });

    test("nadie borra la cuenta de otro", async () => {
        await assertFails(borrar(comoPersona(entorno, beto), deAna));
        await assertFails(borrar(comoPersona(entorno, beto), ["cuentas/uid-ana"]));
        await assertFails(borrar(comoPersona(entorno, beto), ["mails/ana@test.com"]));
    });

    test("sin mail verificado no se puede", async () => {
        await assertFails(borrar(comoPersona(entorno, persona("Ana", { verificada: false })), deAna));
    });

    test("quien creó un perfil que ya tiene dueño no lo puede borrar", async () => {
        const db = comoPersona(entorno, dani);
        const lote = db.batch();
        lote.update(db.doc("perfiles/perfil-dani"), { uid: dani.uid });
        lote.set(db.doc("cuentas/uid-dani"), { perfil: dani.idPerfil });
        lote.set(db.doc("mails/dani@test.com"), { perfil: dani.idPerfil, uid: dani.uid });
        await lote.commit();

        const deDani = ["perfiles/perfil-dani", "nombres/dani", "cuentas/uid-dani", "mails/dani@test.com"];
        await assertFails(borrar(comoPersona(entorno, ana), deDani));
        await assertFails(borrar(comoPersona(entorno, ana), ["perfiles/perfil-dani", "nombres/dani"]));
        await assertSucceeds(borrar(db, deDani));
    });
});
