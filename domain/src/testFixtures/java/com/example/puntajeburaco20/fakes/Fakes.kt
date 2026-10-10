package com.example.puntajeburaco20.fakes

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PartidaJugada
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.domain.repository.SincronizacionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** Jugador de prueba: su id es el nombre en minúsculas, como los perfiles de [FakeUsuarioRepository]. */
fun jugador(nombre: String) = Jugador(id = Usuario.claveDeNombre(nombre), nombre = nombre)

/** Cuenta verificada de prueba para quien se llama [nombre]. */
fun cuentaDe(nombre: String, verificada: Boolean = true): Cuenta {
    val clave = Usuario.claveDeNombre(nombre)
    return Cuenta(uid = "uid-$clave", mail = "$clave@test.com", verificada = verificada)
}

/**
 * Repositorio de perfiles en memoria, con amistades simétricas como el real. El id de cada perfil
 * es su nombre en minúsculas, para que los tests sean fáciles de leer.
 */
class FakeUsuarioRepository : UsuarioRepository {

    private data class Perfil(val nombre: String, val uid: String?, val creadoPor: String?, val amigos: List<String>)

    private val perfiles = MutableStateFlow<Map<String, Perfil>>(emptyMap())

    /** Deja registrado un perfil vinculado a la cuenta de [cuentaDe]. */
    fun registrar(nombre: String, uid: String? = cuentaDe(nombre).uid) {
        perfiles.value += Usuario.claveDeNombre(nombre) to Perfil(nombre, uid, creadoPor = null, amigos = emptyList())
    }

    /** Uid de quien creó el perfil, o `null` si no fue creado para otro. */
    fun creadorDe(id: String): String? = perfiles.value[id]?.creadoPor

    fun tieneLogin(id: String): Boolean = perfiles.value[id]?.uid != null

    override fun observar(id: String): Flow<Usuario?> = perfiles.map { aUsuario(id) }

    override suspend fun obtener(id: String): Usuario? = aUsuario(id)

    override suspend fun buscarPorNombre(nombre: String): Usuario? = aUsuario(Usuario.claveDeNombre(nombre))

    override fun observarIdDeCuenta(uid: String): Flow<String?> =
        perfiles.map { actuales -> actuales.entries.firstOrNull { it.value.uid == uid }?.key }

    override suspend fun crear(cuenta: Cuenta, nombre: String): Usuario {
        val id = idLibre(nombre)
        perfiles.value += id to Perfil(nombre, cuenta.uid, creadoPor = null, amigos = emptyList())
        return aUsuario(id)!!
    }

    override suspend fun crearSinLogin(nombre: String, creador: Cuenta): Usuario {
        val id = idLibre(nombre)
        perfiles.value += id to Perfil(nombre, uid = null, creadoPor = creador.uid, amigos = emptyList())
        return aUsuario(id)!!
    }

    override suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador) {
        modificar(usuario.id) { it + amigo.id }
        modificar(amigo.id) { it + usuario.id }
    }

    override suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador) {
        modificar(usuario.id) { it - amigo.id }
        modificar(amigo.id) { it - usuario.id }
    }

    private fun idLibre(nombre: String): String {
        val id = Usuario.claveDeNombre(nombre)
        if (id in perfiles.value) throw ErrorUsuario.NombreEnUso
        return id
    }

    private fun modificar(id: String, cambio: (List<String>) -> List<String>) {
        val perfil = perfiles.value.getValue(id)
        perfiles.value += id to perfil.copy(amigos = cambio(perfil.amigos))
    }

    private fun aJugador(id: String): Jugador? = perfiles.value[id]?.let { Jugador(id, it.nombre) }

    private fun aUsuario(id: String): Usuario? = perfiles.value[id]?.let { perfil ->
        Usuario(Jugador(id, perfil.nombre), perfil.amigos.mapNotNull(::aJugador))
    }
}

/** Cuentas de acceso en memoria. Los "mails" que envía quedan anotados en las listas públicas. */
class FakeAuthRepository(cuentaInicial: Cuenta? = null) : AuthRepository {

    private data class Registro(val cuenta: Cuenta, val password: String)

    private val registros = mutableMapOf<String, Registro>()
    private val actual = MutableStateFlow(cuentaInicial)

    /** Mails a los que se envió el enlace de verificación, en orden. */
    val verificacionesEnviadas = mutableListOf<String>()

    /** Mails a los que se envió el enlace para cambiar la contraseña, en orden. */
    val recuperacionesEnviadas = mutableListOf<String>()

    /** Si no es `null`, todas las operaciones fallan con este error (por ejemplo, sin conexión). */
    var error: Exception? = null

    override val cuenta: StateFlow<Cuenta?> get() = actual

    /** Deja una cuenta ya creada, sin iniciar sesión con ella. */
    fun registrarCuenta(cuenta: Cuenta, password: String = "clave123") {
        registros[cuenta.mail] = Registro(cuenta, password)
    }

    /** Simula que la persona abrió el enlace del mail. La app se entera recién al recargar. */
    fun verificarMail(mail: String) {
        val registro = registros.getValue(mail)
        registros[mail] = registro.copy(cuenta = registro.cuenta.copy(verificada = true))
    }

    override suspend fun registrar(mail: String, password: String): Cuenta {
        error?.let { throw it }
        if (mail in registros) throw ErrorUsuario.MailEnUso
        val cuenta = Cuenta(uid = "uid-${registros.size + 1}", mail = mail, verificada = false)
        registrarCuenta(cuenta, password)
        actual.value = cuenta
        return cuenta
    }

    override suspend fun iniciarSesion(mail: String, password: String): Cuenta {
        error?.let { throw it }
        val registro = registros[mail]?.takeIf { it.password == password } ?: throw ErrorUsuario.CredencialesIncorrectas
        actual.value = registro.cuenta
        return registro.cuenta
    }

    override suspend fun cerrarSesion() {
        actual.value = null
    }

    override suspend fun enviarVerificacion() {
        error?.let { throw it }
        verificacionesEnviadas += (actual.value ?: throw ErrorUsuario.SinSesion).mail
    }

    override suspend fun recargar(): Cuenta? {
        error?.let { throw it }
        val mail = actual.value?.mail ?: return null
        // Una cuenta puesta directo en el constructor no tiene registro: queda como está.
        registros[mail]?.let { actual.value = it.cuenta }
        return actual.value
    }

    override suspend fun enviarRecuperacion(mail: String) {
        error?.let { throw it }
        recuperacionesEnviadas += mail
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
