package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
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
    private val sesion = ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Ana")), usuarios)
    private val obtenerActual = ObtenerUsuarioActualUseCase(usuarios, sesion)
    private val agregarAmigo = AgregarAmigoUseCase(usuarios, obtenerActual)
    private val eliminarAmigo = EliminarAmigoUseCase(usuarios, obtenerActual)
    private val crearUsuarioAmigo = CrearUsuarioAmigoUseCase(usuarios, sesion, ValidadorCredenciales())

    @Test
    fun `agregar un amigo lo agrega en ambos sentidos`() = runTest {
        agregarAmigo("beto")

        assertTrue(usuarios.obtener("ana")!!.esAmigoDe(jugador("Beto")))
        assertTrue(usuarios.obtener("beto")!!.esAmigoDe(jugador("Ana")))
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

        assertFalse(usuarios.obtener("ana")!!.esAmigoDe(jugador("Beto")))
        assertFalse(usuarios.obtener("beto")!!.esAmigoDe(jugador("Ana")))
    }

    @Test
    fun `no se puede eliminar a alguien que no es amigo, a uno mismo ni a quien no existe`() = runTest {
        esperarError<ErrorUsuario.NoEsAmigo> { eliminarAmigo("Beto") }
        esperarError<ErrorUsuario.EsElUsuarioActual> { eliminarAmigo("ana") }
        esperarError<ErrorUsuario.UsuarioInexistente> { eliminarAmigo("Caro") }
    }

    @Test
    fun `a un amigo se lo encuentra por su nombre sin distinguir mayusculas`() = runTest {
        assertEquals(jugador("Beto"), usuarios.buscarPorNombre("BETO"))
        assertEquals(null, usuarios.buscarPorNombre("Caro"))
    }

    @Test
    fun `crear un perfil para un amigo lo registra sin login y los hace amigos`() = runTest {
        crearUsuarioAmigo("Caro")

        val caro = usuarios.obtener("caro")!!
        assertEquals("Caro", caro.nombre)
        assertEquals(listOf(jugador("Ana")), caro.amigos)
        assertTrue(usuarios.obtener("ana")!!.esAmigoDe(caro.jugador))
        assertFalse(usuarios.tieneLogin("caro"))
        assertEquals(cuentaDe("Ana").uid, usuarios.creadorDe("caro"))
    }

    @Test
    fun `crear un perfil para un amigo valida el nombre, uno mismo y nombre en uso`() = runTest {
        esperarError<ErrorUsuario.CaracteresInvalidos> { crearUsuarioAmigo("Caro!") }
        esperarError<ErrorUsuario.EsElUsuarioActual> { crearUsuarioAmigo("ANA") }
        esperarError<ErrorUsuario.NombreEnUso> { crearUsuarioAmigo("BETO") }
    }

    @Test
    fun `sin sesion no se pueden gestionar amigos`() = runTest {
        val nadie = ObservarSesionUseCase(FakeAuthRepository(), usuarios)
        val sinSesion = AgregarAmigoUseCase(usuarios, ObtenerUsuarioActualUseCase(usuarios, nadie))

        esperarError<ErrorUsuario.SinSesion> { sinSesion("Beto") }
    }
}
