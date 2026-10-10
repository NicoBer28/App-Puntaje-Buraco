package com.example.puntajeburaco20.fakes

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PartidaJugada
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.model.Reclamo
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.domain.repository.SesionAnteriorRepository
import com.example.puntajeburaco20.domain.repository.SincronizacionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    private data class Perfil(
        val nombre: String,
        val uid: String?,
        val creadoPor: String?,
        val amigos: List<String>,
        val passwordAnterior: String? = null,
    )

    /** Perfil reservado para un mail: a quién se le ofrece, quién lo creó y con qué nombre. */
    private data class Reserva(val idPerfil: String, val creadoPor: String, val nombreCreador: String)

    private val perfiles = MutableStateFlow<Map<String, Perfil>>(emptyMap())

    /** Reclamos por mail. */
    private val reservas = MutableStateFlow<Map<String, Reserva>>(emptyMap())

    /** Pedidos sin resolver, por el uid de quien los hizo. */
    private val pedidos = MutableStateFlow<Map<String, PedidoDeReclamo>>(emptyMap())

    /** Mails que ya son de una cuenta con perfil. */
    private val mailsConCuenta = mutableSetOf<String>()

    /** Deja registrado un perfil vinculado a la cuenta de [cuentaDe]. */
    fun registrar(nombre: String, uid: String? = cuentaDe(nombre).uid) {
        perfiles.value += Usuario.claveDeNombre(nombre) to Perfil(nombre, uid, creadoPor = null, amigos = emptyList())
        if (uid != null) mailsConCuenta += cuentaDe(nombre).mail
    }

    /** Mail para el que está reservado el perfil, o `null` si no lo está para ninguno. */
    fun mailReservadoPara(id: String): String? = reservas.value.entries.firstOrNull { it.value.idPerfil == id }?.key

    /** Deja un perfil anterior a las cuentas con mail: sin cuenta, con la contraseña que tenía. */
    fun registrarAnterior(nombre: String, password: String) {
        perfiles.value += Usuario.claveDeNombre(nombre) to
            Perfil(nombre, uid = null, creadoPor = null, amigos = emptyList(), passwordAnterior = password)
    }

    /** Uid de quien creó el perfil, o `null` si no fue creado para otro. */
    fun creadorDe(id: String): String? = perfiles.value[id]?.creadoPor

    fun tieneLogin(id: String): Boolean = perfiles.value[id]?.uid != null

    override fun observar(id: String): Flow<Usuario?> = perfiles.map { aUsuario(id) }

    override suspend fun obtener(id: String): Usuario? = aUsuario(id)

    override suspend fun buscarPorNombre(nombre: String): Jugador? = aJugador(Usuario.claveDeNombre(nombre))

    override fun observarIdDeCuenta(uid: String): Flow<String?> =
        perfiles.map { actuales -> actuales.entries.firstOrNull { it.value.uid == uid }?.key }

    override suspend fun crear(cuenta: Cuenta, nombre: String): Usuario {
        if (cuenta.mail in reservas.value) throw ErrorUsuario.ReclamoPendiente
        val id = idLibre(nombre)
        perfiles.value += id to Perfil(nombre, cuenta.uid, creadoPor = null, amigos = emptyList())
        mailsConCuenta += cuenta.mail
        return aUsuario(id)!!
    }

    override suspend fun vincularAnterior(cuenta: Cuenta, nombre: String, passwordAnterior: String): Jugador {
        if (cuenta.mail in reservas.value) throw ErrorUsuario.ReclamoPendiente
        val id = Usuario.claveDeNombre(nombre)
        val perfil = perfiles.value[id] ?: throw ErrorUsuario.UsuarioInexistente
        when {
            perfil.uid != null -> throw ErrorUsuario.PerfilYaVinculado
            perfil.creadoPor != null -> throw ErrorUsuario.PerfilCreadoPorOtro
            perfil.passwordAnterior != passwordAnterior -> throw ErrorUsuario.ContrasenaAnteriorIncorrecta
        }
        perfiles.value += id to perfil.copy(uid = cuenta.uid)
        mailsConCuenta += cuenta.mail
        return Jugador(id, perfil.nombre)
    }

    override suspend fun crearAmigoSinLogin(nombre: String, mail: String, creador: Cuenta, amigoDe: Jugador): Jugador {
        exigirMailLibre(mail)
        val id = idLibre(nombre)
        perfiles.value += id to Perfil(nombre, uid = null, creadoPor = creador.uid, amigos = emptyList())
        val nuevo = aJugador(id)!!
        agregarAmistad(amigoDe, nuevo)
        reservas.value += mail to Reserva(id, creador.uid, amigoDe.nombre)
        return nuevo
    }

    /** Como [crearAmigoSinLogin] antes de que se pidiera el mail: el perfil no queda reservado para nadie. */
    suspend fun crearAmigoSinMail(nombre: String, creador: Cuenta, amigoDe: Jugador) {
        val id = idLibre(nombre)
        perfiles.value += id to Perfil(nombre, uid = null, creadoPor = creador.uid, amigos = emptyList())
        agregarAmistad(amigoDe, aJugador(id)!!)
    }

    override fun observarPerfilesACargo(creador: Cuenta): Flow<List<PerfilACargo>> =
        combine(perfiles, reservas) { actuales, _ ->
            actuales
                .filterValues { it.creadoPor == creador.uid && it.uid == null }
                .map { (id, perfil) -> PerfilACargo(Jugador(id, perfil.nombre), mailReservadoPara(id)) }
        }

    override suspend fun reservarPara(perfil: PerfilACargo, mail: String, creador: Cuenta, nombreCreador: String) {
        exigirMailLibre(mail)
        reservas.value = reservas.value - listOfNotNull(perfil.mail).toSet() +
            (mail to Reserva(perfil.jugador.id, creador.uid, nombreCreador))
    }

    override suspend fun buscarReclamo(cuenta: Cuenta): Reclamo? =
        reservas.value[cuenta.mail]?.let { Reclamo(aJugador(it.idPerfil)!!, it.nombreCreador) }

    override suspend fun aceptarReclamo(cuenta: Cuenta): Jugador {
        val reserva = reservas.value[cuenta.mail] ?: throw ErrorUsuario.ReclamoNoDisponible
        perfiles.value += reserva.idPerfil to perfiles.value.getValue(reserva.idPerfil).copy(uid = cuenta.uid)
        reservas.value -= cuenta.mail
        mailsConCuenta += cuenta.mail
        return aJugador(reserva.idPerfil)!!
    }

    override suspend fun rechazarReclamo(cuenta: Cuenta) {
        reservas.value -= cuenta.mail
    }

    override suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador) {
        modificar(usuario.id) { it + amigo.id }
        modificar(amigo.id) { it + usuario.id }
    }

    override suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador) {
        modificar(usuario.id) { it - amigo.id }
        modificar(amigo.id) { it - usuario.id }
    }

    override suspend fun pedirPerfil(cuenta: Cuenta, nombre: String): PedidoDeReclamo {
        if (cuenta.mail in reservas.value) throw ErrorUsuario.ReclamoPendiente
        val id = Usuario.claveDeNombre(nombre)
        val perfil = perfiles.value[id] ?: throw ErrorUsuario.UsuarioInexistente
        if (perfil.uid != null) throw ErrorUsuario.PerfilYaVinculado
        if (perfil.creadoPor == null) throw ErrorUsuario.UsuarioInexistente
        val pedido = PedidoDeReclamo(cuenta.uid, cuenta.mail, Jugador(id, perfil.nombre))
        pedidos.value += cuenta.uid to pedido
        return pedido
    }

    override suspend fun buscarPedidoPropio(cuenta: Cuenta): PedidoDeReclamo? = pedidos.value[cuenta.uid]

    override fun observarPedidoPropio(cuenta: Cuenta): Flow<PedidoDeReclamo?> = pedidos.map { it[cuenta.uid] }

    override suspend fun cancelarPedido(cuenta: Cuenta) {
        pedidos.value -= cuenta.uid
    }

    override fun observarPedidosRecibidos(creador: Cuenta): Flow<List<PedidoDeReclamo>> =
        pedidos.map { actuales -> actuales.values.filter { creadorDe(it.perfil.id) == creador.uid } }

    override suspend fun aceptarPedido(pedido: PedidoDeReclamo, creador: Cuenta, nombreCreador: String) {
        val id = pedido.perfil.id
        if (tieneLogin(id)) {
            pedidos.value -= pedido.uid
            throw ErrorUsuario.PerfilYaVinculado
        }
        if (mailReservadoPara(id) != pedido.mail) {
            reservarPara(PerfilACargo(pedido.perfil, mailReservadoPara(id)), pedido.mail, creador, nombreCreador)
        }
        pedidos.value -= pedido.uid
    }

    override suspend fun rechazarPedido(pedido: PedidoDeReclamo) {
        pedidos.value -= pedido.uid
    }

    private fun exigirMailLibre(mail: String) {
        if (mail in mailsConCuenta || mail in reservas.value) throw ErrorUsuario.MailConPerfil
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

/**
 * Estadísticas en memoria. Como el repositorio real, suma lo cargado en [generales] y
 * [enfrentamientos] (las estadísticas previas) con lo que surge de las partidas guardadas.
 */
class FakeEstadisticasRepository(
    private val partidasJugadas: FakePartidasJugadasRepository = FakePartidasJugadasRepository(),
) : EstadisticasRepository {

    val generales = mutableMapOf<String, Estadisticas>()
    val enfrentamientos = mutableMapOf<Pair<String, String>, Estadisticas>()
    var error: Exception? = null

    override suspend fun obtenerGenerales(equipo: Equipo): Estadisticas? {
        error?.let { throw it }
        return sumar(generales[equipo.id], delHistorial(equipo, rival = null))
    }

    override suspend fun obtenerEnfrentamiento(equipo: Equipo, rival: Equipo): Estadisticas? {
        error?.let { throw it }
        return sumar(enfrentamientos[equipo.id to rival.id], delHistorial(equipo, rival))
    }

    private fun sumar(previas: Estadisticas?, historial: Estadisticas): Estadisticas? =
        ((previas ?: Estadisticas.VACIAS) + historial).takeIf { it.jugadas > 0 }

    private fun delHistorial(equipo: Equipo, rival: Equipo?): Estadisticas {
        val lados = partidasJugadas.guardadas.mapNotNull { jugada ->
            val partida = jugada.partida
            val lado = LadoEquipo.entries.firstOrNull { partida.equipo(it).id == equipo.id }
            lado?.takeIf { rival == null || partida.equipo(it.rival).id == rival.id }?.let { it to partida.ganador }
        }
        return Estadisticas(
            jugadas = lados.size.toLong(),
            ganadas = lados.count { (lado, ganador) -> lado == ganador }.toLong(),
        )
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

    override suspend fun contarDe(jugador: Jugador): Long {
        error?.let { throw it }
        return guardadas.count { it.partida.ladoDe(jugador) != null }.toLong()
    }

    override suspend fun obtenerDe(jugador: Jugador, limite: Int): List<PartidaJugada> {
        error?.let { throw it }
        return guardadas
            .filter { it.partida.ladoDe(jugador) != null }
            .sortedByDescending { it.fecha }
            .take(limite)
    }
}

/** [nombre] es el usuario con el que el dispositivo entraba antes de las cuentas con mail. */
class FakeSesionAnteriorRepository(var nombre: String? = null) : SesionAnteriorRepository {
    override suspend fun nombreDeUsuario(): String? = nombre

    override suspend fun olvidar() {
        nombre = null
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
