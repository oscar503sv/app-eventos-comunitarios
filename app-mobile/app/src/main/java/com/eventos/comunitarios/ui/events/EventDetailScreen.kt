package com.eventos.comunitarios.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import com.eventos.comunitarios.data.model.EventCategory
import com.eventos.comunitarios.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    state: EventDetailState,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (com.eventos.comunitarios.data.model.EventDetail) -> Unit,
    onToggleAttendance: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is EventDetailState.Deleted) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state is EventDetailState.Success && state.isOrganizer) {
                        if (!state.isPast) {
                            IconButton(onClick = { onEdit(state.event) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state is EventDetailState.Success && !state.isPast) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Button(
                        onClick = { onToggleAttendance(state.event.id) },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isUserAttending) Color.Gray else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            text = if (state.isUserAttending) "Cancelar asistencia" else "Confirmar asistencia",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (state is EventDetailState.Success && state.isPast) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White
                ) {
                    Text(
                        text = "Este evento ya ha finalizado",
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFFEF7FF))) {
            when (state) {
                is EventDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is EventDetailState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is EventDetailState.Success -> {
                    val event = state.event
                    val categoryIcon = when (event.category) {
                        EventCategory.CULTURA -> Icons.Default.Museum
                        EventCategory.MUSICA -> Icons.Default.MusicNote
                        EventCategory.DEPORTE -> Icons.Default.SportsSoccer
                        EventCategory.EDUCACION -> Icons.Default.School
                        EventCategory.GASTRONOMIA -> Icons.Default.Restaurant
                        EventCategory.SALUD -> Icons.Default.MedicalServices
                        else -> Icons.Default.Event
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header with Category Icon Background
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .clipToBounds(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIcon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(240.dp)
                                        .rotate(-15f)
                                        .offset(x = 40.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                )
                            }
                            
                            // Category Badge on Top of Header
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.BottomStart)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = event.category.lowercase(Locale.getDefault())
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = event.title,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Organizado por ${event.organizer.displayName ?: "Anónimo"}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Date Info
                            DetailInfoRow(
                                icon = Icons.Default.Event,
                                title = DateUtils.formatToFull(event.date),
                                subtitle = "Inicia a las ${DateUtils.formatTime(event.date)} hrs"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Location Info
                            DetailInfoRow(
                                icon = Icons.Default.LocationOn,
                                title = event.location,
                                subtitle = "Ubicación del evento"
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color(0xFFCAC4D0))
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Acerca del evento",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = event.description ?: "Sin descripción disponible.",
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Attendees count
                            Text(
                                text = "Asistentes (${event.attendances.filter { it.status == "confirmed" }.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Eliminar evento") },
                            text = { Text("¿Estás seguro de que deseas eliminar este evento? Esta acción no se puede deshacer.") },
                            confirmButton = {
                                TextButton(
                                    onClick = { 
                                        onDelete(event.id)
                                        showDeleteConfirm = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Eliminar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun DetailInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFECE6F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
