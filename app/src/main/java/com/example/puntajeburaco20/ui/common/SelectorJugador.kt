package com.example.puntajeburaco20.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Jugador
import kotlinx.coroutines.launch

/**
 * Casilla para elegir un jugador. Al tocarla se abre una hoja con los [opciones]; la opción
 * [textoQuitar] vuelve a dejarla vacía. El estado vive en el ViewModel: la casilla solo lo
 * refleja y avisa los cambios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasillaJugador(
    etiqueta: String,
    elegido: Jugador?,
    opciones: List<Jugador>,
    textoVacio: String,
    textoQuitar: String,
    color: Color,
    colorSuave: Color,
    alElegir: (Jugador?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierta by rememberSaveable { mutableStateOf(false) }
    val forma = MaterialTheme.shapes.medium
    Row(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp,
                if (elegido != null) color.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
                forma,
            )
            .clickable(role = Role.Button) { abierta = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(elegido?.nombre, color, colorSuave)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                elegido?.nombre ?: textoVacio,
                style = MaterialTheme.typography.titleMedium,
                color = if (elegido != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.rotate(90f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (abierta) {
        val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        val elegirYCerrar = { jugador: Jugador? ->
            alElegir(jugador)
            scope.launch { estadoHoja.hide() }.invokeOnCompletion { abierta = false }
        }
        ModalBottomSheet(
            onDismissRequest = { abierta = false },
            sheetState = estadoHoja,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                stringResource(R.string.elegir_jugador_titulo),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            LazyColumn(Modifier.navigationBarsPadding()) {
                if (opciones.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.elegir_jugador_vacio),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(opciones, key = { it.id }) { jugador ->
                    FilaOpcion(
                        modifier = Modifier.testTag("opcion_${jugador.id}"),
                        alTocar = { elegirYCerrar(jugador) },
                    ) {
                        Avatar(jugador.nombre, color, colorSuave)
                        Text(jugador.nombre, style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (elegido != null) {
                    item {
                        HorizontalDivider(
                            Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        FilaOpcion(
                            modifier = Modifier.testTag("opcion_vacia"),
                            alTocar = { elegirYCerrar(null) },
                        ) {
                            Avatar(null, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceContainerHigh)
                            Text(
                                textoQuitar,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FilaOpcion(
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = alTocar)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) { contenido() }
}
