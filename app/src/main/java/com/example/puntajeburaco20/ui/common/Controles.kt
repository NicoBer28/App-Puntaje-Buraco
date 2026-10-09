package com.example.puntajeburaco20.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.ModoJuego

/** Superficie blanca con borde fino, la unidad básica de las pantallas. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    relleno: PaddingValues = PaddingValues(20.dp),
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(relleno),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido,
        )
    }
}

@Composable
fun TituloSeccion(titulo: String, detalle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        if (detalle != null) {
            Text(
                detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Rótulo en mayúsculas con una barrita del color del equipo. */
@Composable
fun EtiquetaEquipo(texto: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(texto.uppercase(), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Círculo con la inicial del jugador, o un ícono de "agregar" si todavía no hay nadie. */
@Composable
fun Avatar(
    nombre: String?,
    color: Color,
    fondoVacio: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp,
    estiloInicial: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Box(
        modifier
            .size(tamano)
            .clip(CircleShape)
            .background(if (nombre != null) color else fondoVacio),
        contentAlignment = Alignment.Center,
    ) {
        if (nombre != null) {
            Text(
                nombre.take(1).uppercase(),
                style = estiloInicial,
                color = color.textoEncima(),
            )
        } else {
            Icon(
                painterResource(R.drawable.ic_person_add),
                contentDescription = null,
                modifier = Modifier.size(tamano / 2),
                tint = color,
            )
        }
    }
}

/** Color legible para escribir sobre este: oscuro sobre colores claros y blanco sobre oscuros. */
fun Color.textoEncima(): Color = if (luminance() > 0.4f) Color(0xFF0B1626) else Color.White

/** Control de dos opciones: 2 o 4 jugadores. */
@Composable
fun SelectorModo(modo: ModoJuego, alCambiar: (ModoJuego) -> Unit, modifier: Modifier = Modifier) {
    SelectorSegmentado(
        // El orden de R.array.modos_juego coincide con el de ModoJuego.
        opciones = stringArrayResource(R.array.modos_juego).asList(),
        elegida = modo.ordinal,
        alElegir = { alCambiar(ModoJuego.entries[it]) },
        tag = { "modo_${ModoJuego.entries[it].name}" },
        modifier = modifier,
    )
}

/** Control de opciones excluyentes, con un indicador que se desliza hasta la elegida. */
@Composable
fun SelectorSegmentado(
    opciones: List<String>,
    elegida: Int,
    alElegir: (Int) -> Unit,
    tag: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        val ancho = maxWidth / opciones.size
        val desplazamiento by animateDpAsState(ancho * elegida, label = "indicadorSelector")
        Box(
            Modifier
                .offset { IntOffset(desplazamiento.roundToPx(), 0) }
                .width(ancho)
                .fillMaxHeight()
                .shadow(2.dp, CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
        )
        Row(
            Modifier
                .fillMaxSize()
                .selectableGroup(),
        ) {
            opciones.forEachIndexed { i, opcion ->
                val estaElegida = i == elegida
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(selected = estaElegida, role = Role.RadioButton) { alElegir(i) }
                        .testTag(tag(i)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        opcion,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (estaElegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun BotonPrincipal(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    colores: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = alTocar,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = habilitado && !cargando,
        shape = MaterialTheme.shapes.medium,
        colors = colores,
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        ContenidoBoton(texto, icono, cargando)
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    habilitado: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedButton(
        onClick = alTocar,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = habilitado,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.5.dp,
            if (habilitado) color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        ContenidoBoton(texto, icono, cargando = false)
    }
}

@Composable
private fun ContenidoBoton(texto: String, @DrawableRes icono: Int?, cargando: Boolean) {
    if (cargando) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp,
            color = LocalContentColor.current,
        )
        return
    }
    if (icono != null) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    acciones: KeyboardActions = KeyboardActions.Default,
    transformacion: VisualTransformation = VisualTransformation.None,
    colorFoco: Color = MaterialTheme.colorScheme.primary,
    estiloTexto: TextStyle = MaterialTheme.typography.bodyLarge,
    iconoFinal: (@Composable () -> Unit)? = null,
) {
    val esquema = MaterialTheme.colorScheme
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        modifier = modifier,
        enabled = habilitado,
        label = { Text(etiqueta, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        textStyle = estiloTexto,
        trailingIcon = iconoFinal,
        keyboardOptions = teclado,
        keyboardActions = acciones,
        visualTransformation = transformacion,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorFoco,
            focusedLabelColor = colorFoco,
            cursorColor = colorFoco,
            unfocusedBorderColor = esquema.outlineVariant,
            focusedContainerColor = esquema.surface,
            unfocusedContainerColor = esquema.surfaceContainerLow,
            disabledContainerColor = esquema.surfaceContainer,
            disabledBorderColor = esquema.outlineVariant.copy(alpha = 0.6f),
        ),
    )
}

/** Atajo a otra pantalla: ícono en un círculo, título y una línea de detalle. */
@Composable
fun Atajo(
    titulo: String,
    detalle: String,
    @DrawableRes icono: Int,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = alTocar,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(icono),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                )
            }
        }
    }
}

/** Fila ancha tocable con ícono, título, detalle y flecha. */
@Composable
fun FilaAtajo(
    titulo: String,
    detalle: String,
    @DrawableRes icono: Int,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = alTocar,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(icono),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(detalle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
        }
    }
}

/** Circulito "vs" que se monta entre las tarjetas de los dos equipos. */
@Composable
fun InsigniaVersus(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.inverseSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.versus),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}
