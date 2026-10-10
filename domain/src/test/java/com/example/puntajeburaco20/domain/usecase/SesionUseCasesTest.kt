package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SesionUseCasesTest {

    private val usuarios = FakeUsuarioRepository()
    private val auth = FakeAuthRepository()
    private val validador = ValidadorCredenciales()
    private val observarSesion = ObservarSesionUseCase(auth, usuarios)
    private val iniciarSesion = IniciarSesionUseCase(auth)
    private val registrarCuenta = RegistrarCuentaUseCase(auth, validador)
    private val crearPerfil = CrearPerfilUseCase(auth, usuarios, validador)

    @Test
    fun `registrar una cuenta inicia sesion sin verificar y envia el mail de verificacion`() = runTest {
        val cuenta = registrarCuenta(" ana@test.com ", "clave123")

        assertEquals("ana@test.com", cuenta.mail)
        assertFalse(cuenta.verificada)
        assertEquals(listOf("ana@test.com"), auth.verificacionesEnviadas)
        assertEquals(EstadoSesion.SinVerificar(cuenta), observarSesion().first())
    }

    @Test
    fun `registrar valida el mail y la contraseña antes de crear la cuenta`() = runTest {
        esperarError<ErrorUsuario.CamposIncompletos> { registrarCuenta("", "clave123") }
        esperarError<ErrorUsuario.CamposIncompletos> { registrarCuenta("ana@test.com", "") }
        esperarError<ErrorUsuario.MailInvalido> { registrarCuenta("ana.test.com", "clave123") }
        esperarError<ErrorUsuario.ContrasenaCorta> { registrarCuenta("ana@test.com", "clave") }

        assertNull(auth.cuenta.value)
        assertTrue(auth.verificacionesEnviadas.isEmpty())
    }

    @Test
    fun `no se puede registrar un mail que ya tiene cuenta`() = runTest {
        auth.registrarCuenta(cuentaDe("Ana"))

        esperarError<ErrorUsuario.MailEnUso> { registrarCuenta("ana@test.com", "clave123") }
    }

    @Test
    fun `iniciar sesion con credenciales correctas deja la cuenta en sesion`() = runTest {
        auth.registrarCuenta(cuentaDe("Ana"), password = "clave123")

        val cuenta = iniciarSesion("ana@test.com ", "clave123")

        assertEquals(cuentaDe("Ana"), cuenta)
        assertEquals(cuenta, auth.cuenta.value)
    }

    @Test
    fun `iniciar sesion informa campos vacios y credenciales incorrectas`() = runTest {
        auth.registrarCuenta(cuentaDe("Ana"), password = "clave123")

        esperarError<ErrorUsuario.CamposIncompletos> { iniciarSesion("", "clave123") }
        esperarError<ErrorUsuario.CamposIncompletos> { iniciarSesion("ana@test.com", "") }
        esperarError<ErrorUsuario.CredencialesIncorrectas> { iniciarSesion("beto@test.com", "clave123") }
        esperarError<ErrorUsuario.CredencialesIncorrectas> { iniciarSesion("ana@test.com", "otraclave") }
        assertNull(auth.cuenta.value)
    }

    @Test
    fun `la sesion avanza de sin verificar a sin perfil y a completa`() = runTest {
        assertEquals(EstadoSesion.SinSesion, observarSesion().first())

        val cuenta = registrarCuenta("ana@test.com", "clave123")
        assertEquals(EstadoSesion.SinVerificar(cuenta), observarSesion().first())

        // Abrir el enlace del mail no cambia nada hasta que la app vuelve a consultar la cuenta.
        auth.verificarMail("ana@test.com")
        assertEquals(EstadoSesion.SinVerificar(cuenta), observarSesion().first())
        val verificada = auth.recargar()!!
        assertEquals(EstadoSesion.SinPerfil(verificada), observarSesion().first())

        val usuario = crearPerfil("Ana")
        assertEquals(EstadoSesion.Completa(verificada, usuario.id), observarSesion().first())
    }

    @Test
    fun `crear el perfil exige sesion, mail verificado y un nombre valido y libre`() = runTest {
        esperarError<ErrorUsuario.SinSesion> { crearPerfil("Ana") }

        registrarCuenta("ana@test.com", "clave123")
        esperarError<ErrorUsuario.MailSinVerificar> { crearPerfil("Ana") }

        auth.verificarMail("ana@test.com")
        auth.recargar()
        usuarios.registrar("Beto")
        esperarError<ErrorUsuario.LongitudInsuficiente> { crearPerfil("An") }
        esperarError<ErrorUsuario.NombreEnUso> { crearPerfil("BETO") }
        assertTrue(observarSesion().first() is EstadoSesion.SinPerfil)
    }

    @Test
    fun `el usuario actual es el perfil vinculado a la cuenta`() = runTest {
        val obtenerUsuarioActual = ObtenerUsuarioActualUseCase(usuarios, observarSesion)
        esperarError<ErrorUsuario.SinSesion> { obtenerUsuarioActual() }

        usuarios.registrar("Ana")
        auth.registrarCuenta(cuentaDe("Ana"), password = "clave123")
        iniciarSesion("ana@test.com", "clave123")

        assertEquals(jugador("Ana"), obtenerUsuarioActual().jugador)
    }

    @Test
    fun `una cuenta sin perfil no tiene usuario actual`() = runTest {
        val sinPerfil = ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Ana")), usuarios)

        esperarError<ErrorUsuario.SinSesion> { ObtenerUsuarioActualUseCase(usuarios, sinPerfil)() }
    }

    @Test
    fun `recuperar la contraseña valida el mail antes de enviarlo`() = runTest {
        val recuperar = RecuperarContrasenaUseCase(auth, validador)

        esperarError<ErrorUsuario.CamposIncompletos> { recuperar(" ") }
        esperarError<ErrorUsuario.MailInvalido> { recuperar("ana") }
        recuperar("ana@test.com")

        assertEquals(listOf("ana@test.com"), auth.recuperacionesEnviadas)
    }

    @Test
    fun `cerrar sesion la borra y descarta la partida en curso`() = runTest {
        val conSesion = FakeAuthRepository(cuentaDe("Ana"))
        val partidaEnCurso = FakePartidaEnCursoRepository(Partida.nueva(listOf(jugador("Ana"), jugador("Beto"))))

        CerrarSesionUseCase(conSesion, partidaEnCurso)()

        assertNull(conSesion.cuenta.value)
        assertNull(partidaEnCurso.partida)
    }
}

/** Verifica que el bloque falle con el error de negocio [E]. */
internal inline fun <reified E : Throwable> esperarError(bloque: () -> Unit) {
    val error = try {
        bloque()
        null
    } catch (e: Throwable) {
        e
    }
    if (error !is E) throw AssertionError("Se esperaba ${E::class.simpleName} pero fue $error", error)
}
