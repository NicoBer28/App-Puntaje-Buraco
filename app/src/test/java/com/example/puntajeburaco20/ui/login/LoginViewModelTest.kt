package com.example.puntajeburaco20.ui.login

import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.model.Reclamo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.domain.usecase.AceptarReclamoUseCase
import com.example.puntajeburaco20.domain.usecase.CancelarPedidoUseCase
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ConsultarPedidoPropioUseCase
import com.example.puntajeburaco20.domain.usecase.ConsultarReclamoUseCase
import com.example.puntajeburaco20.domain.usecase.CrearPerfilUseCase
import com.example.puntajeburaco20.domain.usecase.IniciarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarPedidoPropioUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.RecuperarContrasenaUseCase
import com.example.puntajeburaco20.domain.usecase.RechazarReclamoUseCase
import com.example.puntajeburaco20.domain.usecase.RegistrarCuentaUseCase
import com.example.puntajeburaco20.domain.usecase.VincularPerfilAnteriorUseCase
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakePartidasJugadasRepository
import com.example.puntajeburaco20.fakes.FakeSesionAnteriorRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.login.LoginViewModel.EstadoReclamo
import com.example.puntajeburaco20.ui.login.LoginViewModel.Evento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val usuarios = FakeUsuarioRepository()
    private val auth = FakeAuthRepository()
    private val partidaEnCurso = FakePartidaEnCursoRepository()
    private val sesionAnterior = FakeSesionAnteriorRepository()
    private val validador = ValidadorCredenciales()
    private val observarSesion = ObservarSesionUseCase(auth, usuarios)

    // Se crea recién al usarlo, para que cada test pueda preparar antes la sesión anterior.
    private val viewModel by lazy {
        LoginViewModel(
            iniciarSesion = IniciarSesionUseCase(auth),
            registrarCuenta = RegistrarCuentaUseCase(auth, validador),
            recuperarContrasenaUseCase = RecuperarContrasenaUseCase(auth, validador),
            crearPerfil = CrearPerfilUseCase(auth, usuarios, validador),
            vincularPerfilAnterior = VincularPerfilAnteriorUseCase(auth, usuarios, sesionAnterior, validador),
            consultarReclamo = ConsultarReclamoUseCase(auth, usuarios, FakePartidasJugadasRepository()),
            aceptarReclamoUseCase = AceptarReclamoUseCase(auth, usuarios),
            rechazarReclamoUseCase = RechazarReclamoUseCase(auth, usuarios),
            consultarPedidoPropio = ConsultarPedidoPropioUseCase(auth, usuarios),
            observarPedidoPropio = ObservarPedidoPropioUseCase(auth, usuarios),
            cancelarPedidoUseCase = CancelarPedidoUseCase(auth, usuarios),
            cerrarSesion = CerrarSesionUseCase(auth, partidaEnCurso),
            auth = auth,
            sesionAnterior = sesionAnterior,
        )
    }

    private fun TestScope.eventos(): MutableList<Evento> {
        val eventos = mutableListOf<Evento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.toList(eventos) }
        return eventos
    }

    /** Deja una cuenta recién creada, y devuelve solo los eventos que lleguen de ahí en más. */
    private fun TestScope.eventosTrasRegistrar(verificada: Boolean = false): MutableList<Evento> {
        val eventos = eventos()
        viewModel.registrarse("ana@test.com", "clave123")
        if (verificada) {
            auth.verificarMail("ana@test.com")
            viewModel.comprobarVerificacion()
        }
        eventos.clear()
        return eventos
    }

    private fun mensaje(id: Int) = Evento.Mensaje(UiText.de(id))

    @Test
    fun `crear una cuenta lleva a verificar el mail y avisa que se envio`() = runTest {
        val eventos = eventos()

        viewModel.registrarse("ana@test.com", "clave123")

        assertTrue(observarSesion().first() is EstadoSesion.SinVerificar)
        assertEquals(listOf("ana@test.com"), auth.verificacionesEnviadas)
        assertEquals(listOf(mensaje(R.string.mensaje_verificacion_enviada)), eventos)
        assertFalse(viewModel.cargando.value)
    }

    @Test
    fun `crear una cuenta con datos invalidos muestra el motivo y no la crea`() = runTest {
        val eventos = eventos()

        viewModel.registrarse("ana", "clave123")
        viewModel.registrarse("ana@test.com", "123")

        assertEquals(
            listOf(mensaje(R.string.error_mail_invalido), Evento.Mensaje(UiText.Plural(R.plurals.error_contrasena_corta, 6))),
            eventos,
        )
        assertNull(auth.cuenta.value)
    }

    @Test
    fun `ingresar con una cuenta que ya tiene perfil deja la sesion completa`() = runTest {
        usuarios.registrar("Ana")
        auth.registrarCuenta(cuentaDe("Ana"), password = "clave123")
        val eventos = eventos()

        viewModel.ingresar("ana@test.com", "clave123")

        assertEquals(EstadoSesion.Completa(cuentaDe("Ana"), "ana"), observarSesion().first())
        assertTrue(eventos.isEmpty())
    }

    @Test
    fun `ingresar con credenciales incorrectas lo informa`() = runTest {
        auth.registrarCuenta(cuentaDe("Ana"), password = "clave123")
        val eventos = eventos()

        viewModel.ingresar("ana@test.com", "otraclave")

        assertEquals(listOf(mensaje(R.string.error_credenciales_incorrectas)), eventos)
        assertEquals(EstadoSesion.SinSesion, observarSesion().first())
    }

    @Test
    fun `comprobar la verificacion avanza cuando el mail ya fue abierto`() = runTest {
        val eventos = eventosTrasRegistrar()

        viewModel.comprobarVerificacion()
        assertEquals(listOf(mensaje(R.string.error_mail_sin_verificar)), eventos)

        auth.verificarMail("ana@test.com")
        viewModel.comprobarVerificacion()

        assertTrue(observarSesion().first() is EstadoSesion.SinPerfil)
        assertEquals(1, eventos.size)
    }

    @Test
    fun `la comprobacion automatica no muestra nada si falta verificar o falla`() = runTest {
        val eventos = eventosTrasRegistrar()

        viewModel.comprobarVerificacion(avisar = false)
        auth.error = ErrorUsuario.SinConexion
        viewModel.comprobarVerificacion(avisar = false)

        assertTrue(eventos.isEmpty())
        assertTrue(observarSesion().first() is EstadoSesion.SinVerificar)
    }

    @Test
    fun `reenviar la verificacion manda otro mail y lo avisa`() = runTest {
        val eventos = eventosTrasRegistrar()

        viewModel.reenviarVerificacion()

        assertEquals(listOf("ana@test.com", "ana@test.com"), auth.verificacionesEnviadas)
        assertEquals(listOf(mensaje(R.string.mensaje_verificacion_enviada)), eventos)
    }

    @Test
    fun `elegir un nombre crea el perfil y completa la sesion`() = runTest {
        viewModel.registrarse("ana@test.com", "clave123")
        auth.verificarMail("ana@test.com")
        viewModel.comprobarVerificacion()

        viewModel.elegirNombre("Ana")

        val sesion = observarSesion().first() as EstadoSesion.Completa
        assertEquals(jugador("Ana"), usuarios.obtener(sesion.idPerfil)?.jugador)
    }

    @Test
    fun `elegir un nombre ocupado o invalido muestra el motivo`() = runTest {
        usuarios.registrar("Beto")
        val eventos = eventosTrasRegistrar(verificada = true)

        viewModel.elegirNombre("beto")
        viewModel.elegirNombre("Ana María")

        assertEquals(
            listOf(mensaje(R.string.error_nombre_en_uso), Evento.Mensaje(UiText.Plural(R.plurals.error_longitud_maxima, 8))),
            eventos,
        )
        assertTrue(observarSesion().first() is EstadoSesion.SinPerfil)
    }

    @Test
    fun `quien ya tenia un perfil lo vincula con su contraseña anterior`() = runTest {
        usuarios.registrarAnterior("Ana", password = "1234")
        sesionAnterior.nombre = "ana"
        val eventos = eventosTrasRegistrar(verificada = true)
        assertEquals("ana", viewModel.usuarioAnterior.value)

        viewModel.vincularPerfil("ana", "1234")

        val sesion = observarSesion().first() as EstadoSesion.Completa
        assertEquals(jugador("Ana").id, sesion.idPerfil)
        assertTrue(eventos.isEmpty())
        // El dispositivo ya no recuerda al usuario anterior.
        assertNull(viewModel.usuarioAnterior.value)
        assertNull(sesionAnterior.nombre)
    }

    @Test
    fun `vincular con otra contraseña o un perfil que no sirve muestra el motivo`() = runTest {
        usuarios.registrarAnterior("Ana", password = "1234")
        usuarios.registrar("Beto")
        sesionAnterior.nombre = "ana"
        val eventos = eventosTrasRegistrar(verificada = true)

        viewModel.vincularPerfil("Ana", "4321")
        viewModel.vincularPerfil("Beto", "1234")
        viewModel.vincularPerfil("Zoe", "1234")
        viewModel.vincularPerfil("Ana", "")

        assertEquals(
            listOf(
                mensaje(R.string.error_contrasena_anterior_incorrecta),
                mensaje(R.string.error_perfil_ya_vinculado),
                mensaje(R.string.error_usuario_inexistente),
                mensaje(R.string.error_complete_campos),
            ),
            eventos,
        )
        assertTrue(observarSesion().first() is EstadoSesion.SinPerfil)
        assertEquals("ana", viewModel.usuarioAnterior.value)
    }

    /** Beto le creó un perfil a Ana y lo dejó reservado para el mail con el que ella se registra. */
    private suspend fun betoLeCreaUnPerfilAAna() {
        usuarios.registrar("Beto")
        usuarios.crearAmigoSinLogin("Ana", "ana@test.com", creador = cuentaDe("Beto"), amigoDe = jugador("Beto"))
    }

    private fun prepararEleccionDePerfil() = viewModel.prepararEleccionDePerfil(auth.cuenta.value!!)

    @Test
    fun `a quien le crearon un perfil se le ofrece y al aceptarlo completa la sesion`() = runTest {
        betoLeCreaUnPerfilAAna()
        val eventos = eventosTrasRegistrar(verificada = true)

        prepararEleccionDePerfil()
        assertEquals(
            EstadoReclamo.Pendiente(Reclamo(jugador("Ana"), nombreCreador = "Beto", partidas = 0)),
            viewModel.reclamo.value,
        )

        viewModel.aceptarReclamo()

        val sesion = observarSesion().first() as EstadoSesion.Completa
        assertEquals(jugador("Ana").id, sesion.idPerfil)
        assertTrue(usuarios.tieneLogin("ana"))
        assertTrue(eventos.isEmpty())
    }

    @Test
    fun `si rechaza el perfil que le crearon pasa a elegir un nombre nuevo`() = runTest {
        betoLeCreaUnPerfilAAna()
        eventosTrasRegistrar(verificada = true)
        prepararEleccionDePerfil()

        viewModel.rechazarReclamo()
        assertEquals(EstadoReclamo.Ninguno, viewModel.reclamo.value)
        viewModel.elegirNombre("Anita")

        val sesion = observarSesion().first() as EstadoSesion.Completa
        assertEquals(jugador("Anita"), usuarios.obtener(sesion.idPerfil)?.jugador)
        assertFalse(usuarios.tieneLogin("ana"))
    }

    @Test
    fun `sin un perfil reservado se pasa directo a elegir nombre`() = runTest {
        eventosTrasRegistrar(verificada = true)
        assertEquals(EstadoReclamo.Buscando, viewModel.reclamo.value)

        prepararEleccionDePerfil()

        assertEquals(EstadoReclamo.Ninguno, viewModel.reclamo.value)
    }

    @Test
    fun `si le reservan un perfil despues de la busqueda, se le ofrece al querer crear otro`() = runTest {
        val eventos = eventosTrasRegistrar(verificada = true)
        prepararEleccionDePerfil()
        betoLeCreaUnPerfilAAna()
        // Para la misma cuenta no se vuelve a buscar solo.
        prepararEleccionDePerfil()
        assertEquals(EstadoReclamo.Ninguno, viewModel.reclamo.value)

        viewModel.elegirNombre("Anita")

        assertEquals(listOf(mensaje(R.string.error_reclamo_pendiente)), eventos)
        assertTrue(viewModel.reclamo.value is EstadoReclamo.Pendiente)
        assertTrue(observarSesion().first() is EstadoSesion.SinPerfil)
    }

    /** Beto le creó a Ana un perfil llamado Anita, reservado para un mail que no es el de ella. */
    private suspend fun betoLeCreaUnPerfilAAnaConOtroMail() {
        usuarios.registrar("Beto")
        usuarios.crearAmigoSinLogin("Anita", "otro@test.com", creador = cuentaDe("Beto"), amigoDe = jugador("Beto"))
    }

    private fun pedidoDeAna() = PedidoDeReclamo(auth.cuenta.value!!.uid, "ana@test.com", jugador("Anita"))

    @Test
    fun `quien pide un perfil creado por otro queda esperando, y si se lo confirman se le ofrece`() = runTest {
        betoLeCreaUnPerfilAAnaConOtroMail()
        val eventos = eventosTrasRegistrar(verificada = true)
        prepararEleccionDePerfil()

        viewModel.vincularPerfil("Anita", "")
        assertEquals(EstadoReclamo.Esperando(pedidoDeAna()), viewModel.reclamo.value)

        usuarios.aceptarPedido(pedidoDeAna(), creador = cuentaDe("Beto"), nombreCreador = "Beto")
        assertEquals(
            EstadoReclamo.Pendiente(Reclamo(jugador("Anita"), nombreCreador = "Beto", partidas = 0)),
            viewModel.reclamo.value,
        )

        viewModel.aceptarReclamo()
        assertEquals(jugador("Anita").id, (observarSesion().first() as EstadoSesion.Completa).idPerfil)
        assertTrue(eventos.isEmpty())
    }

    @Test
    fun `si no le confirman el perfil que pidio, se le avisa y pasa a elegir`() = runTest {
        betoLeCreaUnPerfilAAnaConOtroMail()
        val eventos = eventosTrasRegistrar(verificada = true)
        prepararEleccionDePerfil()
        viewModel.vincularPerfil("Anita", "")

        usuarios.rechazarPedido(pedidoDeAna())

        assertEquals(EstadoReclamo.Ninguno, viewModel.reclamo.value)
        assertEquals(listOf(mensaje(R.string.mensaje_pedido_rechazado)), eventos)
    }

    @Test
    fun `desistir del pedido lleva a elegir un nombre nuevo, sin avisos`() = runTest {
        betoLeCreaUnPerfilAAnaConOtroMail()
        val eventos = eventosTrasRegistrar(verificada = true)
        prepararEleccionDePerfil()
        viewModel.vincularPerfil("Anita", "")

        viewModel.cancelarPedido()

        assertEquals(EstadoReclamo.Ninguno, viewModel.reclamo.value)
        assertNull(usuarios.buscarPedidoPropio(auth.cuenta.value!!))
        assertTrue(eventos.isEmpty())
    }

    @Test
    fun `al volver a abrir la app con un pedido sin resolver sigue esperando`() = runTest {
        betoLeCreaUnPerfilAAnaConOtroMail()
        eventosTrasRegistrar(verificada = true)
        usuarios.pedirPerfil(auth.cuenta.value!!, "Anita")

        prepararEleccionDePerfil()

        assertEquals(EstadoReclamo.Esperando(pedidoDeAna()), viewModel.reclamo.value)
    }

    @Test
    fun `recuperar la contraseña envia el mail y avisa sin decir si tiene cuenta`() = runTest {
        val eventos = eventos()

        viewModel.recuperarContrasena("")
        viewModel.recuperarContrasena("nadie@test.com")

        assertEquals(listOf("nadie@test.com"), auth.recuperacionesEnviadas)
        assertEquals(
            listOf(mensaje(R.string.error_complete_campos), mensaje(R.string.mensaje_recuperacion_enviada)),
            eventos,
        )
    }

    @Test
    fun `salir cierra la sesion para poder usar otra cuenta`() = runTest {
        viewModel.registrarse("ana@test.com", "clave123")
        partidaEnCurso.partida = Partida.nueva(listOf(jugador("Ana"), jugador("Beto")))

        viewModel.salir()

        assertEquals(EstadoSesion.SinSesion, observarSesion().first())
        assertNull(partidaEnCurso.partida)
    }

    @Test
    fun `sin conexion se informa en lugar de fallar`() = runTest {
        auth.error = ErrorUsuario.SinConexion
        val eventos = eventos()

        viewModel.ingresar("ana@test.com", "clave123")

        assertEquals(listOf(mensaje(R.string.error_sin_conexion)), eventos)
    }
}
