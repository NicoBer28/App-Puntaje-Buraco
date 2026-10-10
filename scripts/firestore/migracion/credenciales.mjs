// Cómo se guarda la contraseña de una cuenta anterior al corte, para que su dueño pueda vincular
// el perfil a su cuenta nueva sin que la contraseña quede escrita en ningún lado.
//
// Tiene que coincidir con `sabeLaClaveVieja` en firestore.rules y con `pruebaDeClaveVieja` en
// FirestoreUsuarioRepository.kt.
import { createHash } from "node:crypto";

const sha256 = (texto) => createHash("sha256").update(texto, "utf8").digest("hex");

/**
 * Lo que la app envía en lugar de la contraseña. Incluye el id del perfil para que dos personas
 * con la misma contraseña no tengan la misma prueba.
 */
export function pruebaDeClave(idPerfil, clave) {
    return sha256(`${idPerfil}:${clave}`);
}

/** Lo que queda en `credencialesViejas/{idPerfil}`: las reglas lo comparan con el hash de la prueba. */
export function hashDeCredencial(idPerfil, clave) {
    return sha256(pruebaDeClave(idPerfil, clave));
}
