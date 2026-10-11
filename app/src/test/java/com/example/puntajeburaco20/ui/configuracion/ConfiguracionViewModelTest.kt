package com.example.puntajeburaco20.ui.configuracion

import com.example.puntajeburaco20.domain.model.LimitePuntos
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.fakes.FakePreferenciasRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.ui.configuracion.ConfiguracionViewModel.Estado
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfiguracionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencias = FakePreferenciasRepository()

    @Test
    fun `de fabrica sigue al sistema, sin limite y sin dejar la pantalla encendida`() = runTest {
        val viewModel = ConfiguracionViewModel(preferencias)

        assertEquals(
            Estado(ModoTema.SISTEMA, LimitePuntos(puntos = 3000, activo = false), pantallaEncendida = false),
            viewModel.estado.value,
        )
    }

    @Test
    fun `cambiar el tema lo guarda y lo refleja`() = runTest {
        val viewModel = ConfiguracionViewModel(preferencias)

        viewModel.cambiarTema(ModoTema.OSCURO)

        assertEquals(ModoTema.OSCURO, preferencias.modoTema.value)
        assertEquals(ModoTema.OSCURO, viewModel.estado.value?.modoTema)
    }

    @Test
    fun `activar el limite y cambiar sus puntos no se pisan entre si`() = runTest {
        val viewModel = ConfiguracionViewModel(preferencias)

        viewModel.activarLimite(true)
        viewModel.cambiarLimite(2000)
        assertEquals(LimitePuntos(puntos = 2000, activo = true), preferencias.limitePuntos.value)

        viewModel.activarLimite(false)
        assertEquals(LimitePuntos(puntos = 2000, activo = false), viewModel.estado.value?.limite)
    }

    @Test
    fun `un limite fuera de rango se ignora y queda el anterior`() = runTest {
        val viewModel = ConfiguracionViewModel(preferencias)

        viewModel.cambiarLimite(99)
        viewModel.cambiarLimite(1_000_000)
        assertEquals(3000, preferencias.limitePuntos.value.puntos)

        viewModel.cambiarLimite(100)
        assertEquals(100, preferencias.limitePuntos.value.puntos)
        viewModel.cambiarLimite(999_999)
        assertEquals(999_999, preferencias.limitePuntos.value.puntos)
    }

    @Test
    fun `dejar la pantalla encendida se guarda y se refleja`() = runTest {
        val viewModel = ConfiguracionViewModel(preferencias)

        viewModel.cambiarPantallaEncendida(true)

        assertEquals(true, preferencias.pantallaEncendida.value)
        assertEquals(true, viewModel.estado.value?.pantallaEncendida)
    }
}
