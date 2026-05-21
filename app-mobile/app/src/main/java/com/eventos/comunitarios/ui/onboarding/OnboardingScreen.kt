package com.eventos.comunitarios.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventos.comunitarios.R
import com.eventos.comunitarios.ui.theme.*

@Composable
fun OnboardingScreen(
    onBegin: () -> Unit,
    onAlreadyHaveAccount: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        EventCardsDecoration(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp).padding( bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, null,
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.app_brand_name), fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
            }

            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.onboarding_tagline),
                fontSize = 15.sp,
                color = Color(0xFF555566),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onBegin,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
            ) {
                Text(stringResource(R.string.onboarding_begin), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAlreadyHaveAccount,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.5.dp, AppPrimary),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
            ) {
                Text(stringResource(R.string.onboarding_have_account), fontSize = 16.sp,
                    color = AppPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EventCardsDecoration(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(top = 48.dp)) {
        // Blob decorativo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 30.dp)
                .size(280.dp, 280.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0D5F5))
        )

        // Tarjeta pequeña superior izquierda
        SmallEventCard(
            month = "MAY", day = "24",
            category = stringResource(R.string.onboarding_category_gastro),
            title = stringResource(R.string.onboarding_event_market),
            dateBg = Color(0xFFFFE4E4),
            modifier = Modifier.align(Alignment.TopStart).offset(y = 15.dp).padding(start = 16.dp)
        )

        // Tarjeta pequeña superior derecha
        SmallEventCard(
            month = "JUN", day = "07",
            category = stringResource(R.string.onboarding_category_talleres),
            title = stringResource(R.string.onboarding_event_ceramica),
            dateBg = Color(0xFFE4EEFF),
            modifier = Modifier.align(Alignment.TopEnd).offset(y = 20.dp).padding(end = 16.dp)
        )

        // Badge DESTACADO
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚡", fontSize = 10.sp)
            Spacer(Modifier.width(2.dp))
            Text(stringResource(R.string.onboarding_badge_featured), fontSize = 9.sp,
                color = AppPrimary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        // Tarjeta destacada inferior central
        FeaturedEventCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp).padding(horizontal = 36.dp)
        )
    }
}

@Composable
private fun SmallEventCard(
    month: String, day: String,
    category: String, title: String,
    dateBg: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(dateBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(month, fontSize = 9.sp, color = Color(0xFF666677), fontWeight = FontWeight.SemiBold)
            Text(day, fontSize = 20.sp, color = Color(0xFF1A1A2E), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(category, fontSize = 9.sp, color = Color(0xFF888899), fontWeight = FontWeight.SemiBold)
            Text(title, fontSize = 12.sp, color = Color(0xFF1A1A2E),
                fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun FeaturedEventCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AppPrimary)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MAY", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
            Text("31", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Row {
                Text(stringResource(R.string.onboarding_category_fiesta), fontSize = 9.sp, color = Color(0xFF888899), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.onboarding_price_free), fontSize = 9.sp, color = AppPrimary, fontWeight = FontWeight.SemiBold)
            }
            Text(stringResource(R.string.onboarding_event_verbena), fontSize = 14.sp, color = Color(0xFF1A1A2E),
                fontWeight = FontWeight.Bold, lineHeight = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.onboarding_event_attendees_demo), fontSize = 10.sp, color = Color(0xFF888899))
        }
    }
}
