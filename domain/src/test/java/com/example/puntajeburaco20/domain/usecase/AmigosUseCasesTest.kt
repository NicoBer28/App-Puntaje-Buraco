package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import kotlinx.coroutines.flow.first
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
    private val observarPerfilesACargo = ObservarPerfilesACargoUseCase(usuarios, sesion)
    private val reservarPerfilACargo = ReservarPerfilACargoUseCase(usuarios, sesion, ValidadorCredenciales())

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
    fun `crear un perfil para un amigo lo registra sin login, reservado para su mail, y los hace amigos`() = runTest {
        crearUsuarioAmigo("Caro", " Caro@Test.com ")

        val caro = usuarios.obtener("caro")!!
        assertEquals("Caro", caro.nombre)
        assertEquals(listOf(jugador("Ana")), caro.amigos)
        assertTrue(usuarios.obtener("ana")!!.esAmigoDe(caro.jugador))
        assertFalse(usuarios.tieneLogin("caro"))
        assertEquals(cuentaDe("Ana").uid, usuarios.creadorDe("caro"))
        // El mail se guarda como lo deja el servicio de cuentas: sin espacios y en minúsculas.
        assertEquals("caro@test.com", usuarios.mailReservadoPara("caro"))
    }

    @Test
    fun `crear un perfil para un amigo valida el nombre, el mail, uno mismo y nombre en uso`() = runTest {
        esperarError<ErrorUsuario.CamposIncompletos> { crearUsuarioAmigo("", "caro@test.com") }
        esperarError<ErrorUsuario.CamposIncompletos> { crearUsuarioAmigo("Caro", " ") }
        esperarError<ErrorUsuario.CaracteresInvalidos> { crearUsuarioAmigo("Caro!", "caro@test.com") }
        esperarError<ErrorUsuario.MailInvalido> { crearUsuarioAmigo("Caro", "caro.test.com") }
        esperarError<ErrorUsuario.EsElUsuarioActual> { crearUsuarioAmigo("ANA", "caro@test.com") }
        esperarError<ErrorUsuario.NombreEnUso> { crearUsuarioAmigo("BETO", "caro@test.com") }
        assertEquals(null, usuarios.obtener("caro"))
    }

    @Test
    fun `no se crea un perfil para un mail que ya tiene cuenta o perfil reservado`() = runTest {
        // Beto ya usa la app: hay que agregarlo como amigo, no crearle otro perfil.
        esperarError<ErrorUsuario.MailConPerfil> { crearUsuarioAmigo("Beto2", "BETO@test.com") }

        crearUsuarioAmigo("Caro", "caro@test.com")
        esperarError<ErrorUsuario.MailConPerfil> { crearUsuarioAmigo("Caro2", "caro@test.com") }
        assertEquals(null, usuarios.obtener("caro2"))
    }

    @Test
    fun `quien creo un perfil ve los que tiene a cargo y les corrige el mail`() = runTest {
        crearUsuarioAmigo("Caro", "caro@test.com")
        val caro = PerfilACargo(jugador("Caro"), "caro@test.com")
        assertEquals(listOf(caro), observarPerfilesACargo().first())

        reservarPerfilACargo(caro, "Carolina@test.com")

        assertEquals(listOf(PerfilACargo(jugador("Caro"), "carolina@test.com")), observarPerfilesACargo().first())
        // El mail anterior quedó libre.
        crearUsuarioAmigo("Dani", "caro@test.com")
    }

    @Test
    fun `a un perfil creado sin mail se le puede cargar despues`() = runTest {
        usuarios.crearAmigoSinMail("Caro", creador = cuentaDe("Ana"), amigoDe = jugador("Ana"))
        val caro = PerfilACargo(jugador("Caro"), mail = null)
        assertEquals(listOf(caro), observarPerfilesACargo().first())

        reservarPerfilACargo(caro, "caro@test.com")

        assertEquals("caro@test.com", usuarios.mailReservadoPara("caro"))
    }

    @Test
    fun `corregir el mail valida el formato y que no tenga ya un perfil`() = runTest {
        crearUsuarioAmigo("Caro", "caro@test.com")
        crearUsuarioAmigo("Dani", "dani@test.com")
        val caro = PerfilACargo(jugador("Caro"), "caro@test.com")

        esperarError<ErrorUsuario.CamposIncompletos> { reservarPerfilACargo(caro, "") }
        esperarError<ErrorUsuario.MailInvalido> { reservarPerfilACargo(caro, "caro") }
        esperarError<ErrorUsuario.MailConPerfil> { reservarPerfilACargo(caro, "dani@test.com") }
        esperarError<ErrorUsuario.MailConPerfil> { reservarPerfilACargo(caro, "beto@test.com") }
        // Guardar el mismo mail no cambia nada.
        reservarPerfilACargo(caro, "CARO@test.com")

        assertEquals("caro@test.com", usuarios.mailReservadoPara("caro"))
    }

    @Test
    fun `los perfiles a cargo son solo los que creo cada uno`() = runTest {
        crearUsuarioAmigo("Caro", "caro@test.com")
        val deBeto = ObservarPerfilesACargoUseCase(usuarios, ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Beto")), usuarios))

        assertEquals(emptyList<PerfilACargo>(), deBeto().first())
    }

    @Test
    fun `sin sesion no se pueden gestionar amigos`() = runTest {
        val nadie = ObservarSesionUseCase(FakeAuthRepository(), usuarios)
        val sinSesion = AgregarAmigoUseCase(usuarios, ObtenerUsuarioActualUseCase(usuarios, nadie))

        esperarError<ErrorUsuario.SinSesion> { sinSesion("Beto") }
    }
}
