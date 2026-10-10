// Reglas de partidas/ y estadisticasPrevias/ (fase 3 de docs/PLAN_AUTENTICACION.md).
import { after, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { serverTimestamp } from "firebase/firestore";
import { comoPersona, crearEntorno, persona } from "./comun.mjs";

const entorno = await crearEntorno();
const ana = persona("Ana");
const beto = persona("Beto");
const caro = persona("Caro");
const dani = persona("Dani");

beforeEach(() => entorno.clearFirestore());
after(() => entorno.cleanup());

const clave = (ids) => [...ids].sort().join("|");

/** Una partida tal como la guarda la app. `cambios` altera campos; `undefined` los quita. */
function partida(quienAnota, equipoUno, equipoDos, cambios = {}) {
    const jugadores = [...equipoUno, ...equipoDos];
    const equipos = [clave(equipoUno.map((j) => j.idPerfil)), clave(equipoDos.map((j) => j.idPerfil))];
    const datos = {
        equipoUno: equipoUno.map((j) => j.idPerfil),
        equipoDos: equipoDos.map((j) => j.idPerfil),
        jugadores: jugadores.map((j) => j.idPerfil),
        nombres: Object.fromEntries(jugadores.map((j) => [j.idPerfil, j.nombre])),
        equipos,
        enfrentamiento: [...equipos].sort().join("~"),
        equipoGanador: equipos[0],
        empieza: jugadores[0]?.idPerfil ?? "nadie",
        rondas: [{ baseUno: 100, puntosUno: 30, baseDos: 0, puntosDos: -20 }],
        fecha: serverTimestamp(),
        creadaPor: quienAnota.uid,
        ...cambios,
    };
    return Object.fromEntries(Object.entries(datos).filter(([, valor]) => valor !== undefined));
}

const guardar = (quien, datos) => comoPersona(entorno, quien).collection("partidas").add(datos);

describe("guardar una partida", () => {
    test("de a dos y de a cuatro", async () => {
        await assertSucceeds(guardar(ana, partida(ana, [ana], [beto])));
        await assertSucceeds(guardar(ana, partida(ana, [ana, caro], [beto, dani])));
        // El orden en que se sentaron no cambia la clave de la pareja.
        await assertSucceeds(guardar(ana, partida(ana, [dani, ana], [caro, beto])));
    });

    test("la puede anotar alguien que no juega", async () => {
        await assertSucceeds(guardar(caro, partida(caro, [ana], [beto])));
    });

    test("sin mail verificado o sin sesión no se puede", async () => {
        const sinVerificar = { ...ana, verificada: false };
        await assertFails(guardar(sinVerificar, partida(ana, [ana], [beto])));
        const anonimo = entorno.unauthenticatedContext().firestore();
        await assertFails(anonimo.collection("partidas").add(partida(ana, [ana], [beto])));
    });

    test("queda a nombre de quien la anota, con la fecha del servidor", async () => {
        await assertFails(guardar(ana, partida(beto, [ana], [beto])));
        await assertFails(guardar(ana, partida(ana, [ana], [beto], { fecha: new Date(2020, 0, 1) })));
    });

    test("no admite campos de más ni de menos", async () => {
        await assertFails(guardar(ana, partida(ana, [ana], [beto], { extra: true })));
        for (const campo of ["nombres", "equipos", "enfrentamiento", "equipoGanador", "empieza", "rondas", "fecha"]) {
            await assertFails(guardar(ana, partida(ana, [ana], [beto], { [campo]: undefined })));
        }
    });

    test("los equipos tienen que ser de uno o dos, parejos y sin repetir jugadores", async () => {
        await assertFails(guardar(ana, partida(ana, [ana, caro], [beto])));
        await assertFails(guardar(ana, partida(ana, [ana], [ana])));
        await assertFails(guardar(ana, partida(ana, [ana, beto], [beto, caro])));
        await assertFails(guardar(ana, partida(ana, [], [])));
        const tres = persona("Eva");
        await assertFails(guardar(ana, partida(ana, [ana, caro, tres], [beto, dani, persona("Fer")])));
    });

    test("los campos por los que se consulta tienen que coincidir con los equipos", async () => {
        const base = partida(ana, [ana, caro], [beto, dani]);
        // Sumarle una partida a alguien que no jugó.
        await assertFails(guardar(ana, { ...base, jugadores: [...base.jugadores, "perfil-eva"] }));
        await assertFails(guardar(ana, { ...base, jugadores: base.jugadores.slice(0, 3) }));
        await assertFails(guardar(ana, { ...base, equipos: ["perfil-ana", "perfil-beto"] }));
        await assertFails(guardar(ana, { ...base, equipos: [base.equipos[1], base.equipos[0]] }));
        await assertFails(guardar(ana, { ...base, equipos: ["perfil-caro|perfil-ana", base.equipos[1]] }));
        await assertFails(guardar(ana, { ...base, enfrentamiento: "perfil-ana~perfil-beto" }));
        await assertFails(guardar(ana, { ...base, enfrentamiento: [...base.equipos].sort().reverse().join("~") }));
    });

    test("el ganador y quien empieza tienen que ser de la partida", async () => {
        await assertFails(guardar(ana, partida(ana, [ana], [beto], { equipoGanador: "perfil-caro" })));
        await assertFails(guardar(ana, partida(ana, [ana, caro], [beto, dani], { equipoGanador: "perfil-ana" })));
        await assertFails(guardar(ana, partida(ana, [ana], [beto], { empieza: "perfil-caro" })));
    });

    test("hay un nombre por cada jugador, ni más ni menos", async () => {
        await assertFails(guardar(ana, partida(ana, [ana], [beto], { nombres: { "perfil-ana": "Ana" } })));
        const deMas = { "perfil-ana": "Ana", "perfil-beto": "Beto", "perfil-caro": "Caro" };
        await assertFails(guardar(ana, partida(ana, [ana], [beto], { nombres: deMas })));
    });

    test("las rondas son una lista de tamaño razonable", async () => {
        await assertSucceeds(guardar(ana, partida(ana, [ana], [beto], { rondas: [] })));
        await assertFails(guardar(ana, partida(ana, [ana], [beto], { rondas: "muchas" })));
        const demasiadas = Array.from({ length: 301 }, () => ({ baseUno: 0, puntosUno: 0, baseDos: 0, puntosDos: 0 }));
        await assertFails(guardar(ana, partida(ana, [ana], [beto], { rondas: demasiadas })));
    });
});

describe("con partidas guardadas", () => {
    let idPartida;
    beforeEach(async () => {
        idPartida = (await guardar(ana, partida(ana, [ana], [beto]))).id;
        await guardar(ana, partida(ana, [ana, caro], [beto, dani]));
    });

    test("cualquier usuario verificado las consulta y las cuenta", async () => {
        const db = comoPersona(entorno, caro).collection("partidas");
        await assertSucceeds(db.doc(idPartida).get());
        await assertSucceeds(db.where("jugadores", "array-contains", "perfil-ana").orderBy("fecha", "desc").get());
        await assertSucceeds(db.where("equipos", "array-contains", "perfil-ana").get());
        await assertSucceeds(db.where("enfrentamiento", "==", "perfil-ana~perfil-beto").get());
    });

    test("sin verificar o sin sesión no se leen", async () => {
        const sinVerificar = comoPersona(entorno, { ...caro, verificada: false });
        await assertFails(sinVerificar.collection("partidas").doc(idPartida).get());
        await assertFails(entorno.unauthenticatedContext().firestore().collection("partidas").get());
    });

    test("nadie las modifica ni las borra, ni quien las anotó ni quien las jugó", async () => {
        for (const quien of [ana, beto, caro]) {
            const documento = comoPersona(entorno, quien).collection("partidas").doc(idPartida);
            await assertFails(documento.update({ equipoGanador: "perfil-beto" }));
            await assertFails(documento.update({ rondas: [] }));
            await assertFails(documento.delete());
        }
    });
});

describe("estadísticas previas", () => {
    beforeEach(() =>
        entorno.withSecurityRulesDisabled(async (admin) => {
            const db = admin.firestore();
            await db.doc("estadisticasPrevias/perfil-ana").set({ jugadas: 40, ganadas: 22 });
            await db.doc("estadisticasPrevias/perfil-ana/rivales/perfil-beto").set({ jugadas: 12, ganadas: 7 });
        }),
    );

    test("cualquier usuario verificado las lee", async () => {
        const db = comoPersona(entorno, caro);
        await assertSucceeds(db.doc("estadisticasPrevias/perfil-ana").get());
        await assertSucceeds(db.doc("estadisticasPrevias/perfil-ana/rivales/perfil-beto").get());
        // Un equipo sin partidas previas simplemente no tiene documento.
        await assertSucceeds(db.doc("estadisticasPrevias/perfil-caro").get());
    });

    test("sin verificar no se leen", async () => {
        const db = comoPersona(entorno, { ...caro, verificada: false });
        await assertFails(db.doc("estadisticasPrevias/perfil-ana").get());
    });

    test("nadie las escribe, ni las propias", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(db.doc("estadisticasPrevias/perfil-ana").set({ jugadas: 999, ganadas: 999 }));
        await assertFails(db.doc("estadisticasPrevias/perfil-ana").update({ ganadas: 40 }));
        await assertFails(db.doc("estadisticasPrevias/perfil-ana/rivales/perfil-beto").delete());
        await assertFails(db.doc("estadisticasPrevias/perfil-caro").set({ jugadas: 1, ganadas: 1 }));
    });
});
