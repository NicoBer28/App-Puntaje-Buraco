package com.example.puntajeburaco20.domain.model

import com.example.puntajeburaco20.fakes.jugador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelosTest {

    @Test
    fun `dos jugadores con el mismo id son la misma persona aunque cambie el nombre`() {
        assertTrue(Jugador("p1", "JuanPe").esMismaPersona(Jugador("p1", "Juan Pedro")))
        assertFalse(Jugador("p1", "JuanPe").esMismaPersona(Jugador("p2", "JuanPe")))
    }

    @Test
    fun `un nombre de usuario se reserva sin distinguir mayusculas`() {
        assertEquals("juanpe", Usuario.claveDeNombre("JuanPe"))
        assertEquals(Usuario.claveDeNombre("ANA"), Usuario.claveDeNombre("ana"))
    }

    @Test
    fun `el id de una pareja no depende del orden de sus integrantes`() {
        val pareja = Equipo(listOf(jugador("Zoe"), jugador("ana")))

        assertEquals("ana|zoe", pareja.id)
        assertEquals(pareja.id, Equipo(listOf(jugador("Ana"), jugador("zoe"))).id)
        assertTrue(pareja.esPareja)
    }

    @Test
    fun `dos parejas distintas nunca comparten id`() {
        val unaPareja = Equipo(listOf(jugador("abcd"), jugador("efg")))
        val otraPareja = Equipo(listOf(jugador("abc"), jugador("defg")))

        assertNotEquals(unaPareja.id, otraPareja.id)
    }

    @Test
    fun `el id de un jugador solo es el de su perfil`() {
        val equipo = Equipo(listOf(jugador("Ana")))
        assertEquals("ana", equipo.id)
        assertFalse(equipo.esPareja)
    }

    @Test
    fun `un equipo tiene uno o dos jugadores`() {
        assertThrows(IllegalArgumentException::class.java) { Equipo(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            Equipo(listOf(jugador("a"), jugador("b"), jugador("c")))
        }
    }

    @Test
    fun `las estadisticas vistas desde el rival invierten ganadas y perdidas`() {
        val estadisticas = Estadisticas(jugadas = 10, ganadas = 7)

        assertEquals(3, estadisticas.perdidas)
        assertEquals(Estadisticas(jugadas = 10, ganadas = 3), estadisticas.desdeElRival())
    }

    @Test
    fun `las estadisticas se pueden sumar`() {
        assertEquals(Estadisticas(jugadas = 5, ganadas = 3), Estadisticas(2, 1) + Estadisticas(3, 2))
    }

    @Test
    fun `los jugadores disponibles son el usuario y sus amigos`() {
        val usuario = Usuario(jugador("Ana"), amigos = listOf(jugador("Beto"), jugador("Caro")))

        assertEquals(listOf("Ana", "Beto", "Caro"), usuario.jugadoresDisponibles().map { it.nombre })
        assertTrue(usuario.esAmigoDe(jugador("BETO")))
        assertFalse(usuario.esAmigoDe(jugador("Dani")))
    }
}
