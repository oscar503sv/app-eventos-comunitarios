package com.eventos.comunitarios.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventos.comunitarios.R
import com.eventos.comunitarios.ui.theme.SplashBg
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBg)
    ) {
        // Puntos decorativos
        Box(
            modifier = Modifier
                .offset(x = 42.dp, y = 210.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 290.dp, end = 65.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 310.dp, start = 55.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
        )

        // Tarjetas flotantes
        DateCard("MAY", "31",
            Modifier.align(Alignment.TopEnd).padding(top = 118.dp, end = 28.dp))
        DateCard("JUL", "15",
            Modifier.align(Alignment.CenterEnd).padding(end = 18.dp, top = 110.dp))
        DateCard("JUN", "02",
            Modifier.align(Alignment.BottomStart).padding(start = 28.dp, bottom = 158.dp))

        // Contenido central
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = SplashBg,
                    modifier = Modifier.size(48.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 13.dp, end = 13.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7B2D2D))
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.app_brand_name), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.splash_tagline), fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))
        }

        // Dots + label inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .size(if (i == 0) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == 0) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.splash_label),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.65f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DateCard(month: String, day: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(month, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Text(day, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

