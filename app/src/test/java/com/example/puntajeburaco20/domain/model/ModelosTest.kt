package com.example.puntajeburaco20.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelosTest {

    @Test
    fun `el id de un jugador no distingue mayusculas`() {
        assertEquals("juanpe", Jugador("JuanPe").id)
        assertTrue(Jugador("JuanPe").esMismaPersona(Jugador("juanpe")))
    }

    @Test
    fun `el id de una pareja no depende del orden de sus integrantes`() {
        val pareja = Equipo(listOf(Jugador("Zoe"), Jugador("ana")))

        assertEquals("anazoe", pareja.id)
        assertEquals(pareja.id, Equipo(listOf(Jugador("Ana"), Jugador("zoe"))).id)
        assertTrue(pareja.esPareja)
    }

    @Test
    fun `el id de un jugador solo es el de su cuenta`() {
        val equipo = Equipo(listOf(Jugador("Ana")))
        assertEquals("ana", equipo.id)
        assertFalse(equipo.esPareja)
    }

    @Test
    fun `un equipo tiene uno o dos jugadores`() {
        assertThrows(IllegalArgumentException::class.java) { Equipo(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            Equipo(listOf(Jugador("a"), Jugador("b"), Jugador("c")))
        }
    }

    @Test
    fun `las estadisticas vistas desde el rival invierten ganadas y perdidas`() {
        val estadisticas = Estadisticas(jugadas = 10, ganadas = 7)

        assertEquals(3, estadisticas.perdidas)
        assertEquals(Estadisticas(jugadas = 10, ganadas = 3), estadisticas.desdeElRival())
    }

    @Test
    fun `los jugadores disponibles son el usuario y sus amigos`() {
        val usuario = Usuario(Jugador("Ana"), amigos = listOf(Jugador("Beto"), Jugador("Caro")))

        assertEquals(listOf("Ana", "Beto", "Caro"), usuario.jugadoresDisponibles().map { it.nombre })
        assertTrue(usuario.esAmigoDe(Jugador("BETO")))
        assertFalse(usuario.esAmigoDe(Jugador("Dani")))
    }
}
