package com.example.puntajeburaco20.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.ModoTema

private val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

/** Versión angosta de Barlow, para títulos y puntajes: entra mucho número en poco ancho. */
private val BarlowCondensada = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

private fun estilo(familia: FontFamily, peso: FontWeight, tamano: Int, interlineado: Int, espaciado: Double = 0.0) =
    TextStyle(
        fontFamily = familia,
        fontWeight = peso,
        fontSize = tamano.sp,
        lineHeight = interlineado.sp,
        letterSpacing = espaciado.sp,
    )

private val Tipografia = Typography(
    displayLarge = estilo(BarlowCondensada, FontWeight.Bold, 72, 72, -0.5),
    displayMedium = estilo(BarlowCondensada, FontWeight.Bold, 56, 58, -0.25),
    displaySmall = estilo(BarlowCondensada, FontWeight.Bold, 44, 48),
    headlineLarge = estilo(BarlowCondensada, FontWeight.Bold, 38, 42),
    headlineMedium = estilo(BarlowCondensada, FontWeight.SemiBold, 30, 34),
    headlineSmall = estilo(BarlowCondensada, FontWeight.SemiBold, 26, 30),
    titleLarge = estilo(Barlow, FontWeight.SemiBold, 21, 28),
    titleMedium = estilo(Barlow, FontWeight.SemiBold, 17, 24, 0.1),
    titleSmall = estilo(Barlow, FontWeight.SemiBold, 15, 20, 0.1),
    bodyLarge = estilo(Barlow, FontWeight.Normal, 17, 24),
    bodyMedium = estilo(Barlow, FontWeight.Normal, 15, 21),
    bodySmall = estilo(Barlow, FontWeight.Normal, 13, 18),
    labelLarge = estilo(Barlow, FontWeight.SemiBold, 16, 20, 0.2),
    labelMedium = estilo(Barlow, FontWeight.SemiBold, 13, 16, 0.6),
    labelSmall = estilo(Barlow, FontWeight.Medium, 11, 14, 0.6),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun TemaBuraco(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColoresBuraco provides if (oscuro) ColoresOscuros else ColoresClaros) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = Tipografia,
            shapes = Formas,
            content = contenido,
        )
    }
}

/** Si corresponde el tema oscuro: el que eligió el usuario o, si no eligió, el del sistema. */
@Composable
fun ModoTema.esOscuro(): Boolean = when (this) {
    ModoTema.SISTEMA -> isSystemInDarkTheme()
    ModoTema.CLARO -> false
    ModoTema.OSCURO -> true
}

object TemaBuraco {
    val colores: ColoresBuraco
        @Composable @ReadOnlyComposable
        get() = LocalColoresBuraco.current
}

/** Números de ancho fijo: los puntajes no "bailan" cuando cambian. */
val TextStyle.numerico: TextStyle get() = copy(fontFeatureSettings = "tnum")
