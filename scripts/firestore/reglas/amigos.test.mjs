// Reglas de las amistades y de los perfiles sin login (fase 2 de docs/PLAN_AUTENTICACION.md).
import { after, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import {
    altaDePerfil,
    altaSinLogin,
    amistad,
    amistadCon,
    comoPersona,
    crearEntorno,
    escribir,
    persona,
    rutaAmistad,
} from "./comun.mjs";

const entorno = await crearEntorno();
const ana = persona("Ana");
const beto = persona("Beto");
const caro = persona("Caro");
// Dani no usa la app: su perfil lo crea Ana.
const dani = { idPerfil: "perfil-dani", nombre: "Dani" };

beforeEach(async () => {
    await entorno.clearFirestore();
    for (const quien of [ana, beto, caro]) await altaDePerfil(comoPersona(entorno, quien), quien);
});
after(() => entorno.cleanup());

const sinAmistad = (uno, otro) => ({ [rutaAmistad(uno, otro)]: null, [rutaAmistad(otro, uno)]: null });

describe("agregar un amigo", () => {
    test("cualquiera de los dos crea la amistad en ambas listas, sin que el otro acepte", async () => {
        await assertSucceeds(escribir(comoPersona(entorno, ana), amistad(ana, beto)));
        await assertSucceeds(escribir(comoPersona(entorno, caro), amistad(ana, caro)));
    });

    test("no se puede crear un solo lado", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(escribir(db, { [rutaAmistad(ana, beto)]: amistadCon(beto) }));
        await assertFails(escribir(db, { [rutaAmistad(beto, ana)]: amistadCon(ana) }));
    });

    test("un tercero no puede hacer amigos a otros dos", async () => {
        await assertFails(escribir(comoPersona(entorno, caro), amistad(ana, beto)));
    });

    test("no se puede con uno mismo ni con un perfil que no existe", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(escribir(db, { [rutaAmistad(ana, ana)]: amistadCon(ana) }));
        await assertFails(escribir(db, amistad(ana, { idPerfil: "perfil-nadie", nombre: "Nadie" })));
    });

    test("el nombre guardado tiene que ser el del amigo", async () => {
        const conOtroNombre = { ...amistad(ana, beto), [rutaAmistad(ana, beto)]: amistadCon(caro) };
        await assertFails(escribir(comoPersona(entorno, ana), conOtroNombre));
    });

    test("la fecha la pone el servidor y no se admiten campos de más", async () => {
        const db = comoPersona(entorno, ana);
        const fechaInventada = { nombre: "Beto", desde: new Date(2020, 0, 1) };
        await assertFails(escribir(db, { ...amistad(ana, beto), [rutaAmistad(ana, beto)]: fechaInventada }));
        const conExtra = { ...amistadCon(beto), favorito: true };
        await assertFails(escribir(db, { ...amistad(ana, beto), [rutaAmistad(ana, beto)]: conExtra }));
    });

    test("sin mail verificado no se puede", async () => {
        const sinVerificar = { ...ana, verificada: false };
        await assertFails(escribir(comoPersona(entorno, sinVerificar), amistad(ana, beto)));
    });
});

describe("con una amistad ya creada", () => {
    beforeEach(() => escribir(comoPersona(entorno, ana), amistad(ana, beto)));

    test("cada uno lee solo su propia lista de amigos", async () => {
        const db = comoPersona(entorno, ana);
        await assertSucceeds(db.collection("perfiles/perfil-ana/amigos").get());
        await assertSucceeds(db.doc(rutaAmistad(ana, beto)).get());
        await assertFails(db.collection("perfiles/perfil-beto/amigos").get());
        await assertFails(comoPersona(entorno, caro).doc(rutaAmistad(ana, beto)).get());
    });

    test("cualquiera de los dos la quita, de ambas listas", async () => {
        await assertSucceeds(escribir(comoPersona(entorno, beto), sinAmistad(ana, beto)));
    });

    test("no se puede quitar un solo lado", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(escribir(db, { [rutaAmistad(ana, beto)]: null }));
        await assertFails(escribir(db, { [rutaAmistad(beto, ana)]: null }));
    });

    test("un tercero no puede quitarla", async () => {
        await assertFails(escribir(comoPersona(entorno, caro), sinAmistad(ana, beto)));
    });

    test("no se puede modificar ni volver a crear", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(db.doc(rutaAmistad(ana, beto)).update({ nombre: "Otro" }));
        await assertFails(escribir(db, amistad(ana, beto)));
    });
});

describe("crear un usuario para alguien que no usa la app", () => {
    test("se crea el perfil, se reserva su nombre y queda como amigo de quien lo creó", async () => {
        await assertSucceeds(escribir(comoPersona(entorno, ana), altaSinLogin(ana, dani)));
    });

    test("no puede quedar suelto, sin estar en la lista de quien lo crea", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(escribir(db, altaSinLogin(ana, dani, sinAmistad(ana, dani))));
        // Tampoco como amigo de otra persona en lugar del creador.
        await assertFails(escribir(db, { ...altaSinLogin(ana, dani, sinAmistad(ana, dani)), ...amistad(beto, dani) }));
    });

    test("no se puede crear a nombre de otro creador, ni con cuenta ajena", async () => {
        const db = comoPersona(entorno, ana);
        const deOtro = { nombre: "Dani", creadoPor: beto.uid };
        await assertFails(escribir(db, altaSinLogin(ana, dani, { "perfiles/perfil-dani": deOtro })));
        const conCuenta = { nombre: "Dani", uid: beto.uid };
        await assertFails(escribir(db, altaSinLogin(ana, dani, { "perfiles/perfil-dani": conCuenta })));
        const conAmbos = { nombre: "Dani", uid: ana.uid, creadoPor: ana.uid };
        await assertFails(escribir(db, altaSinLogin(ana, dani, { "perfiles/perfil-dani": conAmbos })));
    });

    test("el nombre tiene que ser válido, estar libre y quedar reservado", async () => {
        const db = comoPersona(entorno, ana);
        await assertFails(escribir(db, altaSinLogin(ana, { idPerfil: "perfil-x", nombre: "Da" })));
        await assertFails(escribir(db, altaSinLogin(ana, { idPerfil: "perfil-x", nombre: "BETO" })));
        await assertFails(escribir(db, altaSinLogin(ana, dani, { "nombres/dani": null })));
    });

    test("hace falta tener perfil propio y mail verificado", async () => {
        const sinPerfil = persona("Eva");
        await assertFails(escribir(comoPersona(entorno, sinPerfil), altaSinLogin(sinPerfil, dani)));
        const sinVerificar = { ...ana, verificada: false };
        await assertFails(escribir(comoPersona(entorno, sinVerificar), altaSinLogin(ana, dani)));
    });
});

describe("con un perfil sin login ya creado", () => {
    beforeEach(() => escribir(comoPersona(entorno, ana), altaSinLogin(ana, dani)));

    test("otra persona lo encuentra por su nombre y lo agrega como amigo", async () => {
        const db = comoPersona(entorno, beto);
        await assertSucceeds(db.doc("nombres/dani").get());
        await assertSucceeds(db.doc("perfiles/perfil-dani").get());
        await assertSucceeds(escribir(db, amistad(beto, dani)));
    });

    test("su lista de amigos la lee solo quien lo creó", async () => {
        await assertSucceeds(comoPersona(entorno, ana).collection("perfiles/perfil-dani/amigos").get());
        await assertFails(comoPersona(entorno, beto).collection("perfiles/perfil-dani/amigos").get());
    });

    test("quien lo creó puede quitarlo de la lista de otro", async () => {
        await escribir(comoPersona(entorno, beto), amistad(beto, dani));
        await assertFails(escribir(comoPersona(entorno, caro), sinAmistad(beto, dani)));
        await assertSucceeds(escribir(comoPersona(entorno, ana), sinAmistad(beto, dani)));
    });

    test("quien lo creó puede dejar de ser su amigo y volver a agregarlo", async () => {
        const db = comoPersona(entorno, ana);
        await assertSucceeds(escribir(db, sinAmistad(ana, dani)));
        await assertSucceeds(escribir(db, amistad(ana, dani)));
    });

    test("nadie lo modifica ni lo borra, ni se queda con él", async () => {
        for (const db of [comoPersona(entorno, ana), comoPersona(entorno, beto)]) {
            await assertFails(db.doc("perfiles/perfil-dani").update({ nombre: "Otro" }));
            await assertFails(db.doc("perfiles/perfil-dani").update({ uid: beto.uid }));
            await assertFails(db.doc("perfiles/perfil-dani").update({ creadoPor: beto.uid }));
            await assertFails(db.doc("perfiles/perfil-dani").delete());
            await assertFails(db.doc("nombres/dani").delete());
        }
    });
});
