package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakeSesionRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SesionUseCasesTest {

    private val usuarios = FakeUsuarioRepository()
    private val sesion = FakeSesionRepository()
    private val iniciarSesion = IniciarSesionUseCase(usuarios, sesion)
    private val registrarUsuario = RegistrarUsuarioUseCase(usuarios, sesion, ValidadorCredenciales())

    @Test
    fun `iniciar sesion con credenciales correctas guarda la sesion`() = runTest {
        usuarios.registrar("Ana", "clave")

        val usuario = iniciarSesion("ANA", "clave")

        assertEquals("Ana", usuario.nombre)
        assertEquals("ana", sesion.usuarioActualId.value)
    }

    @Test
    fun `iniciar sesion informa usuario inexistente, contraseña incorrecta o campos vacios`() = runTest {
        usuarios.registrar("Ana", "clave")

        esperarError<ErrorUsuario.CamposIncompletos> { iniciarSesion("", "clave") }
        esperarError<ErrorUsuario.UsuarioInexistente> { iniciarSesion("Beto", "clave") }
        esperarError<ErrorUsuario.ContrasenaIncorrecta> { iniciarSesion("Ana", "otra") }
        assertNull(sesion.usuarioActualId.value)
    }

    @Test
    fun `registrarse crea la cuenta e inicia sesion`() = runTest {
        registrarUsuario("Ana", "clave")

        assertNotNull(usuarios.obtener("ana"))
        assertEquals("ana", sesion.usuarioActualId.value)
    }

    @Test
    fun `no se puede registrar un nombre en uso, sin importar mayusculas`() = runTest {
        usuarios.registrar("Ana")

        esperarError<ErrorUsuario.NombreEnUso> { registrarUsuario("ANA", "clave") }
    }

    @Test
    fun `cerrar sesion la borra y descarta la partida en curso`() = runTest {
        sesion.iniciar("ana")
        val partidaEnCurso = FakePartidaEnCursoRepository(Partida.nueva(listOf(Jugador("Ana"), Jugador("Beto"))))

        CerrarSesionUseCase(sesion, partidaEnCurso)()

        assertNull(sesion.usuarioActualId.value)
        assertNull(partidaEnCurso.partida)
    }

    @Test
    fun `registrarse valida las credenciales antes de consultar la base`() = runTest {
        esperarError<ErrorUsuario.LongitudInsuficiente> { registrarUsuario("An", "clave") }
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
