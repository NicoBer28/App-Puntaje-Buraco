package com.example.puntajeburaco20.ui.recorrido

import com.example.puntajeburaco20.domain.model.Recorrido
import com.example.puntajeburaco20.fakes.FakePreferenciasRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecorridoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `sin nada visto estan todos pendientes, y terminar uno lo saca`() = runTest {
        val preferencias = FakePreferenciasRepository()
        val viewModel = RecorridoViewModel(preferencias)
        assertEquals(setOf(Recorrido.INICIO, Recorrido.PARTIDA), viewModel.pendientes.value)

        viewModel.terminar(Recorrido.INICIO)

        assertEquals(setOf(Recorrido.PARTIDA), viewModel.pendientes.value)
        assertEquals(setOf(Recorrido.INICIO), preferencias.recorridosVistos.value)
    }

    @Test
    fun `los que ya se vieron no quedan pendientes hasta que se los reinicia`() = runTest {
        val preferencias = FakePreferenciasRepository(recorridosVistos = Recorrido.entries.toSet())
        val viewModel = RecorridoViewModel(preferencias)
        assertEquals(emptySet<Recorrido>(), viewModel.pendientes.value)

        preferencias.reiniciarRecorridos()

        assertEquals(Recorrido.entries.toSet(), viewModel.pendientes.value)
    }
}
