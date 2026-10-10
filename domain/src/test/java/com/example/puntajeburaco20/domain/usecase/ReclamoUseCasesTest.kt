package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.model.Reclamo
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakePartidasJugadasRepository
import com.example.puntajeburaco20.fakes.FakeSesionAnteriorRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Ana le creó un perfil a Dani, que no usaba la app, y lo dejó reservado para su mail. */
class ReclamoUseCasesTest {

    private val usuarios = FakeUsuarioRepository().apply { registrar("Ana") }
    private val partidasJugadas = FakePartidasJugadasRepository()
    private val validador = ValidadorCredenciales()

    // La cuenta con la que Dani se registra después.
    private val cuenta = Cuenta(uid = "uid-dani", mail = "dani@test.com", verificada = true)
    private val auth = FakeAuthRepository(cuenta)
    private val observarSesion = ObservarSesionUseCase(auth, usuarios)
    private val consultarReclamo = ConsultarReclamoUseCase(auth, usuarios, partidasJugadas)
    private val aceptarReclamo = AceptarReclamoUseCase(auth, usuarios)
    private val rechazarReclamo = RechazarReclamoUseCase(auth, usuarios)
    private val crearPerfil = CrearPerfilUseCase(auth, usuarios, validador)

    private val perfilesACargoDeAna =
        ObservarPerfilesACargoUseCase(usuarios, ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Ana")), usuarios))

    @Before
    fun anaCreaElPerfilDeDani() = runTest {
        usuarios.crearAmigoSinLogin("Dani", "dani@test.com", creador = cuentaDe("Ana"), amigoDe = jugador("Ana"))
    }

    private suspend fun jugarUnaPartida() {
        val terminada = Partida.nueva(listOf(jugador("Ana"), jugador("Dani"))).copy(ganador = LadoEquipo.UNO)
        partidasJugadas.guardar(terminada)
    }

    @Test
    fun `a quien se registra con ese mail se le ofrece el perfil, con quien lo creo y sus partidas`() = runTest {
        jugarUnaPartida()

        assertEquals(Reclamo(jugador("Dani"), nombreCreador = "Ana", partidas = 1), consultarReclamo())
    }

    @Test
    fun `si no se pueden contar las partidas el perfil se ofrece igual`() = runTest {
        partidasJugadas.error = ErrorUsuario.SinConexion

        assertEquals(Reclamo(jugador("Dani"), nombreCreador = "Ana", partidas = null), consultarReclamo())
    }

    @Test
    fun `con otro mail no se ofrece nada`() = runTest {
        val otra = FakeAuthRepository(Cuenta(uid = "uid-eva", mail = "eva@test.com", verificada = true))

        assertNull(ConsultarReclamoUseCase(otra, usuarios, partidasJugadas)())
    }

    @Test
    fun `aceptar vincula el perfil a la cuenta, con sus amigos, y deja de estar a cargo de quien lo creo`() = runTest {
        val vinculado = aceptarReclamo()

        assertEquals(jugador("Dani"), vinculado)
        assertEquals(EstadoSesion.Completa(cuenta, vinculado.id), observarSesion().first())
        assertEquals(listOf(jugador("Ana")), usuarios.obtener(vinculado.id)?.amigos)
        assertTrue(usuarios.tieneLogin("dani"))
        assertEquals(emptyList<PerfilACargo>(), perfilesACargoDeAna().first())
        assertNull(consultarReclamo())
    }

    @Test
    fun `rechazar deja el perfil a cargo de quien lo creo y permite elegir un nombre nuevo`() = runTest {
        rechazarReclamo()

        assertNull(consultarReclamo())
        assertFalse(usuarios.tieneLogin("dani"))
        assertEquals(listOf(PerfilACargo(jugador("Dani"), mail = null)), perfilesACargoDeAna().first())

        val propio = crearPerfil("Daniel")
        assertEquals(EstadoSesion.Completa(cuenta, propio.id), observarSesion().first())
    }

    @Test
    fun `mientras no responda no puede crear otro perfil ni recuperar uno anterior`() = runTest {
        usuarios.registrarAnterior("Viejo", password = "1234")
        val vincularPerfilAnterior =
            VincularPerfilAnteriorUseCase(auth, usuarios, FakeSesionAnteriorRepository(), validador)

        esperarError<ErrorUsuario.ReclamoPendiente> { crearPerfil("Daniel") }
        esperarError<ErrorUsuario.ReclamoPendiente> { vincularPerfilAnterior("Viejo", "1234") }

        assertTrue(observarSesion().first() is EstadoSesion.SinPerfil)
    }

    @Test
    fun `si quien lo creo cambio el mail, ya no se puede aceptar`() = runTest {
        ReservarPerfilACargoUseCase(usuarios, ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Ana")), usuarios), validador)(
            PerfilACargo(jugador("Dani"), "dani@test.com"),
            "daniel@test.com",
        )

        esperarError<ErrorUsuario.ReclamoNoDisponible> { aceptarReclamo() }
        assertNull(consultarReclamo())
    }

    @Test
    fun `aceptar o rechazar exige una cuenta con el mail verificado`() = runTest {
        val sinSesion = FakeAuthRepository()
        esperarError<ErrorUsuario.SinSesion> { AceptarReclamoUseCase(sinSesion, usuarios)() }
        esperarError<ErrorUsuario.SinSesion> { RechazarReclamoUseCase(sinSesion, usuarios)() }
        esperarError<ErrorUsuario.SinSesion> { ConsultarReclamoUseCase(sinSesion, usuarios, partidasJugadas)() }

        val sinVerificar = FakeAuthRepository(cuenta.copy(verificada = false))
        esperarError<ErrorUsuario.MailSinVerificar> { AceptarReclamoUseCase(sinVerificar, usuarios)() }
        esperarError<ErrorUsuario.MailSinVerificar> { RechazarReclamoUseCase(sinVerificar, usuarios)() }
        esperarError<ErrorUsuario.MailSinVerificar> { ConsultarReclamoUseCase(sinVerificar, usuarios, partidasJugadas)() }
    }
}
