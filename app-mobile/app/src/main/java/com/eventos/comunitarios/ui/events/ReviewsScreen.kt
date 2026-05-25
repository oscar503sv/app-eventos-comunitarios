package com.eventos.comunitarios.ui.events

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventos.comunitarios.data.model.ReviewStats
import com.eventos.comunitarios.data.model.ReviewWithUser
import com.eventos.comunitarios.data.model.UserPublic
import com.eventos.comunitarios.ui.theme.AppEventosComunitariosTheme
import com.eventos.comunitarios.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    state: ReviewsState,
    isSubmitting: Boolean,
    eventName: String,
    canReview: Boolean, // True si el evento pasó y el usuario asistió
    onBack: () -> Unit,
    onSubmitReview: (Int, String?) -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text("Reseñas del evento", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            when (state) {
                is ReviewsState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ReviewsState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                        Button(onClick = onRefresh) { Text("Reintentar") }
                    }
                }
                is ReviewsState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        // Rating Summary Section
                        item {
                            RatingSummaryCard(state.stats)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Input Section (Conditional)
                        if (canReview) {
                            item {
                                ReviewInputCard(
                                    eventName = eventName,
                                    isSubmitting = isSubmitting,
                                    onSubmit = onSubmitReview
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }

                        // Reviews List Section
                        item {
                            Text(
                                text = "${state.reviews.size} reseñas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        items(state.reviews) { review ->
                            ReviewItem(review)
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = Color(0xFFF1F1F1)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingSummaryCard(stats: ReviewStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(0.4f)
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f", stats.average),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1B20)
            )
            RatingStars(rating = stats.average.toInt())
            Text(
                text = "${stats.count} reseñas",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Column(
            modifier = Modifier.weight(0.6f).padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Mocking distribution bars as the API doesn't provide them in detail yet
            RatingBar(5, 0.7f)
            RatingBar(4, 0.2f)
            RatingBar(3, 0.05f)
            RatingBar(2, 0.02f)
            RatingBar(1, 0.01f)
        }
    }
}

@Composable
fun RatingBar(stars: Int, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = stars.toString(), fontSize = 12.sp, modifier = Modifier.width(12.dp))
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color(0xFFEADDFF)
        )
    }
}

@Composable
fun ReviewInputCard(
    eventName: String,
    isSubmitting: Boolean,
    onSubmit: (Int, String?) -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6EDFF))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "COMPARTE TU EXPERIENCIA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "¿Cómo fue la $eventName?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1D1B20)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val starIndex = index + 1
                    IconButton(
                        onClick = { rating = starIndex },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (starIndex <= rating) Color(0xFFFFB400) else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Toca para puntuar", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 280) comment = it },
                placeholder = { Text("Tu reseña (opcional)") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
            
            Text(
                text = "${comment.length}/280",
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.End,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onSubmit(rating, comment.ifBlank { null }) },
                    enabled = rating > 0 && !isSubmitting,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Publicar reseña", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: ReviewWithUser) {
    Row(modifier = Modifier.fillMaxWidth()) {
        val initial = review.user.displayName?.take(1)?.uppercase() ?: "U"
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = review.user.displayName ?: "Usuario", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingStars(rating = review.rating, size = 12.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = DateUtils.getTimeAgo(review.createdAt), fontSize = 12.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = review.comment ?: "", fontSize = 14.sp, color = Color(0xFF49454F))
        }
    }
}

@Composable
fun RatingStars(rating: Int, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (index < rating) Color(0xFFFFB400) else Color.Gray,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewsScreenPreview() {
    val mockReviews = listOf(
        ReviewWithUser(
            id = "1",
            userId = "u1",
            eventId = "e1",
            rating = 4,
            comment = "Una verbena espectacular. Mucha gente, buen ambiente y los food trucks fueron geniales.",
            createdAt = "2025-05-15T10:00:00Z",
            user = UserPublic("u1", "Lucía Méndez", null)
        )
    )
    val mockStats = ReviewStats(4.3, 92)
    
    AppEventosComunitariosTheme {
        ReviewsScreen(
            state = ReviewsState.Success(mockReviews, mockStats),
            isSubmitting = false,
            eventName = "Verbena de Primavera",
            canReview = true,
            onBack = {},
            onSubmitReview = { _, _ -> },
            onRefresh = {}
        )
    }
}
