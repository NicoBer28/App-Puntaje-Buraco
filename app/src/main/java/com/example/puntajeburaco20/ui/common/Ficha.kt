package com.example.puntajeburaco20.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Una ficha de Buraco: número arriba y el circulito de abajo, como las de verdad. */
@Composable
fun Ficha(numero: Int, color: Color, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier
            .size(width = 60.dp, height = 82.dp)
            .shadow(10.dp, forma, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Color(0xFFFBFCFE), forma)
            .border(1.dp, Color(0xFFDCE3EE), forma),
    ) {
        Text(
            numero.toString(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 36.sp,
            color = color,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(12.dp)
                .border(2.5.dp, color, CircleShape),
        )
    }
}
