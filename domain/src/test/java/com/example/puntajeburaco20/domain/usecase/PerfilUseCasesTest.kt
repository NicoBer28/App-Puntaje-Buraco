package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakePartidasJugadasRepository
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

/** Ana y Beto usan la app y son amigos; a Dani, que no la usa, le creó el perfil Ana. */
class PerfilUseCasesTest {

    private val usuarios = FakeUsuarioRepository().apply {
        registrar("Ana")
        registrar("Beto")
    }
    private val auth = FakeAuthRepository().apply { registrarCuenta(cuentaDe("Ana"), password = "clave123") }
    private val partidaEnCurso = FakePartidaEnCursoRepository()
    private val partidasJugadas = FakePartidasJugadasRepository()
    private val validador = ValidadorCredenciales()
    private val sesion = ObservarSesionUseCase(auth, usuarios)
    private val renombrarPerfil = RenombrarPerfilUseCase(usuarios, validador)
    private val borrarPerfilACargo = BorrarPerfilACargoUseCase(usuarios, sesion)
    private val borrarCuenta = BorrarCuentaUseCase(auth, usuarios, partidaEnCurso, sesion)
    private val perfilesACargo = ObservarPerfilesACargoUseCase(usuarios, sesion)

    private val dani = PerfilACargo(jugador("Dani"), "dani@test.com")

    @Before
    fun preparar() = runTest {
        auth.iniciarSesion("ana@test.com", "clave123")
        usuarios.agregarAmistad(jugador("Ana"), jugador("Beto"))
        usuarios.crearAmigoSinLogin("Dani", "dani@test.com", creador = cuentaDe("Ana"), amigoDe = jugador("Ana"))
        usuarios.agregarAmistad(jugador("Beto"), jugador("Dani"))
    }

    @Test
    fun `renombrar el perfil propio cambia como lo ven sus amigos y deja libre el nombre anterior`() = runTest {
        val renombrada = renombrarPerfil(jugador("Ana"), "Anita")

        // Sigue siendo el mismo perfil: lo que cambia es el nombre.
        assertEquals(Jugador(jugador("Ana").id, "Anita"), renombrada)
        assertEquals(renombrada, usuarios.obtener(renombrada.id)?.jugador)
        assertTrue(renombrada in usuarios.obtener("beto")!!.amigos)
        assertEquals(renombrada, usuarios.buscarPorNombre("ANITA"))
        assertNull(usuarios.buscarPorNombre("Ana"))

        // Otra persona puede quedarse con el nombre que dejó.
        val nueva = Cuenta(uid = "uid-otra", mail = "otra@test.com", verificada = true)
        val otraAna = CrearPerfilUseCase(FakeAuthRepository(nueva), usuarios, validador)("Ana")
        assertFalse(otraAna.jugador.esMismaPersona(renombrada))
    }

    @Test
    fun `renombrar valida el nombre y que no sea de otro perfil`() = runTest {
        esperarError<ErrorUsuario.LongitudInsuficiente> { renombrarPerfil(jugador("Ana"), "An") }
        esperarError<ErrorUsuario.CaracteresInvalidos> { renombrarPerfil(jugador("Ana"), "Ana!") }
        esperarError<ErrorUsuario.NombreEnUso> { renombrarPerfil(jugador("Ana"), "BETO") }
        esperarError<ErrorUsuario.NombreEnUso> { renombrarPerfil(jugador("Ana"), "Dani") }

        // El mismo nombre no cambia nada, y cambiar solo mayúsculas sí se puede.
        assertEquals(jugador("Ana"), renombrarPerfil(jugador("Ana"), "Ana"))
        assertEquals("ANA", renombrarPerfil(jugador("Ana"), "ANA").nombre)
    }

    @Test
    fun `quien creo un perfil lo puede renombrar, y sigue reservado para el mismo mail`() = runTest {
        val renombrado = renombrarPerfil(dani.jugador, "Daniel")

        assertEquals(listOf(PerfilACargo(renombrado, "dani@test.com")), perfilesACargo().first())
        assertTrue(renombrado in usuarios.obtener("ana")!!.amigos)
    }

    @Test
    fun `borrar un perfil creado para otro lo quita de todas las listas y libera su nombre y su mail`() = runTest {
        usuarios.pedirPerfil(Cuenta(uid = "uid-x", mail = "x@test.com", verificada = true), "Dani")
        partidasJugadas.guardar(Partida.nueva(listOf(jugador("Ana"), jugador("Dani"))).copy(ganador = LadoEquipo.UNO))

        borrarPerfilACargo(dani)

        assertNull(usuarios.obtener("dani"))
        assertEquals(emptyList<PerfilACargo>(), perfilesACargo().first())
        assertEquals(listOf(jugador("Beto")), usuarios.obtener("ana")!!.amigos)
        assertEquals(listOf(jugador("Ana")), usuarios.obtener("beto")!!.amigos)
        // Tampoco queda el pedido de quien decía ser Dani.
        assertEquals(emptyList<PedidoDeReclamo>(), ObservarPedidosRecibidosUseCase(usuarios, sesion)().first())
        // Las partidas que jugó siguen en el historial de los demás.
        assertEquals(1, partidasJugadas.obtenerDe(jugador("Ana"), limite = 10).size)

        // El nombre y el mail se pueden volver a usar.
        CrearUsuarioAmigoUseCase(usuarios, sesion, validador)("Dani", "dani@test.com")
    }

    @Test
    fun `borrar la cuenta elimina el perfil, sus amistades y la cuenta de acceso`() = runTest {
        partidaEnCurso.partida = Partida.nueva(listOf(jugador("Ana"), jugador("Beto")))
        partidasJugadas.guardar(Partida.nueva(listOf(jugador("Ana"), jugador("Beto"))).copy(ganador = LadoEquipo.UNO))

        borrarCuenta("clave123")

        assertEquals(EstadoSesion.SinSesion, sesion().first())
        assertFalse(auth.tieneCuenta("ana@test.com"))
        assertNull(usuarios.obtener("ana"))
        assertEquals(listOf(jugador("Dani")), usuarios.obtener("beto")!!.amigos)
        assertNull(partidaEnCurso.partida)
        // Beto conserva en su historial la partida que jugó con ella.
        assertEquals(1, partidasJugadas.obtenerDe(jugador("Beto"), limite = 10).size)

        // Puede volver a registrarse con el mismo mail y el mismo nombre.
        val deNuevo = auth.registrar("ana@test.com", "otraclave")
        auth.verificarMail("ana@test.com")
        auth.recargar()
        assertEquals("Ana", CrearPerfilUseCase(auth, usuarios, validador)("Ana").nombre)
        assertEquals("ana@test.com", deNuevo.mail)
    }

    @Test
    fun `sin la contraseña correcta no se borra nada`() = runTest {
        esperarError<ErrorUsuario.CamposIncompletos> { borrarCuenta("") }
        esperarError<ErrorUsuario.ContrasenaIncorrecta> { borrarCuenta("otraclave") }

        assertTrue(sesion().first() is EstadoSesion.Completa)
        assertTrue(auth.tieneCuenta("ana@test.com"))
        assertEquals(2, usuarios.obtener("ana")!!.amigos.size)
    }

    @Test
    fun `sin sesion no se borra una cuenta ni un perfil a cargo`() = runTest {
        val nadie = ObservarSesionUseCase(FakeAuthRepository(), usuarios)

        esperarError<ErrorUsuario.SinSesion> { BorrarCuentaUseCase(FakeAuthRepository(), usuarios, partidaEnCurso, nadie)("clave123") }
        esperarError<ErrorUsuario.SinSesion> { BorrarPerfilACargoUseCase(usuarios, nadie)(dani) }
    }
}
