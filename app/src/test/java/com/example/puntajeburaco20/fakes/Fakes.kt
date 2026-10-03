package com.example.puntajeburaco20.fakes

import android.graphics.Bitmap
import com.example.puntajeburaco20.data.vision.DetectorFichas
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.FichaDetectada
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.SesionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** Repositorio de usuarios en memoria, con amistades simétricas como el real. */
class FakeUsuarioRepository : UsuarioRepository {

    private data class Cuenta(val nombre: String, val password: String, val amigos: List<String>)

    private val cuentas = MutableStateFlow<Map<String, Cuenta>>(emptyMap())

    fun registrar(nombre: String, password: String = "clave") {
        cuentas.value += Jugador.idDesdeNombre(nombre) to Cuenta(nombre, password, emptyList())
    }

    override fun observar(id: String): Flow<Usuario?> = cuentas.map { aUsuario(id) }

    override suspend fun obtener(id: String): Usuario? = aUsuario(id)

    override suspend fun autenticar(nombre: String, password: String): Usuario {
        val id = Jugador.idDesdeNombre(nombre)
        val cuenta = cuentas.value[id] ?: throw ErrorUsuario.UsuarioInexistente
        if (cuenta.password != password) throw ErrorUsuario.ContrasenaIncorrecta
        return aUsuario(id)!!
    }

    override suspend fun crear(nombre: String, password: String): Usuario {
        registrar(nombre, password)
        return aUsuario(Jugador.idDesdeNombre(nombre))!!
    }

    override suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador) {
        modificar(usuario.id) { it + amigo.nombre }
        modificar(amigo.id) { it + usuario.nombre }
    }

    override suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador) {
        modificar(usuario.id) { it - amigo.nombre }
        modificar(amigo.id) { it - usuario.nombre }
    }

    private fun modificar(id: String, cambio: (List<String>) -> List<String>) {
        val cuenta = cuentas.value.getValue(id)
        cuentas.value += id to cuenta.copy(amigos = cambio(cuenta.amigos))
    }

    private fun aUsuario(id: String): Usuario? = cuentas.value[id]?.let { cuenta ->
        Usuario(Jugador(cuenta.nombre), cuenta.amigos.map(::Jugador))
    }
}

class FakeSesionRepository(usuarioInicial: String? = null) : SesionRepository {
    override val usuarioActualId: StateFlow<String?> get() = actual
    private val actual = MutableStateFlow(usuarioInicial)

    override fun iniciar(usuarioId: String) {
        actual.value = usuarioId
    }
}

class FakePartidaEnCursoRepository(var partida: Partida? = null) : PartidaEnCursoRepository {
    override suspend fun obtener(): Partida? = partida
    override suspend fun guardar(partida: Partida) {
        this.partida = partida
    }
    override suspend fun eliminar() {
        partida = null
    }
}

class FakeEstadisticasRepository : EstadisticasRepository {

    val resultados = mutableListOf<Pair<Equipo, Equipo>>()
    val generales = mutableMapOf<String, Estadisticas>()
    val enfrentamientos = mutableMapOf<Pair<String, String>, Estadisticas>()
    var error: Exception? = null

    override suspend fun registrarResultado(ganador: Equipo, perdedor: Equipo) {
        error?.let { throw it }
        resultados += ganador to perdedor
    }

    override suspend fun obtenerGenerales(equipo: Equipo): Estadisticas? {
        error?.let { throw it }
        return generales[equipo.id]
    }

    override suspend fun obtenerEnfrentamiento(equipo: Equipo, rival: Equipo): Estadisticas? {
        error?.let { throw it }
        return enfrentamientos[equipo.id to rival.id]
    }
}

class FakeDetectorFichas(var disponible: Boolean = true) : DetectorFichas {
    var fichas: List<FichaDetectada> = emptyList()

    override fun preparar() {
        if (!disponible) throw java.io.IOException("Modelo no encontrado")
    }

    override fun detectar(imagen: Bitmap): List<FichaDetectada> = fichas
}
