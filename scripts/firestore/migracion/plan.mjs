// Convierte los datos del esquema anterior (users/ y doubles/) en los documentos del esquema
// nuevo, descripto en docs/PLAN_AUTENTICACION.md. No habla con Firestore: recibe los documentos
// viejos y devuelve los nuevos, así que el mismo respaldo da siempre el mismo resultado.
//
// Esquema anterior:
//
//   users/{usuario}                              Nombre, Password, Amigos, AmigosNombre,
//                                                Partidas Jugadas, Partidas Ganadas
//   users/{usuario}/statistics/{rival}           Partidas Jugadas, Partidas Ganadas
//   users/{usuario}/partidas/{idPartida}         EquipoUno, EquipoDos, Empieza, Rondas, Ganador,
//                                                Fecha (una copia por jugador, con el mismo id)
//   doubles/{pareja}                             Partidas Jugadas, Partidas Ganadas
//   doubles/{pareja}/statisticsDoubles/{rival}   Partidas Jugadas, Partidas Ganadas
//
// El id de un usuario es su nombre en minúsculas. El de una pareja son los dos ids ordenados y
// unidos con "|" o, en los datos más viejos, pegados sin separador ("anazoe").
import { createHash } from "node:crypto";
import { hashDeCredencial } from "./credenciales.mjs";

/** Cuentas que se crearon para probar la app: no se migran, ni nada en lo que participen. */
export const USUARIOS_DE_PRUEBA = ["prueba", "test", "test2", "test3"];

const JUGADAS = "Partidas Jugadas";
const GANADAS = "Partidas Ganadas";

const SEPARADOR_PAREJA = "|";
const SEPARADOR_ENFRENTAMIENTO = "~";

const ALFABETO_IDS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
const LARGO_ID = 20;

/**
 * Id del perfil que le toca a un usuario del esquema anterior. Tiene la forma de los ids
 * automáticos de Firestore, pero sale del id viejo: así volver a correr la migración encuentra
 * los mismos perfiles en lugar de crear otros.
 */
export function idDePerfil(usuario) {
    const bytes = createHash("sha256").update(`perfil:${usuario}`, "utf8").digest();
    return Array.from(bytes.subarray(0, LARGO_ID), (byte) => ALFABETO_IDS[byte % ALFABETO_IDS.length]).join("");
}

/**
 * @param documentos mapa de la ruta de cada documento de users/ y doubles/ a sus datos.
 * @param fecha la que se anota como comienzo de las amistades, que antes no se guardaba.
 * @returns
 *   - `perfiles`: por cada usuario, los documentos que se crean juntos (perfil, nombre
 *     reservado, credencial vieja y amigos);
 *   - `partidas` y `estadisticas`: mapas de ruta a datos;
 *   - `avisos`: lo que no se pudo migrar tal cual, para revisar a mano;
 *   - `omitidos`: cuántos usuarios y partidas de prueba se dejaron afuera.
 */
export function planificarMigracion(documentos, { fecha }) {
    const avisos = [];
    const rutas = Object.keys(documentos).sort();
    const segmentos = (ruta) => ruta.split("/");

    // --- Usuarios ---

    /** Todos los ids de users/, también los que no se migran: sirven para separar parejas. */
    const idsDeUsuarios = rutas.filter((ruta) => /^users\/[^/]+$/.test(ruta)).map((ruta) => segmentos(ruta)[1]);
    const esDePrueba = (usuario) => USUARIOS_DE_PRUEBA.includes(usuario);

    /** Usuarios que se migran: id viejo → { nombre, idPerfil, datos }. */
    const usuarios = new Map();
    for (const usuario of idsDeUsuarios) {
        if (esDePrueba(usuario)) continue;
        const datos = documentos[`users/${usuario}`];
        if (typeof datos.Nombre !== "string" || datos.Nombre === "") {
            avisos.push(`users/${usuario} no tiene nombre: no se migra`);
            continue;
        }
        if (datos.Nombre.toLowerCase() !== usuario) {
            avisos.push(`users/${usuario} se llama "${datos.Nombre}": su nombre no coincide con su id`);
        }
        usuarios.set(usuario, { nombre: datos.Nombre, idPerfil: idDePerfil(usuario), datos });
    }

    const amigosDe = new Map([...usuarios.keys()].map((usuario) => [usuario, new Set()]));
    for (const [usuario, { datos }] of usuarios) {
        for (const amigo of listaDe(datos.Amigos)) {
            if (esDePrueba(amigo) || amigo === usuario) continue;
            if (!usuarios.has(amigo)) {
                avisos.push(`${usuario} tiene de amigo a "${amigo}", que no existe: se omite`);
                continue;
            }
            if (!listaDe(usuarios.get(amigo).datos.Amigos).includes(usuario)) {
                avisos.push(`${usuario} tiene de amigo a ${amigo}, pero no al revés: queda en la lista de los dos`);
            }
            // En el esquema nuevo una amistad figura siempre en las dos listas.
            amigosDe.get(usuario).add(amigo);
            amigosDe.get(amigo).add(usuario);
        }
    }

    const perfiles = [...usuarios].map(([usuario, { nombre, idPerfil, datos }]) => {
        const nuevos = {
            [`perfiles/${idPerfil}`]: { nombre },
            [`nombres/${nombre.toLowerCase()}`]: { perfil: idPerfil },
        };
        if (typeof datos.Password === "string" && datos.Password !== "") {
            nuevos[`credencialesViejas/${idPerfil}`] = { hash: hashDeCredencial(idPerfil, datos.Password) };
        } else {
            avisos.push(`${usuario} no tiene contraseña: su perfil no se va a poder vincular a una cuenta`);
        }
        for (const amigo of [...amigosDe.get(usuario)].sort()) {
            const { nombre: nombreAmigo, idPerfil: idAmigo } = usuarios.get(amigo);
            nuevos[`perfiles/${idPerfil}/amigos/${idAmigo}`] = { nombre: nombreAmigo, desde: fecha };
        }
        return { usuario, idPerfil, nombre, documentos: nuevos };
    });

    // --- Equipos ---

    const claveDeEquipo = (integrantes) =>
        integrantes.map((usuario) => usuarios.get(usuario).idPerfil).sort().join(SEPARADOR_PAREJA);

    /** Nombre legible de cada equipo migrado, para los avisos. */
    const etiquetas = new Map();

    /**
     * Identifica a un equipo del esquema anterior.
     *
     * @returns su clave en el esquema nuevo, `"prueba"` si lo integra un usuario de prueba, o
     *   `null` si no se puede saber quiénes son.
     */
    function resolverEquipo(idViejo, esPareja) {
        const integrantes = esPareja ? separarPareja(idViejo, idsDeUsuarios) : [idViejo];
        if (integrantes === null) return null;
        if (integrantes.some(esDePrueba)) return "prueba";
        if (!integrantes.every((usuario) => usuarios.has(usuario))) return null;
        const clave = claveDeEquipo(integrantes);
        etiquetas.set(clave, integrantes.map((usuario) => usuarios.get(usuario).nombre).join(" y "));
        return clave;
    }

    // --- Estadísticas ---

    /** Clave del equipo → { jugadas, ganadas, rivales: clave del rival → { jugadas, ganadas } }. */
    const estadisticas = new Map();
    const estadisticasDe = (clave) => {
        if (!estadisticas.has(clave)) estadisticas.set(clave, { jugadas: 0, ganadas: 0, rivales: new Map() });
        return estadisticas.get(clave);
    };
    const contraRival = (clave, rival) => {
        const { rivales } = estadisticasDe(clave);
        if (!rivales.has(rival)) rivales.set(rival, { jugadas: 0, ganadas: 0 });
        return rivales.get(rival);
    };
    const sumar = (contador, jugadas, ganadas) => {
        contador.jugadas += jugadas;
        contador.ganadas += ganadas;
    };

    const equiposSinIdentificar = new Set();
    for (const ruta of rutas) {
        const [coleccion, idEquipo, subcoleccion, idRival] = segmentos(ruta);
        if (coleccion !== "users" && coleccion !== "doubles") continue;
        const esPareja = coleccion === "doubles";
        const esGeneral = subcoleccion === undefined;
        const esContraRival = subcoleccion === (esPareja ? "statisticsDoubles" : "statistics");
        if (!esGeneral && !esContraRival) continue;

        const jugadas = numero(documentos[ruta][JUGADAS]);
        const ganadas = numero(documentos[ruta][GANADAS]);
        if (jugadas === 0 && ganadas === 0) continue;

        const equipo = resolverEquipo(idEquipo, esPareja);
        if (equipo === "prueba") continue;
        if (equipo === null) {
            equiposSinIdentificar.add(`${coleccion}/${idEquipo}`);
            continue;
        }
        if (esGeneral) {
            // Una pareja puede tener dos documentos, uno por cada formato de id: se suman.
            sumar(estadisticasDe(equipo), jugadas, ganadas);
            continue;
        }
        const rival = resolverEquipo(idRival, esPareja);
        if (rival === "prueba") {
            // Las partidas contra usuarios de prueba tampoco cuentan en el total.
            sumar(estadisticasDe(equipo), -jugadas, -ganadas);
        } else if (rival === null) {
            avisos.push(`${ruta}: no se pudo identificar al rival; esas partidas quedan solo en el total`);
        } else {
            sumar(contraRival(equipo, rival), jugadas, ganadas);
        }
    }
    for (const equipo of equiposSinIdentificar) {
        avisos.push(`${equipo}: no se pudo identificar al equipo; sus estadísticas no se migran`);
    }

    // --- Partidas ---

    /** Id de la partida → rutas de sus copias, una por jugador. */
    const copias = new Map();
    for (const ruta of rutas) {
        const [coleccion, , subcoleccion, idPartida] = segmentos(ruta);
        if (coleccion !== "users" || subcoleccion !== "partidas") continue;
        copias.set(idPartida, [...(copias.get(idPartida) ?? []), ruta]);
    }

    const partidas = {};
    let partidasDePrueba = 0;
    for (const [idPartida, rutasDeCopias] of copias) {
        const [primera, ...otras] = rutasDeCopias;
        const resultado = convertirPartida(documentos[primera]);
        if (resultado === "prueba") {
            partidasDePrueba++;
            continue;
        }
        if (typeof resultado === "string") {
            avisos.push(`La partida ${idPartida} no se migra: ${resultado}`);
            continue;
        }
        const distinta = otras.find((ruta) => !sonIguales(documentos[ruta], documentos[primera]));
        if (distinta) avisos.push(`La partida ${idPartida} no es igual en ${primera} y en ${distinta}: se usa la primera`);

        partidas[`partidas/${idPartida}`] = resultado;
        // Los contadores ya incluyen esta partida: se la descuenta para no contarla dos veces.
        const [uno, dos] = resultado.equipos;
        for (const [equipo, rival] of [[uno, dos], [dos, uno]]) {
            const ganadas = resultado.equipoGanador === equipo ? 1 : 0;
            sumar(estadisticasDe(equipo), -1, -ganadas);
            sumar(contraRival(equipo, rival), -1, -ganadas);
        }
    }

    /** @returns la partida en el esquema nuevo, `"prueba"` si es de prueba, o el motivo por el que no sirve. */
    function convertirPartida(datos) {
        const equipoUno = listaDe(datos.EquipoUno);
        const equipoDos = listaDe(datos.EquipoDos);
        const nombres = [...equipoUno, ...equipoDos];
        if (![1, 2].includes(equipoUno.length) || equipoDos.length !== equipoUno.length || nombres.length === 0) {
            return "los equipos están mal formados";
        }
        // La partida guardaba nombres; el id de cada usuario era su nombre en minúsculas.
        const jugadores = nombres.map((nombre) => nombre.toLowerCase());
        if (jugadores.some(esDePrueba)) return "prueba";
        const desconocido = jugadores.find((usuario) => !usuarios.has(usuario));
        if (desconocido !== undefined) return `juega "${desconocido}", que no existe`;
        if (new Set(jugadores).size !== jugadores.length) return "hay un jugador repetido";
        if (datos.Ganador !== "UNO" && datos.Ganador !== "DOS") return "no dice quién ganó";
        if (typeof datos.Empieza !== "string" || !jugadores.includes(datos.Empieza.toLowerCase())) {
            return "no dice quién empezó";
        }
        if (!datos.Fecha) return "no tiene fecha";
        const rondas = listaDe(datos.Rondas, "object").map((ronda) => ({
            baseUno: ronda.BaseUno,
            puntosUno: ronda.PuntosUno,
            baseDos: ronda.BaseDos,
            puntosDos: ronda.PuntosDos,
        }));
        if (rondas.some((ronda) => Object.values(ronda).some((valor) => typeof valor !== "number"))) {
            return "tiene rondas mal formadas";
        }

        const idDe = (nombre) => usuarios.get(nombre.toLowerCase()).idPerfil;
        const claveUno = resolverEquipo(...idAnteriorDe(equipoUno));
        const claveDos = resolverEquipo(...idAnteriorDe(equipoDos));
        return {
            equipoUno: equipoUno.map(idDe),
            equipoDos: equipoDos.map(idDe),
            // Lo que sigue repite a los equipos en la forma en que la app los consulta.
            jugadores: nombres.map(idDe),
            // El nombre que cada uno tenía al jugar.
            nombres: Object.fromEntries(nombres.map((nombre) => [idDe(nombre), nombre])),
            equipos: [claveUno, claveDos],
            enfrentamiento: [claveUno, claveDos].sort().join(SEPARADOR_ENFRENTAMIENTO),
            equipoGanador: datos.Ganador === "UNO" ? claveUno : claveDos,
            empieza: idDe(datos.Empieza),
            rondas,
            fecha: datos.Fecha,
        };
    }

    // --- Documentos de estadísticas ---

    const documentosDeEstadisticas = {};
    for (const [equipo, { jugadas, ganadas, rivales }] of estadisticas) {
        const etiqueta = etiquetas.get(equipo);
        const general = sinNegativos({ jugadas, ganadas }, etiqueta, avisos);
        if (general.jugadas > 0) documentosDeEstadisticas[`estadisticasPrevias/${equipo}`] = general;

        let jugadasContraRivales = 0;
        for (const [rival, contador] of rivales) {
            const contraEste = sinNegativos(contador, `${etiqueta} contra ${etiquetas.get(rival)}`, avisos);
            jugadasContraRivales += contraEste.jugadas;
            if (contraEste.jugadas > 0) {
                documentosDeEstadisticas[`estadisticasPrevias/${equipo}/rivales/${rival}`] = contraEste;
            }
        }
        if (jugadasContraRivales !== general.jugadas) {
            avisos.push(
                `${etiqueta}: el total dice ${general.jugadas} partidas, pero sumando por rival dan ${jugadasContraRivales}`,
            );
        }
    }

    return {
        perfiles,
        partidas,
        estadisticas: documentosDeEstadisticas,
        avisos,
        omitidos: { usuariosDePrueba: idsDeUsuarios.filter(esDePrueba).length, partidasDePrueba },
    };
}

/**
 * Los dos integrantes de una pareja a partir de su id. Para el formato sin separador prueba qué
 * usuarios existentes, pegados, dan ese id; si no hay exactamente una pareja posible, `null`.
 */
function separarPareja(idPareja, idsDeUsuarios) {
    if (idPareja.includes(SEPARADOR_PAREJA)) {
        const integrantes = idPareja.split(SEPARADOR_PAREJA);
        return integrantes.length === 2 ? integrantes : null;
    }
    const posibles = new Map();
    for (const uno of idsDeUsuarios) {
        const otro = idPareja.slice(uno.length);
        if (idPareja.startsWith(uno) && otro !== uno && idsDeUsuarios.includes(otro)) {
            posibles.set([uno, otro].sort().join(SEPARADOR_PAREJA), [uno, otro]);
        }
    }
    return posibles.size === 1 ? [...posibles.values()][0] : null;
}

/** El id que tenía un equipo en el esquema anterior, y si es una pareja: lo que espera `resolverEquipo`. */
function idAnteriorDe(nombres) {
    const usuarios = nombres.map((nombre) => nombre.toLowerCase());
    return [usuarios.sort().join(SEPARADOR_PAREJA), usuarios.length === 2];
}

/** Los contadores viejos pueden estar desfasados: un negativo se informa y queda en cero. */
function sinNegativos({ jugadas, ganadas }, etiqueta, avisos) {
    if (jugadas < 0 || ganadas < 0) {
        avisos.push(`${etiqueta}: hay más partidas con detalle que las que decían los contadores; quedan en cero`);
        return { jugadas: Math.max(jugadas, 0), ganadas: Math.max(ganadas, 0) };
    }
    if (ganadas > jugadas) avisos.push(`${etiqueta}: figura con más partidas ganadas que jugadas`);
    return { jugadas, ganadas };
}

/** Los elementos del tipo esperado de un campo que debería ser una lista. */
function listaDe(valor, tipo = "string") {
    return Array.isArray(valor) ? valor.filter((elemento) => elemento !== null && typeof elemento === tipo) : [];
}

function numero(valor) {
    return Number.isFinite(valor) ? valor : 0;
}

function sonIguales(uno, otro) {
    return JSON.stringify(ordenado(uno)) === JSON.stringify(ordenado(otro));
}

/** El mismo valor con las claves de cada mapa en orden, para compararlo sin que el orden importe. */
function ordenado(valor) {
    if (Array.isArray(valor)) return valor.map(ordenado);
    if (valor === null || typeof valor !== "object" || Object.getPrototypeOf(valor) !== Object.prototype) return valor;
    return Object.fromEntries(Object.keys(valor).sort().map((clave) => [clave, ordenado(valor[clave])]));
}
