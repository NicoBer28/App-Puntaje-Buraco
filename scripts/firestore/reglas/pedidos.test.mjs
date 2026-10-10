// Reglas de los pedidos de un perfil creado por otro (fase 6 de docs/PLAN_AUTENTICACION.md).
import { after, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { serverTimestamp } from "firebase/firestore";
import { altaDePerfil, altaSinLogin, comoPersona, crearEntorno, escribir, persona } from "./comun.mjs";

const entorno = await crearEntorno();
const ana = persona("Ana");
const beto = persona("Beto");
// A Dani le creó el perfil Ana, reservado para dani@test.com; pero Dani se registra con otro mail.
const dani = { ...persona("Dani"), mail: "daniel@test.com" };
const eva = persona("Eva");

/** El pedido como lo escribe la app. `cambios` altera campos; `undefined` los quita. */
function pedido(quien, perfil, creador, cambios = {}) {
    const datos = {
        perfil: perfil.idPerfil,
        nombre: perfil.nombre,
        mail: quien.mail,
        creador: creador.uid,
        fecha: serverTimestamp(),
        ...cambios,
    };
    return Object.fromEntries(Object.entries(datos).filter(([, valor]) => valor !== undefined));
}

const reclamo = (creador, perfil) => ({ perfil: perfil.idPerfil, creadoPor: creador.uid, nombreCreador: creador.nombre });

beforeEach(async () => {
    await entorno.clearFirestore();
    for (const quien of [ana, beto]) await altaDePerfil(comoPersona(entorno, quien), quien);
    await escribir(comoPersona(entorno, ana), altaSinLogin(ana, dani, { "mails/dani@test.com": reclamo(ana, dani) }));
});
after(() => entorno.cleanup());

describe("pedir un perfil creado por otro", () => {
    test("lo pide una cuenta verificada sin perfil, con su propio mail", async () => {
        await assertSucceeds(comoPersona(entorno, dani).doc("pedidosReclamo/uid-dani").set(pedido(dani, dani, ana)));
    });

    test("el pedido es el de la propia cuenta y dice su mail verdadero", async () => {
        const db = comoPersona(entorno, dani);
        await assertFails(db.doc("pedidosReclamo/uid-eva").set(pedido(dani, dani, ana)));
        await assertFails(db.doc("pedidosReclamo/uid-dani").set(pedido(dani, dani, ana, { mail: "otro@test.com" })));
    });

    test("tiene que nombrar bien al perfil y a quien lo creó, y nada más", async () => {
        const db = comoPersona(entorno, dani);
        for (const cambios of [
            { creador: beto.uid },
            { nombre: "Otro" },
            { fecha: new Date(2020, 0, 1) },
            { fecha: undefined },
            { extra: true },
        ]) {
            await assertFails(db.doc("pedidosReclamo/uid-dani").set(pedido(dani, dani, ana, cambios)));
        }
    });

    test("solo se pide un perfil sin dueño que alguien haya creado para otro", async () => {
        await entorno.withSecurityRulesDisabled((contexto) =>
            contexto.firestore().doc("perfiles/perfil-migrado").set({ nombre: "Migrado" }),
        );
        const db = comoPersona(entorno, dani);
        // El de Beto ya tiene dueño; el migrado se recupera con su contraseña vieja; el otro no existe.
        await assertFails(db.doc("pedidosReclamo/uid-dani").set(pedido(dani, beto, beto)));
        const migrado = { idPerfil: "perfil-migrado", nombre: "Migrado" };
        await assertFails(db.doc("pedidosReclamo/uid-dani").set(pedido(dani, migrado, ana)));
        await assertFails(db.doc("pedidosReclamo/uid-dani").set(pedido(dani, { idPerfil: "perfil-nadie", nombre: "Nadie" }, ana)));
    });

    test("quien ya tiene perfil, o no verificó su mail, no puede pedir otro", async () => {
        await assertFails(comoPersona(entorno, beto).doc("pedidosReclamo/uid-beto").set(pedido(beto, dani, ana)));
        const sinVerificar = { ...dani, verificada: false };
        await assertFails(comoPersona(entorno, sinVerificar).doc("pedidosReclamo/uid-dani").set(pedido(dani, dani, ana)));
    });
});

describe("con un pedido hecho", () => {
    beforeEach(() => comoPersona(entorno, dani).doc("pedidosReclamo/uid-dani").set(pedido(dani, dani, ana)));

    test("lo leen quien lo hizo y quien creó el perfil, y nadie más", async () => {
        await assertSucceeds(comoPersona(entorno, dani).doc("pedidosReclamo/uid-dani").get());
        await assertSucceeds(comoPersona(entorno, ana).doc("pedidosReclamo/uid-dani").get());
        await assertFails(comoPersona(entorno, beto).doc("pedidosReclamo/uid-dani").get());
        await assertFails(comoPersona(entorno, eva).doc("pedidosReclamo/uid-dani").get());
    });

    test("cada cuenta ve si tiene un pedido, aunque no tenga ninguno", async () => {
        await assertSucceeds(comoPersona(entorno, eva).doc("pedidosReclamo/uid-eva").get());
    });

    test("quien creó perfiles lista solo los pedidos que le hicieron", async () => {
        const db = comoPersona(entorno, ana);
        await assertSucceeds(db.collection("pedidosReclamo").where("creador", "==", ana.uid).get());
        await assertFails(db.collection("pedidosReclamo").where("creador", "==", beto.uid).get());
        await assertFails(db.collection("pedidosReclamo").get());
    });

    test("no se modifica: para pedir otro perfil hay que desistir de este", async () => {
        const db = comoPersona(entorno, dani);
        await assertFails(db.doc("pedidosReclamo/uid-dani").set(pedido(dani, dani, ana)));
        await assertFails(db.doc("pedidosReclamo/uid-dani").update({ mail: "otro@test.com" }));
        await assertFails(comoPersona(entorno, ana).doc("pedidosReclamo/uid-dani").update({ mail: "otro@test.com" }));
    });

    test("quien lo hizo puede desistir", async () => {
        await assertSucceeds(comoPersona(entorno, dani).doc("pedidosReclamo/uid-dani").delete());
    });

    test("quien creó el perfil lo acepta: lo reserva para el mail del pedido y el pedido se va", async () => {
        const aceptar = {
            "mails/dani@test.com": null,
            "mails/daniel@test.com": reclamo(ana, dani),
            "pedidosReclamo/uid-dani": null,
        };
        await assertSucceeds(escribir(comoPersona(entorno, ana), aceptar));
        // Ahora Dani se queda con el perfil, como cualquiera al que se lo reservaron.
        const db = comoPersona(entorno, dani);
        const lote = db.batch();
        lote.update(db.doc("perfiles/perfil-dani"), { uid: dani.uid });
        lote.set(db.doc("cuentas/uid-dani"), { perfil: dani.idPerfil });
        lote.set(db.doc("mails/daniel@test.com"), { perfil: dani.idPerfil, uid: dani.uid });
        await assertSucceeds(lote.commit());
    });

    test("quien creó el perfil lo rechaza borrándolo; un tercero no puede", async () => {
        await assertFails(comoPersona(entorno, beto).doc("pedidosReclamo/uid-dani").delete());
        await assertSucceeds(comoPersona(entorno, ana).doc("pedidosReclamo/uid-dani").delete());
    });

    test("el pedido solo no alcanza para quedarse con el perfil", async () => {
        const db = comoPersona(entorno, dani);
        const lote = db.batch();
        lote.update(db.doc("perfiles/perfil-dani"), { uid: dani.uid });
        lote.set(db.doc("cuentas/uid-dani"), { perfil: dani.idPerfil });
        lote.set(db.doc("mails/daniel@test.com"), { perfil: dani.idPerfil, uid: dani.uid });
        await assertFails(lote.commit());
    });
});
