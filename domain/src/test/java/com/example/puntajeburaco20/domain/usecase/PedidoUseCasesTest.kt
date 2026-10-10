package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
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

/**
 * Ana le creó un perfil a Dani y lo dejó reservado para dani@test.com, pero Dani se registra con
 * daniel@test.com: tiene que pedir el perfil y esperar a que Ana confirme que es él.
 */
class PedidoUseCasesTest {

    private val usuarios = FakeUsuarioRepository().apply {
        registrar("Ana")
        registrar("Beto")
        registrarAnterior("Viejo", password = "1234")
    }
    private val validador = ValidadorCredenciales()

    private val cuenta = Cuenta(uid = "uid-dani", mail = "daniel@test.com", verificada = true)
    private val auth = FakeAuthRepository(cuenta)
    private val observarSesion = ObservarSesionUseCase(auth, usuarios)
    private val vincularPerfilAnterior =
        VincularPerfilAnteriorUseCase(auth, usuarios, FakeSesionAnteriorRepository(), validador)
    private val consultarPedidoPropio = ConsultarPedidoPropioUseCase(auth, usuarios)
    private val observarPedidoPropio = ObservarPedidoPropioUseCase(auth, usuarios)
    private val consultarReclamo = ConsultarReclamoUseCase(auth, usuarios, FakePartidasJugadasRepository())

    // Del lado de Ana, que creó el perfil.
    private val sesionDeAna = ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Ana")), usuarios)
    private val pedidosRecibidos = ObservarPedidosRecibidosUseCase(usuarios, sesionDeAna)
    private val aceptarPedido = AceptarPedidoUseCase(usuarios, sesionDeAna)
    private val rechazarPedido = RechazarPedidoUseCase(usuarios)

    private val pedido = PedidoDeReclamo(uid = "uid-dani", mail = "daniel@test.com", perfil = jugador("Dani"))

    @Before
    fun anaCreaElPerfilDeDani() = runTest {
        usuarios.crearAmigoSinLogin("Dani", "dani@test.com", creador = cuentaDe("Ana"), amigoDe = jugador("Ana"))
    }

    @Test
    fun `un perfil creado por otro se pide sin contraseña y queda esperando a quien lo creo`() = runTest {
        assertEquals(pedido, vincularPerfilAnterior("DANI", ""))

        assertTrue(observarSesion().first() is EstadoSesion.SinPerfil)
        assertFalse(usuarios.tieneLogin("dani"))
        assertEquals(pedido, consultarPedidoPropio())
        assertEquals(listOf(pedido), pedidosRecibidos().first())
        // Solo lo ve quien creó el perfil.
        val deBeto = ObservarPedidosRecibidosUseCase(usuarios, ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Beto")), usuarios))
        assertEquals(emptyList<PedidoDeReclamo>(), deBeto().first())
    }

    @Test
    fun `si quien lo creo acepta, el perfil queda reservado para el mail del pedido y se puede aceptar`() = runTest {
        vincularPerfilAnterior("Dani", "")

        aceptarPedido(pedido)

        assertEquals("daniel@test.com", usuarios.mailReservadoPara("dani"))
        assertNull(observarPedidoPropio().first())
        assertEquals(emptyList<PedidoDeReclamo>(), pedidosRecibidos().first())
        assertEquals(Reclamo(jugador("Dani"), nombreCreador = "Ana", partidas = 0), consultarReclamo())

        AceptarReclamoUseCase(auth, usuarios)()
        assertEquals(EstadoSesion.Completa(cuenta, jugador("Dani").id), observarSesion().first())
    }

    @Test
    fun `si quien lo creo rechaza, el pedido se va y el perfil sigue reservado para el mail que tenia`() = runTest {
        vincularPerfilAnterior("Dani", "")

        rechazarPedido(pedido)

        assertNull(consultarPedidoPropio())
        assertNull(consultarReclamo())
        assertEquals("dani@test.com", usuarios.mailReservadoPara("dani"))
    }

    @Test
    fun `quien pidio puede desistir y elegir un nombre nuevo`() = runTest {
        vincularPerfilAnterior("Dani", "")

        CancelarPedidoUseCase(auth, usuarios)()

        assertEquals(emptyList<PedidoDeReclamo>(), pedidosRecibidos().first())
        val propio = CrearPerfilUseCase(auth, usuarios, validador)("Daniel")
        assertEquals(EstadoSesion.Completa(cuenta, propio.id), observarSesion().first())
    }

    @Test
    fun `sin contraseña solo se puede pedir un perfil creado por otro que no tenga dueño`() = runTest {
        esperarError<ErrorUsuario.CamposIncompletos> { vincularPerfilAnterior("Viejo", "") }
        esperarError<ErrorUsuario.UsuarioInexistente> { vincularPerfilAnterior("Zoe", "") }
        esperarError<ErrorUsuario.PerfilYaVinculado> { vincularPerfilAnterior("Beto", "") }

        assertNull(consultarPedidoPropio())
    }

    @Test
    fun `quien tiene un perfil reservado sin responder no puede pedir otro`() = runTest {
        usuarios.crearAmigoSinLogin("Dan", "daniel@test.com", creador = cuentaDe("Beto"), amigoDe = jugador("Beto"))

        esperarError<ErrorUsuario.ReclamoPendiente> { vincularPerfilAnterior("Dani", "") }
    }

    @Test
    fun `si otra cuenta ya se quedo con el perfil, el pedido se descarta al querer aceptarlo`() = runTest {
        vincularPerfilAnterior("Dani", "")
        usuarios.aceptarReclamo(Cuenta(uid = "uid-otro", mail = "dani@test.com", verificada = true))

        esperarError<ErrorUsuario.PerfilYaVinculado> { aceptarPedido(pedido) }

        assertEquals(emptyList<PedidoDeReclamo>(), pedidosRecibidos().first())
    }

    @Test
    fun `no se acepta un pedido cuyo mail ya tiene un perfil`() = runTest {
        vincularPerfilAnterior("Dani", "")
        // Mientras esperaba, la misma cuenta se creó un perfil propio sin desistir del pedido.
        usuarios.crear(cuenta, "Daniel")

        esperarError<ErrorUsuario.MailConPerfil> { aceptarPedido(pedido) }

        assertEquals("dani@test.com", usuarios.mailReservadoPara("dani"))
        assertEquals(listOf(pedido), pedidosRecibidos().first())
    }
}
