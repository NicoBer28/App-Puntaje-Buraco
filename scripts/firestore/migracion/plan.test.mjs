// Tests de la conversión del esquema anterior al nuevo. No necesitan el emulador.
import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { hashDeCredencial } from "./credenciales.mjs";
import { idDePerfil, planificarMigracion } from "./plan.mjs";

const FECHA = new Date("2026-10-10T12:00:00Z");
const JUGADA = new Date("2026-09-01T20:00:00Z");

const [ana, beto, caro, dani] = ["ana", "beto", "caro", "dani"].map(idDePerfil);
const pareja = (...ids) => ids.sort().join("|");

function usuario(nombre, amigos = [], contadores = {}) {
    return { Nombre: nombre, Password: `clave-${nombre}`, Amigos: amigos, AmigosNombre: amigos, ...contadores };
}

const contador = (jugadas, ganadas) => ({ "Partidas Jugadas": jugadas, ...(ganadas ? { "Partidas Ganadas": ganadas } : {}) });

function partida(equipoUno, equipoDos, ganador = "UNO") {
    return {
        EquipoUno: equipoUno,
        EquipoDos: equipoDos,
        Empieza: equipoUno[0],
        Ganador: ganador,
        Fecha: JUGADA,
        Rondas: [{ BaseUno: 100, PuntosUno: 250, BaseDos: 50, PuntosDos: -30 }],
    };
}

const migrar = (documentos) => planificarMigracion(documentos, { fecha: FECHA });

/** Todos los documentos nuevos de los perfiles, juntos. */
const documentosDePerfiles = (plan) => Object.assign({}, ...plan.perfiles.map((perfil) => perfil.documentos));

describe("perfiles", () => {
    test("cada usuario pasa a ser un perfil con su nombre reservado y el hash de su contraseña", () => {
        const plan = migrar({ "users/ana": usuario("Ana") });

        assert.deepEqual(plan.perfiles, [
            {
                usuario: "ana",
                idPerfil: ana,
                nombre: "Ana",
                documentos: {
                    [`perfiles/${ana}`]: { nombre: "Ana" },
                    "nombres/ana": { perfil: ana },
                    [`credencialesViejas/${ana}`]: { hash: hashDeCredencial(ana, "clave-Ana") },
                },
            },
        ]);
        assert.deepEqual(plan.avisos, []);
    });

    test("el id del perfil tiene la forma de un id automático y es siempre el mismo", () => {
        assert.match(ana, /^[A-Za-z0-9]{20}$/);
        assert.equal(idDePerfil("ana"), ana);
        assert.notEqual(ana, beto);
    });

    test("las amistades quedan en la lista de los dos, con el nombre del amigo", () => {
        const plan = migrar({ "users/ana": usuario("Ana", ["beto"]), "users/beto": usuario("Beto", ["ana"]) });
        const documentos = documentosDePerfiles(plan);

        assert.deepEqual(documentos[`perfiles/${ana}/amigos/${beto}`], { nombre: "Beto", desde: FECHA });
        assert.deepEqual(documentos[`perfiles/${beto}/amigos/${ana}`], { nombre: "Ana", desde: FECHA });
        assert.deepEqual(plan.avisos, []);
    });

    test("una amistad que figuraba de un solo lado se completa y se avisa", () => {
        const plan = migrar({ "users/ana": usuario("Ana", ["beto"]), "users/beto": usuario("Beto") });
        const documentos = documentosDePerfiles(plan);

        assert.ok(documentos[`perfiles/${ana}/amigos/${beto}`]);
        assert.ok(documentos[`perfiles/${beto}/amigos/${ana}`]);
        assert.equal(plan.avisos.length, 1);
    });

    test("un amigo que no existe se omite y se avisa", () => {
        const plan = migrar({ "users/ana": usuario("Ana", ["nadie"]) });

        assert.deepEqual(Object.keys(documentosDePerfiles(plan)).filter((ruta) => ruta.includes("/amigos/")), []);
        assert.equal(plan.avisos.length, 1);
    });

    test("un usuario sin contraseña se migra sin credencial y se avisa", () => {
        const plan = migrar({ "users/ana": { Nombre: "Ana", Amigos: [] } });

        assert.deepEqual(Object.keys(plan.perfiles[0].documentos), [`perfiles/${ana}`, "nombres/ana"]);
        assert.equal(plan.avisos.length, 1);
    });

    test("un usuario sin nombre no se migra y se avisa", () => {
        const plan = migrar({ "users/ana": { Password: "1234" } });

        assert.deepEqual(plan.perfiles, []);
        assert.equal(plan.avisos.length, 1);
    });
});

describe("usuarios de prueba", () => {
    const documentos = {
        "users/ana": usuario("Ana", ["test", "beto"], contador(5, 3)),
        "users/ana/statistics/test": contador(2, 2),
        "users/ana/statistics/beto": contador(3, 1),
        "users/beto": usuario("Beto", ["ana"], contador(3, 2)),
        "users/beto/statistics/ana": contador(3, 2),
        "users/test": usuario("test", ["ana"], contador(2)),
        "users/test/statistics/ana": contador(2),
        "users/test/partidas/p1": partida(["test"], ["Ana"]),
        "users/ana/partidas/p1": partida(["test"], ["Ana"]),
        "users/test2": usuario("teST2"),
        "doubles/ana|beto": contador(1, 1),
        "doubles/ana|beto/statisticsDoubles/test|test2": contador(1, 1),
        "doubles/test|test2": contador(1),
        "doubles/test|test2/statisticsDoubles/ana|beto": contador(1),
    };
    const plan = migrar(documentos);

    test("no se migran, ni sus amistades ni sus partidas", () => {
        assert.deepEqual(plan.perfiles.map((perfil) => perfil.usuario), ["ana", "beto"]);
        assert.deepEqual(
            Object.keys(documentosDePerfiles(plan)).filter((ruta) => ruta.includes("/amigos/")),
            [`perfiles/${ana}/amigos/${beto}`, `perfiles/${beto}/amigos/${ana}`],
        );
        assert.deepEqual(plan.partidas, {});
        assert.deepEqual(plan.omitidos, { usuariosDePrueba: 2, partidasDePrueba: 1 });
    });

    test("las partidas contra ellos se descuentan del total de los demás", () => {
        assert.deepEqual(plan.estadisticas, {
            [`estadisticasPrevias/${ana}`]: { jugadas: 3, ganadas: 1 },
            [`estadisticasPrevias/${ana}/rivales/${beto}`]: { jugadas: 3, ganadas: 1 },
            [`estadisticasPrevias/${beto}`]: { jugadas: 3, ganadas: 2 },
            [`estadisticasPrevias/${beto}/rivales/${ana}`]: { jugadas: 3, ganadas: 2 },
        });
        assert.deepEqual(plan.avisos, []);
    });
});

describe("estadísticas de parejas", () => {
    const cuatro = {
        "users/ana": usuario("Ana"),
        "users/beto": usuario("Beto"),
        "users/caro": usuario("Caro"),
        "users/dani": usuario("Dani"),
    };

    test("se suman los dos formatos de id de una misma pareja", () => {
        const plan = migrar({
            ...cuatro,
            "doubles/anabeto": contador(4, 1),
            "doubles/anabeto/statisticsDoubles/carodani": contador(4, 1),
            "doubles/ana|beto": contador(2, 2),
            "doubles/ana|beto/statisticsDoubles/caro|dani": contador(2, 2),
        });

        assert.deepEqual(plan.estadisticas, {
            [`estadisticasPrevias/${pareja(ana, beto)}`]: { jugadas: 6, ganadas: 3 },
            [`estadisticasPrevias/${pareja(ana, beto)}/rivales/${pareja(caro, dani)}`]: { jugadas: 6, ganadas: 3 },
        });
        assert.deepEqual(plan.avisos, []);
    });

    test("un id sin separador se resuelve aunque un nombre sea el comienzo de otro", () => {
        const plan = migrar({ ...cuatro, "users/anab": usuario("AnaB"), "doubles/anabcaro": contador(1) });

        assert.deepEqual(Object.keys(plan.estadisticas), [`estadisticasPrevias/${pareja(idDePerfil("anab"), caro)}`]);
    });

    test("un id que puede ser de dos parejas distintas no se migra y se avisa", () => {
        // "anabeto" puede ser ana + beto o anab + eto.
        const plan = migrar({ ...cuatro, "users/anab": usuario("AnaB"), "users/eto": usuario("Eto"), "doubles/anabeto": contador(1) });

        assert.deepEqual(plan.estadisticas, {});
        assert.deepEqual(plan.avisos, ["doubles/anabeto: no se pudo identificar al equipo; sus estadísticas no se migran"]);
    });

    test("un id que no corresponde a ningún usuario no se migra y se avisa", () => {
        const plan = migrar({ ...cuatro, "doubles/anazoe": contador(1), "doubles/ana|zoe": contador(1) });

        assert.deepEqual(plan.estadisticas, {});
        assert.equal(plan.avisos.length, 2);
    });

    test("si no se identifica al rival, las partidas quedan solo en el total", () => {
        const plan = migrar({
            ...cuatro,
            "doubles/ana|beto": contador(2, 1),
            "doubles/ana|beto/statisticsDoubles/carozoe": contador(2, 1),
        });

        assert.deepEqual(plan.estadisticas, { [`estadisticasPrevias/${pareja(ana, beto)}`]: { jugadas: 2, ganadas: 1 } });
        assert.equal(plan.avisos.length, 2);
    });
});

describe("partidas", () => {
    const documentos = {
        "users/ana": usuario("Ana", [], contador(3, 2)),
        "users/ana/statistics/beto": contador(3, 2),
        "users/beto": usuario("Beto", [], contador(3, 1)),
        "users/beto/statistics/ana": contador(3, 1),
        "users/caro": usuario("Caro"),
        "users/dani": usuario("Dani"),
        "users/ana/partidas/p1": partida(["Beto"], ["Ana"], "DOS"),
        "users/beto/partidas/p1": partida(["Beto"], ["Ana"], "DOS"),
        "doubles/ana|caro": contador(1),
        "doubles/ana|caro/statisticsDoubles/beto|dani": contador(1),
        "doubles/beto|dani": contador(1, 1),
        "doubles/beto|dani/statisticsDoubles/ana|caro": contador(1, 1),
    };
    for (const jugador of ["ana", "beto", "caro", "dani"]) {
        documentos[`users/${jugador}/partidas/p2`] = partida(["Dani", "Beto"], ["Caro", "Ana"]);
    }
    const plan = migrar(documentos);

    test("las copias de cada jugador pasan a ser un solo documento, con el mismo id", () => {
        assert.deepEqual(Object.keys(plan.partidas), ["partidas/p1", "partidas/p2"]);
        assert.deepEqual(plan.partidas["partidas/p1"], {
            equipoUno: [beto],
            equipoDos: [ana],
            jugadores: [beto, ana],
            nombres: { [beto]: "Beto", [ana]: "Ana" },
            equipos: [beto, ana],
            enfrentamiento: [ana, beto].sort().join("~"),
            equipoGanador: ana,
            empieza: beto,
            rondas: [{ baseUno: 100, puntosUno: 250, baseDos: 50, puntosDos: -30 }],
            fecha: JUGADA,
        });
    });

    test("en una partida de parejas, cada equipo se identifica sin importar el orden", () => {
        const deParejas = plan.partidas["partidas/p2"];

        assert.deepEqual(deParejas.equipoUno, [dani, beto]);
        assert.deepEqual(deParejas.jugadores, [dani, beto, caro, ana]);
        assert.deepEqual(deParejas.equipos, [pareja(beto, dani), pareja(ana, caro)]);
        assert.equal(deParejas.enfrentamiento, [pareja(beto, dani), pareja(ana, caro)].sort().join("~"));
        assert.equal(deParejas.equipoGanador, pareja(beto, dani));
        assert.equal(deParejas.empieza, dani);
    });

    test("se descuentan de los contadores, para no contarlas dos veces", () => {
        assert.deepEqual(plan.estadisticas, {
            [`estadisticasPrevias/${ana}`]: { jugadas: 2, ganadas: 1 },
            [`estadisticasPrevias/${ana}/rivales/${beto}`]: { jugadas: 2, ganadas: 1 },
            [`estadisticasPrevias/${beto}`]: { jugadas: 2, ganadas: 1 },
            [`estadisticasPrevias/${beto}/rivales/${ana}`]: { jugadas: 2, ganadas: 1 },
        });
        assert.deepEqual(plan.avisos, []);
    });

    test("si el contador no la incluía, queda en cero y se avisa", () => {
        const plan = migrar({
            "users/ana": usuario("Ana"),
            "users/beto": usuario("Beto"),
            "users/ana/partidas/p1": partida(["Ana"], ["Beto"]),
        });

        assert.deepEqual(Object.keys(plan.partidas), ["partidas/p1"]);
        assert.deepEqual(plan.estadisticas, {});
        assert.equal(plan.avisos.length, 4);
    });

    test("una partida mal formada o con un jugador que no existe no se migra y se avisa", () => {
        const plan = migrar({
            "users/ana": usuario("Ana"),
            "users/beto": usuario("Beto"),
            "users/ana/partidas/p1": partida(["Ana"], ["Zoe"]),
            "users/ana/partidas/p2": { ...partida(["Ana"], ["Beto"]), Ganador: "TRES" },
            "users/ana/partidas/p3": partida(["Ana", "Beto"], ["Beto"]),
            "users/ana/partidas/p4": { ...partida(["Ana"], ["Beto"]), Rondas: [{ BaseUno: 100 }] },
        });

        assert.deepEqual(plan.partidas, {});
        assert.equal(plan.avisos.length, 4);
    });

    test("si las copias de una partida no coinciden se usa la primera y se avisa", () => {
        const plan = migrar({
            "users/ana": usuario("Ana", [], contador(1, 1)),
            "users/ana/statistics/beto": contador(1, 1),
            "users/beto": usuario("Beto", [], contador(1)),
            "users/beto/statistics/ana": contador(1),
            "users/ana/partidas/p1": partida(["Ana"], ["Beto"], "UNO"),
            "users/beto/partidas/p1": partida(["Ana"], ["Beto"], "DOS"),
        });

        assert.equal(plan.partidas["partidas/p1"].equipoGanador, ana);
        assert.equal(plan.avisos.length, 1);
    });
});

test("el mismo respaldo da siempre el mismo resultado", () => {
    const documentos = {
        "users/ana": usuario("Ana", ["beto"], contador(2, 1)),
        "users/ana/statistics/beto": contador(2, 1),
        "users/beto": usuario("Beto", ["ana"], contador(2, 1)),
        "users/beto/statistics/ana": contador(2, 1),
        "users/ana/partidas/p1": partida(["Ana"], ["Beto"]),
    };

    assert.deepEqual(migrar(documentos), migrar(documentos));
});
