package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.fakes.FakeSesionRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmigosUseCasesTest {

    private val usuarios = FakeUsuarioRepository().apply {
        registrar("Ana")
        registrar("Beto")
    }
    private val sesion = FakeSesionRepository(usuarioInicial = "ana")
    private val obtenerActual = ObtenerUsuarioActualUseCase(usuarios, sesion)
    private val agregarAmigo = AgregarAmigoUseCase(usuarios, obtenerActual)
    private val eliminarAmigo = EliminarAmigoUseCase(usuarios, obtenerActual)
    private val crearUsuarioAmigo = CrearUsuarioAmigoUseCase(usuarios, obtenerActual, ValidadorCredenciales())

    @Test
    fun `agregar un amigo lo agrega en ambos sentidos`() = runTest {
        agregarAmigo("beto")

        assertTrue(usuarios.obtener("ana")!!.esAmigoDe(Jugador("Beto")))
        assertTrue(usuarios.obtener("beto")!!.esAmigoDe(Jugador("Ana")))
    }

    @Test
    fun `agregar amigo valida campo vacio, uno mismo, inexistente y repetido`() = runTest {
        esperarError<ErrorUsuario.CamposIncompletos> { agregarAmigo("") }
        esperarError<ErrorUsuario.EsElUsuarioActual> { agregarAmigo("ANA") }
        esperarError<ErrorUsuario.UsuarioInexistente> { agregarAmigo("Caro") }

        agregarAmigo("Beto")
        esperarError<ErrorUsuario.YaEsAmigo> { agregarAmigo("Beto") }
    }

    @Test
    fun `eliminar un amigo lo quita en ambos sentidos`() = runTest {
        agregarAmigo("Beto")

        eliminarAmigo("Beto")

        assertFalse(usuarios.obtener("ana")!!.esAmigoDe(Jugador("Beto")))
        assertFalse(usuarios.obtener("beto")!!.esAmigoDe(Jugador("Ana")))
    }

    @Test
    fun `no se puede eliminar a alguien que no es amigo`() = runTest {
        esperarError<ErrorUsuario.NoEsAmigo> { eliminarAmigo("Beto") }
        esperarError<ErrorUsuario.UsuarioInexistente> { eliminarAmigo("Caro") }
    }

    @Test
    fun `crear un usuario para un amigo lo registra y los hace amigos`() = runTest {
        crearUsuarioAmigo("Caro", "clave")

        val caro = usuarios.obtener("caro")!!
        assertEquals(listOf(Jugador("Ana")), caro.amigos)
        assertTrue(usuarios.obtener("ana")!!.esAmigoDe(caro.jugador))
    }

    @Test
    fun `crear usuario amigo valida credenciales, uno mismo y nombre en uso`() = runTest {
        esperarError<ErrorUsuario.CaracteresInvalidos> { crearUsuarioAmigo("Caro!", "clave") }
        esperarError<ErrorUsuario.EsElUsuarioActual> { crearUsuarioAmigo("Ana", "clave") }
        esperarError<ErrorUsuario.NombreEnUso> { crearUsuarioAmigo("BETO", "clave") }
    }

    @Test
    fun `sin sesion no se pueden gestionar amigos`() = runTest {
        val sinSesion = AgregarAmigoUseCase(usuarios, ObtenerUsuarioActualUseCase(usuarios, FakeSesionRepository()))

        esperarError<ErrorUsuario.SinSesion> { sinSesion("Beto") }
    }
}
