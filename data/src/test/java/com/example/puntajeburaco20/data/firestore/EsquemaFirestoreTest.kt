package com.example.puntajeburaco20.data.firestore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EsquemaFirestoreTest {

    // El mismo caso está en scripts/firestore/migracion/credenciales.test.mjs: si la app y el
    // script de migración calcularan distinto, nadie podría vincular su perfil anterior.
    @Test
    fun `la prueba de la clave vieja es la misma que calcula el script de migracion`() {
        assertEquals(
            "fe85cb81ce63cfa8402eba50f7340b8c9635fc81496a997252bc498c77fd83c6",
            EsquemaFirestore.pruebaDeClaveVieja("perfilDeAna", "ñandú 1234"),
        )
    }

    @Test
    fun `la misma clave da pruebas distintas en perfiles distintos`() {
        assertNotEquals(
            EsquemaFirestore.pruebaDeClaveVieja("perfilDeAna", "1234"),
            EsquemaFirestore.pruebaDeClaveVieja("perfilDeBeto", "1234"),
        )
    }
}
