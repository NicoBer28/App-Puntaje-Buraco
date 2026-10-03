package com.example.puntajeburaco20.data.local

import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
}
