package com.example.puntajeburaco20.fakes

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PartidaJugada
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.domain.repository.SesionRepository
import com.example.puntajeburaco20.domain.repository.SincronizacionRepository
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
        if (Jugador.idDesdeNombre(nombre) in cuentas.value) throw ErrorUsuario.NombreEnUso
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
    private val actual = MutableStateFlow(usuarioInicial)
    override val usuarioActualId: StateFlow<String?> get() = actual

    override suspend fun iniciar(usuarioId: String) {
        actual.value = usuarioId
    }

    override suspend fun cerrar() {
        actual.value = null
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

/** Guarda las partidas en memoria; la fecha es la cantidad de partidas guardadas hasta el momento. */
class FakePartidasJugadasRepository : PartidasJugadasRepository {

    val guardadas = mutableListOf<PartidaJugada>()
    var error: Exception? = null

    override suspend fun guardar(partida: Partida) {
        error?.let { throw it }
        guardadas += PartidaJugada(partida, fecha = guardadas.size.toLong())
    }

    override suspend fun obtenerDe(jugador: Jugador, limite: Int): List<PartidaJugada> {
        error?.let { throw it }
        return guardadas
            .filter { it.partida.ladoDe(jugador) != null }
            .sortedByDescending { it.fecha }
            .take(limite)
    }
}

class FakeSincronizacionRepository(pendientes: Boolean = false) : SincronizacionRepository {
    override val hayCambiosPendientes = MutableStateFlow(pendientes)
}

class FakePreferenciasRepository(modoInicial: ModoTema = ModoTema.SISTEMA) : PreferenciasRepository {
    override val modoTema = MutableStateFlow(modoInicial)

    override suspend fun cambiarModoTema(modo: ModoTema) {
        modoTema.value = modo
    }
}
