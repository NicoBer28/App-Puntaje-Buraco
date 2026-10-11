import assert from "node:assert/strict";
import { test } from "node:test";
import { hashDeCredencial, pruebaDeClave } from "./credenciales.mjs";

// El mismo caso está en EsquemaFirestoreTest.kt: si la app y este script calcularan distinto,
// nadie podría vincular su perfil anterior.
test("la prueba de la clave vieja es la misma que calcula la app", () => {
    assert.equal(pruebaDeClave("perfilDeAna", "ñandú 1234"), "fe85cb81ce63cfa8402eba50f7340b8c9635fc81496a997252bc498c77fd83c6");
});

test("lo que se guarda es el hash de la prueba, no la prueba", () => {
    assert.equal(hashDeCredencial("perfilDeAna", "ñandú 1234"), "7104b476a77f131d56122d3c7b588496149a3682f06666c3b582faa2ce90e7dd");
});
