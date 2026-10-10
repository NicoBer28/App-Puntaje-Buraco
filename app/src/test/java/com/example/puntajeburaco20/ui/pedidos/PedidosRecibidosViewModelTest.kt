package com.example.puntajeburaco20.ui.pedidos

import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.usecase.AceptarPedidoUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarPedidosRecibidosUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.RechazarPedidoUseCase
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.pedidos.PedidosRecibidosViewModel.Estado
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Ana creó los perfiles de Dani y de Eva; dos cuentas con otros mails dicen ser ellos. */
class PedidosRecibidosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val usuarios = FakeUsuarioRepository().apply { registrar("Ana") }
    private val sesion = ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Ana")), usuarios)

    private val cuentaDeDani = Cuenta(uid = "uid-dani", mail = "daniel@test.com", verificada = true)
    private val cuentaDeEva = Cuenta(uid = "uid-eva", mail = "evita@test.com", verificada = true)
    private val deDani = PedidoDeReclamo("uid-dani", "daniel@test.com", jugador("Dani"))
    private val deEva = PedidoDeReclamo("uid-eva", "evita@test.com", jugador("Eva"))

    private suspend fun crearViewModelConPedidos(): PedidosRecibidosViewModel {
        usuarios.crearAmigoSinLogin("Dani", "dani@test.com", creador = cuentaDe("Ana"), amigoDe = jugador("Ana"))
        usuarios.crearAmigoSinLogin("Eva", "eva@test.com", creador = cuentaDe("Ana"), amigoDe = jugador("Ana"))
        usuarios.pedirPerfil(cuentaDeDani, "Dani")
        usuarios.pedirPerfil(cuentaDeEva, "Eva")
        return PedidosRecibidosViewModel(
            observarPedidosRecibidos = ObservarPedidosRecibidosUseCase(usuarios, sesion),
            aceptarPedido = AceptarPedidoUseCase(usuarios, sesion),
            rechazarPedido = RechazarPedidoUseCase(usuarios),
        )
    }

    @Test
    fun `los pedidos se muestran de a uno, y al responder aparece el siguiente`() = runTest {
        val viewModel = crearViewModelConPedidos()
        assertEquals(Estado(deDani), viewModel.estado.value)

        viewModel.aceptar()
        assertEquals("daniel@test.com", usuarios.mailReservadoPara("dani"))
        assertEquals(Estado(deEva), viewModel.estado.value)

        viewModel.rechazar()
        assertEquals("eva@test.com", usuarios.mailReservadoPara("eva"))
        assertNull(viewModel.estado.value.pedido)
    }

    @Test
    fun `un pedido dejado para despues no se vuelve a mostrar, pero sigue sin resolver`() = runTest {
        val viewModel = crearViewModelConPedidos()

        viewModel.posponer()
        assertEquals(Estado(deEva), viewModel.estado.value)
        viewModel.posponer()

        assertNull(viewModel.estado.value.pedido)
        assertEquals(deDani, usuarios.buscarPedidoPropio(cuentaDeDani))
    }

    @Test
    fun `si no se puede aceptar, el pedido sigue a la vista con el motivo`() = runTest {
        val viewModel = crearViewModelConPedidos()
        // Mientras esperaba, esa cuenta se creó un perfil propio: su mail ya no está libre.
        usuarios.crear(cuentaDeDani, "Daniel")

        viewModel.aceptar()

        assertEquals(Estado(deDani, error = UiText.de(R.string.error_mail_con_perfil)), viewModel.estado.value)
        // El motivo es de ese pedido: no se arrastra al siguiente.
        viewModel.rechazar()
        assertEquals(Estado(deEva), viewModel.estado.value)
    }

    @Test
    fun `sin pedidos no se muestra nada`() = runTest {
        val viewModel = PedidosRecibidosViewModel(
            observarPedidosRecibidos = ObservarPedidosRecibidosUseCase(usuarios, sesion),
            aceptarPedido = AceptarPedidoUseCase(usuarios, sesion),
            rechazarPedido = RechazarPedidoUseCase(usuarios),
        )

        assertEquals(Estado(), viewModel.estado.value)
    }
}
