package com.example.puntajeburaco20.data.local

import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PartidaGuardadaTest {

    private val json = Json { ignoreUnknownKeys = true }

    // Con ids distintos del nombre, como los perfiles reales.
    private fun jugador(nombre: String) = Jugador(id = "id-de-$nombre", nombre = nombre)

    private fun idaYVuelta(partida: Partida): Partida {
        val texto = json.encodeToString(PartidaGuardada.serializer(), PartidaGuardada.desde(partida))
        return json.decodeFromString(PartidaGuardada.serializer(), texto).aDominio()
    }

    @Test
    fun `una partida en curso se recupera tal cual se guardo`() {
        val partida = Partida.nueva(listOf("Ana", "Beto", "Caro", "Dani").map(::jugador))
            .registrarRonda(PuntajeRonda(100, 35), PuntajeRonda(-50, 10))

        assertEquals(partida, idaYVuelta(partida))
    }

    @Test
    fun `una partida terminada conserva al ganador`() {
        val partida = Partida.nueva(listOf(jugador("Ana"), jugador("Beto"))).finalizar(LadoEquipo.UNO)

        assertEquals(partida, idaYVuelta(partida))
    }

    @Test
    fun `los jugadores se recuperan por su id aunque dos se llamen igual`() {
        val partida = Partida.nueva(listOf(Jugador("p1", "Ana"), Jugador("p2", "Ana")))
            .registrarRonda(PuntajeRonda(10, 0), PuntajeRonda(0, 0))

        val recuperada = idaYVuelta(partida)

        assertEquals(listOf("p1", "p2"), recuperada.ordenDeInicio.map { it.id })
        assertEquals("p2", recuperada.empieza.id)
    }

    @Test
    fun `una partida guardada por una version anterior no se puede leer`() {
        // Las versiones anteriores identificaban a los jugadores solo por su nombre.
        val soloNombres = """
            {"equipoUno":["Ana"],"equipoDos":["Beto"],"empieza":"Ana",
             "rondas":[{"equipoUno":{"base":100,"puntos":30},"equipoDos":{"base":0,"puntos":-20}}]}
        """.trimIndent()
        val soloTotales = """
            {"equipoUno":["Ana"],"equipoDos":["Beto"],"empieza":"Ana",
             "ultimaRondaUno":{"base":100,"puntos":30},"ultimaRondaDos":{"base":0,"puntos":-20},
             "totalUno":530,"totalDos":-20}
        """.trimIndent()

        listOf(soloNombres, soloTotales).forEach { texto ->
            assertThrows(SerializationException::class.java) {
                json.decodeFromString(PartidaGuardada.serializer(), texto)
            }
        }
    }
}
