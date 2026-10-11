package com.example.puntajeburaco20.ui.tema

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta: azules profundos para la marca, cobalto para el equipo 1, azul petróleo para el
// equipo 2 y un dorado que se usa solo para destacar al ganador.

private val Abismo = Color(0xFF071A33)
private val Marino = Color(0xFF0F2D57)
private val Cobalto = Color(0xFF2858D6)
private val Petroleo = Color(0xFF0A7E9A)

internal val EsquemaClaro = lightColorScheme(
    primary = Cobalto,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0B2A6E),
    secondary = Petroleo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F0F7),
    onSecondaryContainer = Color(0xFF023A48),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7BA),
    onTertiaryContainer = Color(0xFF3F2800),
    background = Color(0xFFF1F5FB),
    onBackground = Color(0xFF0B1626),
    surface = Color.White,
    onSurface = Color(0xFF0B1626),
    surfaceVariant = Color(0xFFE3EAF4),
    onSurfaceVariant = Color(0xFF4A5A70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FD),
    surfaceContainer = Color(0xFFEEF2F9),
    surfaceContainerHigh = Color(0xFFE6EDF6),
    surfaceContainerHighest = Color(0xFFDDE5F1),
    inverseSurface = Color(0xFF13233A),
    inverseOnSurface = Color(0xFFE8EEF8),
    inversePrimary = Color(0xFF9DB8FF),
    outline = Color(0xFF8291A8),
    outlineVariant = Color(0xFFD5DDE9),
    error = Color(0xFFC2364B),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADF),
    onErrorContainer = Color(0xFF5C0617),
    scrim = Color(0xFF020A14),
)

internal val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF9DB8FF),
    onPrimary = Color(0xFF0A2463),
    primaryContainer = Color(0xFF1E3F99),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFF6DD3EA),
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF0B4F60),
    onSecondaryContainer = Color(0xFFC8F1FA),
    tertiary = Color(0xFFF4C46B),
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF5E4100),
    onTertiaryContainer = Color(0xFFFFE7BA),
    background = Color(0xFF06111F),
    onBackground = Color(0xFFE3EAF5),
    surface = Color(0xFF0C1A2D),
    onSurface = Color(0xFFE3EAF5),
    surfaceVariant = Color(0xFF1A2B42),
    onSurfaceVariant = Color(0xFFA9B7CC),
    surfaceContainerLowest = Color(0xFF040C17),
    surfaceContainerLow = Color(0xFF0A1727),
    surfaceContainer = Color(0xFF0F1E33),
    surfaceContainerHigh = Color(0xFF15263E),
    surfaceContainerHighest = Color(0xFF1B2E49),
    inverseSurface = Color(0xFFE3EAF5),
    inverseOnSurface = Color(0xFF13233A),
    inversePrimary = Cobalto,
    outline = Color(0xFF6F7F96),
    outlineVariant = Color(0xFF26374F),
    error = Color(0xFFFF8A9A),
    onError = Color(0xFF5C0617),
    errorContainer = Color(0xFF8C1D31),
    onErrorContainer = Color(0xFFFFDADF),
    scrim = Color.Black,
)

/** Colores propios de la app que Material no contempla: los de cada equipo y el del encabezado. */
@Immutable
data class ColoresBuraco(
    val equipoUno: Color,
    val equipoUnoSuave: Color,
    val equipoDos: Color,
    val equipoDosSuave: Color,
    val ganador: Color,
    val ganadorSuave: Color,
    /** El encabezado es azul oscuro tanto en modo claro como oscuro. */
    val encabezadoArriba: Color = Abismo,
    val encabezadoAbajo: Color = Marino,
    val sobreEncabezado: Color = Color.White,
    val sobreEncabezadoSuave: Color = Color(0xFFA9C4EC),
    val equipoUnoEnEncabezado: Color = Color(0xFF85A6FF),
    val equipoDosEnEncabezado: Color = Color(0xFF5CCBE3),
    val ganadorEnEncabezado: Color = Color(0xFFF4C46B),
)

internal val ColoresClaros = ColoresBuraco(
    equipoUno = Cobalto,
    equipoUnoSuave = Color(0xFFDCE6FF),
    equipoDos = Petroleo,
    equipoDosSuave = Color(0xFFD3F0F7),
    ganador = Color(0xFF8A5A00),
    ganadorSuave = Color(0xFFFFE7BA),
)

internal val ColoresOscuros = ColoresBuraco(
    equipoUno = Color(0xFF8FAEFF),
    equipoUnoSuave = Color(0xFF1C2F5E),
    equipoDos = Color(0xFF5CCBE3),
    equipoDosSuave = Color(0xFF0C3B48),
    ganador = Color(0xFFF4C46B),
    ganadorSuave = Color(0xFF3D2C0B),
    encabezadoArriba = Color(0xFF040E1C),
    encabezadoAbajo = Color(0xFF0D2547),
)

internal val LocalColoresBuraco = staticCompositionLocalOf { ColoresClaros }
