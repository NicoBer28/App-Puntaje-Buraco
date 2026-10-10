package com.example.puntajeburaco20

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakePartidasJugadasRepository
import com.example.puntajeburaco20.fakes.FakePreferenciasRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import com.example.puntajeburaco20.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** Recorre los flujos principales de la app de punta a punta, con repositorios en memoria. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FlujosPrincipalesTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    // La actividad se abre a mano en cada test, después de preparar los repositorios.
    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject
    lateinit var usuarios: FakeUsuarioRepository

    @Inject
    lateinit var auth: FakeAuthRepository

    @Inject
    lateinit var partidasJugadas: FakePartidasJugadasRepository

    @Inject
    lateinit var preferencias: FakePreferenciasRepository

    @Before
    fun preparar() {
        hilt.inject()
        usuarios.registrar("Ana")
        usuarios.registrar("Beto")
        auth.registrarCuenta(cuentaDe("Ana"), password = CLAVE)
        runBlocking { usuarios.agregarAmistad(jugador("Ana"), jugador("Beto")) }
    }

    private fun abrirApp() = ActivityScenario.launch(MainActivity::class.java)

    private fun iniciarSesionComoAna() = runBlocking { auth.iniciarSesion(cuentaDe("Ana").mail, CLAVE) }

    private fun amigosDeAna() = runBlocking { usuarios.obtener("ana")!!.amigos }

    private fun escribir(tag: String, texto: String) {
        esperar(tag)
        compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(texto)
    }

    private fun texto(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun esperar(tag: String) {
        compose.waitUntil(TIEMPO_MAXIMO_MS) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Toca un elemento que puede estar fuera de la parte visible de una lista desplazable. */
    private fun tocarDesplazando(tag: String) {
        esperar(tag)
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun tocar(tag: String) {
        esperar(tag)
        compose.onNodeWithTag(tag).performClick()
    }

    private fun elegir(posicion: Int, nombre: String) {
        tocarDesplazando("jugador_$posicion")
        tocarDesplazando("opcion_${jugador(nombre).id}")
    }

    private fun confirmarDialogo() {
        compose.onNodeWithText(texto(R.string.si)).performClick()
        compose.waitForIdle()
    }

    private fun empezarPartidaAnaContraBeto() {
        elegir(0, "Ana")
        elegir(1, "Beto")
        compose.onNodeWithTag("btnNuevaPartida").performClick()
        esperar("btnSumar")
    }

    private fun cargarRonda(baseUno: String, puntosUno: String, baseDos: String, puntosDos: String) {
        compose.onNodeWithTag("baseUno").performScrollTo().performTextReplacement(baseUno)
        compose.onNodeWithTag("puntosUno").performScrollTo().performTextReplacement(puntosUno)
        compose.onNodeWithTag("baseDos").performScrollTo().performTextReplacement(baseDos)
        compose.onNodeWithTag("puntosDos").performScrollTo().performTextReplacement(puntosDos)
        tocar("btnSumar")
        compose.waitForIdle()
    }

    @Test
    fun sinSesionSeMuestraElLoginYAlIngresarSeVaALaPantallaPrincipal() {
        abrirApp().use {
            escribir("mail", "ana@test.com")
            escribir("password", CLAVE)
            // Con el teclado abierto el botón puede quedar fuera de la parte visible.
            tocarDesplazando("btnLogin")

            // El nombre está dentro del acceso al perfil, que agrupa su contenido para los lectores de pantalla.
            esperar("btnPerfil")
            compose.onNodeWithTag("nombreUsuario", useUnmergedTree = true).assertTextEquals("Ana")
            assertEquals(cuentaDe("Ana"), auth.cuenta.value)
        }
    }

    @Test
    fun crearUnaCuentaPideVerificarElMailYElegirNombreAntesDeEntrar() {
        abrirApp().use {
            escribir("mail", "caro@test.com")
            escribir("password", CLAVE)
            tocarDesplazando("btnCrear")

            // Hasta que no se abre el enlace del mail no se puede avanzar.
            esperar("btnYaVerifique")
            assertEquals(listOf("caro@test.com"), auth.verificacionesEnviadas)
            tocarDesplazando("btnYaVerifique")
            compose.waitForIdle()
            compose.onNodeWithTag("btnYaVerifique").assertIsDisplayed()

            auth.verificarMail("caro@test.com")
            tocarDesplazando("btnYaVerifique")

            escribir("usuarioNuevo", "Caro")
            // Se confirma desde el teclado: mientras se abre, el botón todavía se está moviendo.
            compose.onNodeWithTag("usuarioNuevo").performImeAction()

            esperar("btnPerfil")
            compose.onNodeWithTag("nombreUsuario", useUnmergedTree = true).assertTextEquals("Caro")
        }
    }

    @Test
    fun agregarUnAmigoYCrearUnUsuarioParaOtroLosDejaElegibles() {
        usuarios.registrar("Caro")
        iniciarSesionComoAna()
        abrirApp().use {
            tocarDesplazando("btnUsuario")

            // Se confirma desde el teclado: mientras se abre, los botones todavía se están moviendo.
            escribir("usuarioAmigo", "caro")
            compose.onNodeWithTag("usuarioAmigo").performImeAction()
            compose.waitUntil(TIEMPO_MAXIMO_MS) { amigosDeAna().any { it.nombre == "Caro" } }

            escribir("usuarioCrear", "Dani")
            compose.onNodeWithTag("usuarioCrear").performImeAction()
            compose.waitUntil(TIEMPO_MAXIMO_MS) { amigosDeAna().any { it.nombre == "Dani" } }
            // Dani no usa la app: su perfil no tiene cuenta y queda a cargo de Ana.
            assertFalse(usuarios.tieneLogin("dani"))
            assertEquals(cuentaDe("Ana").uid, usuarios.creadorDe("dani"))

            tocar("btnVolver")
            tocarDesplazando("jugador_0")
            esperar("opcion_caro")
            esperar("opcion_dani")
        }
    }

    @Test
    fun sumarYDeshacerUnaRondaActualizaLosTotales() {
        iniciarSesionComoAna()
        abrirApp().use {
            empezarPartidaAnaContraBeto()
            compose.onNodeWithTag("btnDeshacer").assertIsNotEnabled()

            cargarRonda("100", "30", "0", "-20")
            compose.onNodeWithTag("totalUno").assertTextEquals("130")
            compose.onNodeWithTag("totalDos").assertTextEquals("-20")

            tocarDesplazando("btnDeshacer")
            confirmarDialogo()
            compose.onNodeWithTag("totalUno").assertTextEquals("0")
            compose.onNodeWithTag("btnDeshacer").assertIsNotEnabled()
        }
    }

    @Test
    fun unaPartidaTerminadaApareceEnMisPartidas() {
        iniciarSesionComoAna()
        abrirApp().use {
            empezarPartidaAnaContraBeto()
            cargarRonda("200", "50", "100", "0")
            tocar("btnFin")
            compose.onNodeWithTag("ganador_UNO").performClick()
            compose.waitForIdle()
            assertEquals(1, partidasJugadas.guardadas.size)

            tocarDesplazando("btnAtras")
            confirmarDialogo()
            tocarDesplazando("btnHistoriales")
            tocarDesplazando("btnMisPartidas")

            esperar("partidaJugada")
            compose.onNodeWithText("Ana vs Beto").assertIsDisplayed()
            compose.onNodeWithText(texto(R.string.partida_ganada)).assertIsDisplayed()
        }
    }

    @Test
    fun cerrarSesionLlevaAlLogin() {
        iniciarSesionComoAna()
        abrirApp().use {
            tocar("btnPerfil")
            tocarDesplazando("btnCerrarSesion")
            confirmarDialogo()

            esperar("btnLogin")
            compose.onNodeWithTag("btnLogin").assertIsDisplayed()
            assertEquals(null, auth.cuenta.value)
        }
    }

    @Test
    fun elPerfilMuestraAlUsuarioYGuardaElTemaElegido() {
        iniciarSesionComoAna()
        abrirApp().use {
            tocar("btnPerfil")
            esperar("nombrePerfil")
            compose.onNodeWithTag("nombrePerfil").assertTextEquals("Ana")
            compose.onNodeWithTag("tema_SISTEMA").assertIsSelected()

            tocarDesplazando("tema_OSCURO")
            compose.waitForIdle()

            compose.onNodeWithTag("tema_OSCURO").assertIsSelected()
            assertEquals(ModoTema.OSCURO, preferencias.modoTema.value)
        }
    }

    private companion object {
        const val TIEMPO_MAXIMO_MS = 5_000L
        const val CLAVE = "clave123"
    }
}
