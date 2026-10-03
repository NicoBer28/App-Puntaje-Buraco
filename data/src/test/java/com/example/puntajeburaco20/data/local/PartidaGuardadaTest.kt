package com.example.puntajeburaco20.data.local

import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.domain.model.Ronda
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PartidaGuardadaTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun idaYVuelta(partida: Partida): Partida {
        val texto = json.encodeToString(PartidaGuardada.serializer(), PartidaGuardada.desde(partida))
        return json.decodeFromString(PartidaGuardada.serializer(), texto).aDominio()
    }

    @Test
    fun `una partida en curso se recupera tal cual se guardo`() {
        val partida = Partida.nueva(listOf("Ana", "Beto", "Caro", "Dani").map(::Jugador))
            .registrarRonda(PuntajeRonda(100, 35), PuntajeRonda(-50, 10))

        assertEquals(partida, idaYVuelta(partida))
    }

    @Test
    fun `una partida terminada conserva al ganador`() {
        val partida = Partida.nueva(listOf(Jugador("Ana"), Jugador("Beto"))).finalizar(LadoEquipo.UNO)

        assertEquals(partida, idaYVuelta(partida))
    }

    @Test
    fun `una partida guardada por la version anterior conserva totales, ultima ronda y turno`() {
        // Formato anterior: sin rondas, solo la última y los totales.
        val texto = """
            {"equipoUno":["Ana"],"equipoDos":["Beto"],"empieza":"Ana",
             "ultimaRondaUno":{"base":100,"puntos":30},"ultimaRondaDos":{"base":0,"puntos":-20},
             "totalUno":530,"totalDos":-20}
        """.trimIndent()

        val partida = json.decodeFromString(PartidaGuardada.serializer(), texto).aDominio()

        assertEquals(530, partida.totalUno)
        assertEquals(-20, partida.totalDos)
        assertEquals(Ronda(PuntajeRonda(100, 30), PuntajeRonda(0, -20)), partida.ultimaRonda)
        assertEquals(Jugador("Ana"), partida.empieza)
        assertNull(partida.ganador)
    }

    @Test
    fun `una partida de la version anterior sin rondas jugadas queda vacia`() {
        val texto = """
            {"equipoUno":["Ana"],"equipoDos":["Beto"],"empieza":"Ana",
             "ultimaRondaUno":{"base":0,"puntos":0},"ultimaRondaDos":{"base":0,"puntos":0},
             "totalUno":0,"totalDos":0}
        """.trimIndent()

        val partida = json.decodeFromString(PartidaGuardada.serializer(), texto).aDominio()

        assertEquals(Partida.nueva(listOf(Jugador("Ana"), Jugador("Beto"))), partida)
    }
}
