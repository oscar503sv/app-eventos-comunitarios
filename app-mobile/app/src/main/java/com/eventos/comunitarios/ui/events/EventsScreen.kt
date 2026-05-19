package com.eventos.comunitarios.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.eventos.comunitarios.data.model.EventCounts
import com.eventos.comunitarios.data.model.EventSummary
import com.eventos.comunitarios.data.model.UserPublic
import com.eventos.comunitarios.ui.theme.AppEventosComunitariosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    state: EventsState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eventos Comunitarios") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión", tint = Color.Red)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is EventsState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is EventsState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Reintentar")
                        }
                    }
                }
                is EventsState.Success -> {
                    if (state.events.isEmpty()) {
                        Text(
                            text = "No hay eventos disponibles",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.events) { event ->
                                EventCard(event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(event: EventSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = event.location, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.description ?: "Sin descripción", maxLines = 2)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Por: ${event.organizer.displayName ?: "Anónimo"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${event.count.attendances} Asistentes",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventsScreenPreview() {
    val mockEvents = listOf(
        EventSummary(
            id = "1",
            title = "Taller de Fotografía",
            description = "Aprende a usar tu cámara profesional en este taller intensivo.",
            date = "2025-06-15T10:00:00Z",
            location = "Parque Central",
            organizerId = "user1",
            createdAt = "",
            updatedAt = "",
            organizer = UserPublic("user1", "Ana García", "ana@example.com"),
            count = EventCounts(15, 5)
        ),
        EventSummary(
            id = "2",
            title = "Feria Gastronómica",
            description = "Ven a probar los mejores platillos de la región.",
            date = "2025-06-20T12:00:00Z",
            location = "Plaza Mayor",
            organizerId = "user2",
            createdAt = "",
            updatedAt = "",
            organizer = UserPublic("user2", "Carlos Ruiz", "carlos@example.com"),
            count = EventCounts(50, 12)
        )
    )
    AppEventosComunitariosTheme {
        EventsScreen(
            state = EventsState.Success(mockEvents),
            onRefresh = {},
            onLogout = {}
        )
    }
}
