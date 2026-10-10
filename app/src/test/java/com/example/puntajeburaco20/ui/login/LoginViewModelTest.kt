package com.example.puntajeburaco20.ui.login

import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.CrearPerfilUseCase
import com.example.puntajeburaco20.domain.usecase.IniciarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.RecuperarContrasenaUseCase
import com.example.puntajeburaco20.domain.usecase.RegistrarCuentaUseCase
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import com.example.puntajeburaco20.ui.common.UiText
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
    private val validador = ValidadorCredenciales()
    private val observarSesion = ObservarSesionUseCase(auth, usuarios)

    private val viewModel = LoginViewModel(
        iniciarSesion = IniciarSesionUseCase(auth),
        registrarCuenta = RegistrarCuentaUseCase(auth, validador),
        recuperarContrasenaUseCase = RecuperarContrasenaUseCase(auth, validador),
        crearPerfil = CrearPerfilUseCase(auth, usuarios, validador),
        cerrarSesion = CerrarSesionUseCase(auth, partidaEnCurso),
        auth = auth,
    )

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
