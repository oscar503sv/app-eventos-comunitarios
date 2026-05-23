package com.eventos.comunitarios.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eventos.comunitarios.data.network.AttendedEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryState,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi historial") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar historial")
                    }
                }
            )
        }
    ) { padding ->
        when (state) {
            is HistoryState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HistoryState.Error -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No se pudo cargar el historial",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = onRefresh) {
                        Text("Intentar nuevamente")
                    }
                }
            }

            is HistoryState.Success -> {
                val eventos = state.attendedEvents

                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Historial de participación",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Aquí puedes ver los eventos en los que has confirmado asistencia y un resumen de tu participación.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    item {
                        ParticipationStatsCard(
                            totalEventos = eventos.size,
                            eventosAsistidos = eventos.count { it.status.equals("confirmed", ignoreCase = true) },
                            eventosPasados = eventos.size
                        )
                    }

                    item {
                        Text(
                            text = "Eventos registrados",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (eventos.isEmpty()) {
                        item {
                            EmptyHistoryCard()
                        }
                    } else {
                        items(eventos) { attendedEvent ->
                            HistoryEventCard(attendedEvent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipationStatsCard(
    totalEventos: Int,
    eventosAsistidos: Int,
    eventosPasados: Int
) {
    val porcentaje = if (totalEventos > 0) {
        (eventosAsistidos * 100) / totalEventos
    } else {
        0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row {
                Icon(Icons.Default.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Estadísticas de participación",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text("Total de eventos registrados: $totalEventos")
            Text("Eventos con asistencia confirmada: $eventosAsistidos")
            Text("Eventos en historial: $eventosPasados")
            Text("Participación aproximada: $porcentaje%")
        }
    }
}

@Composable
fun HistoryEventCard(attendedEvent: AttendedEvent) {
    val event = attendedEvent.event

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = event.description ?: "Sin descripción",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Ubicación: ${event.location}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Fecha: ${event.date}",
                style = MaterialTheme.typography.bodySmall
            )

            val estadoTexto = when (attendedEvent.status.lowercase()) {
                "confirmed" -> "Confirmada"
                "cancelled" -> "Cancelada"
                else -> attendedEvent.status
            }

            Text(
                text = "Estado de asistencia: $estadoTexto",
                style = MaterialTheme.typography.bodySmall
            )

            Row {
                Icon(Icons.Default.EventAvailable, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registro de asistencia guardado",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Todavía no tienes eventos en tu historial.",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cuando confirmes asistencia a un evento, aparecerá en esta sección.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}