package com.example.puntajeburaco20.ui.perfil

import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.example.puntajeburaco20.domain.usecase.BorrarCuentaUseCase
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
import com.example.puntajeburaco20.domain.usecase.RenombrarPerfilUseCase
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakePreferenciasRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.perfil.PerfilViewModel.Evento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val usuarios = FakeUsuarioRepository().apply {
        registrar("Ana")
        registrar("Beto")
    }
    private val auth = FakeAuthRepository().apply {
        registrarCuenta(cuentaDe("Ana"), password = "clave123")
        runBlocking { iniciarSesion("ana@test.com", "clave123") }
    }
    private val partidaEnCurso = FakePartidaEnCursoRepository()
    private val preferencias = FakePreferenciasRepository()
    private val sesion = ObservarSesionUseCase(auth, usuarios)

    private fun crearViewModel() = PerfilViewModel(
        preferencias = preferencias,
        cerrarSesionUseCase = CerrarSesionUseCase(auth, partidaEnCurso),
        renombrarPerfil = RenombrarPerfilUseCase(usuarios, ValidadorCredenciales()),
        borrarCuentaUseCase = BorrarCuentaUseCase(auth, usuarios, partidaEnCurso, sesion),
        observarUsuarioActual = ObservarUsuarioActualUseCase(usuarios, sesion),
    )

    private fun TestScope.eventosDe(viewModel: PerfilViewModel): List<Evento> {
        val eventos = mutableListOf<Evento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.toList(eventos) }
        return eventos
    }

    @Test
    fun `muestra al usuario y cuantos amigos tiene`() = runTest {
        val viewModel = crearViewModel()
        assertEquals("Ana", viewModel.estado.value.nombreUsuario)
        assertEquals(0, viewModel.estado.value.cantidadAmigos)

        usuarios.agregarAmistad(jugador("Ana"), jugador("Beto"))
        assertEquals(1, viewModel.estado.value.cantidadAmigos)
    }

    @Test
    fun `cambiar el tema lo guarda y lo refleja`() = runTest {
        val viewModel = crearViewModel()
        assertEquals(ModoTema.SISTEMA, viewModel.estado.value.modoTema)

        viewModel.cambiarTema(ModoTema.OSCURO)

        assertEquals(ModoTema.OSCURO, preferencias.modoTema.value)
        assertEquals(ModoTema.OSCURO, viewModel.estado.value.modoTema)
    }

    @Test
    fun `cerrar sesion descarta la partida en curso, sin avisar nada a la pantalla`() = runTest {
        partidaEnCurso.partida = Partida.nueva(listOf(jugador("Ana"), jugador("Beto")))
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.cerrarSesion()

        assertNull(auth.cuenta.value)
        assertNull(partidaEnCurso.partida)
        assertEquals(emptyList<Evento>(), eventos)
        // Mientras se sale de la pantalla se sigue viendo a quien cerró la sesión.
        assertEquals("Ana", viewModel.estado.value.nombreUsuario)
    }

    @Test
    fun `cambiar el nombre lo muestra y lo avisa`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.cambiarNombre("Anita")

        assertEquals("Anita", viewModel.estado.value.nombreUsuario)
        assertEquals(listOf(Evento.Mensaje(UiText.de(R.string.mensaje_nombre_cambiado))), eventos)
        assertFalse(viewModel.estado.value.ocupado)
    }

    @Test
    fun `un nombre que ya es de otro o no es valido muestra el motivo y no cambia nada`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.cambiarNombre("Beto")
        viewModel.cambiarNombre("Ana María")

        assertEquals(
            listOf(
                Evento.Mensaje(UiText.de(R.string.error_nombre_en_uso)),
                Evento.Mensaje(UiText.Plural(R.plurals.error_longitud_maxima, 8)),
            ),
            eventos,
        )
        assertEquals("Ana", viewModel.estado.value.nombreUsuario)
    }

    @Test
    fun `borrar la cuenta con su contraseña la elimina, sin avisar nada a la pantalla`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.borrarCuenta("clave123")

        assertNull(auth.cuenta.value)
        assertFalse(auth.tieneCuenta("ana@test.com"))
        assertNull(usuarios.obtener("ana"))
        assertEquals(emptyList<Evento>(), eventos)
    }

    @Test
    fun `con otra contraseña la cuenta no se borra`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.borrarCuenta("otraclave")

        assertEquals(listOf(Evento.Mensaje(UiText.de(R.string.error_contrasena_incorrecta))), eventos)
        assertEquals(cuentaDe("Ana"), auth.cuenta.value)
        assertEquals("Ana", usuarios.obtener("ana")?.nombre)
    }
}
