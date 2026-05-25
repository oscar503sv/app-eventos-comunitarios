package com.eventos.comunitarios.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.CircleShape
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.eventos.comunitarios.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import com.eventos.comunitarios.data.model.EventCategory
import com.eventos.comunitarios.data.model.EventCounts
import com.eventos.comunitarios.data.model.EventSummary
import com.eventos.comunitarios.data.model.UserPublic
import com.eventos.comunitarios.ui.theme.AppEventosComunitariosTheme
import com.eventos.comunitarios.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    state: EventsState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onEventClick: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onLoadMore: () -> Unit,
    userName: String = "Usuario"
) {
    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val displayName = user?.displayName ?: userName
    val initial = displayName.take(1).uppercase()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars, // Forzar uso solo de barra de estado
                title = {
                    Column {
                        Text(
                            text = "Hola,",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = displayName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp, start = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showLogoutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        DropdownMenu(
                            expanded = showLogoutDialog,
                            onDismissRequest = { showLogoutDialog = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                onClick = {
                                    showLogoutDialog = false
                                    onLogout()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Evento")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFFEF7FF))) {
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
                            Text(stringResource(R.string.events_retry))
                        }
                    }
                }
                is EventsState.Success -> {
                    val listState = rememberLazyListState()
                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val layoutInfo = listState.layoutInfo
                            val totalItemsNumber = layoutInfo.totalItemsCount
                            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

                            lastVisibleItemIndex > (totalItemsNumber - 5) && state.hasMore && !state.isLoadingMore
                        }
                    }

                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value) {
                            onLoadMore()
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            // Search Bar
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    placeholder = { Text("Buscar eventos...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    shape = RoundedCornerShape(28.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color(0xFFECE6F0),
                                        unfocusedContainerColor = Color(0xFFECE6F0)
                                    )
                                )
                            }

                            // Categories Row
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedCategory == null,
                                            onClick = { onCategorySelect(null) },
                                            label = { Text("Todos") },
                                            leadingIcon = if (selectedCategory == null) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                            } else null
                                        )
                                    }
                                    items(EventCategory.all) { category ->
                                        FilterChip(
                                            selected = selectedCategory == category,
                                            onClick = { onCategorySelect(category) },
                                            label = { 
                                                Text(category.lowercase(Locale.getDefault())
                                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) 
                                            },
                                            leadingIcon = if (selectedCategory == category) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }

                            // Section: Upcoming Events
                            if (state.upcomingEvents.isNotEmpty()) {
                                item {
                                    val coroutineScope = rememberCoroutineScope()
                                    val upcomingListState = rememberLazyListState()

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Próximos eventos",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val target = (upcomingListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                                        upcomingListState.animateScrollToItem(target)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Anterior",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val target = (upcomingListState.firstVisibleItemIndex + 1)
                                                            .coerceAtMost(state.upcomingEvents.size - 1)
                                                        upcomingListState.animateScrollToItem(target)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Siguiente",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    LazyRow(
                                        state = upcomingListState,
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(state.upcomingEvents) { event ->
                                            UpcomingEventCard(event, onClick = { onEventClick(event.id) })
                                        }
                                    }
                                }
                            }

                            // Section: Nearby / All
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Explorar eventos",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            items(state.pastEvents + state.upcomingEvents) { event ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    EventListItem(event, onClick = { onEventClick(event.id) })
                                }
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            
                            if (state.upcomingEvents.isEmpty() && state.pastEvents.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillParentMaxSize()) {
                                        Text(
                                            text = "No se encontraron eventos",
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        EventCategory.CULTURA -> Icons.Default.Museum
        EventCategory.MUSICA -> Icons.Default.MusicNote
        EventCategory.DEPORTE -> Icons.Default.SportsSoccer
        EventCategory.EDUCACION -> Icons.Default.School
        EventCategory.GASTRONOMIA -> Icons.Default.Restaurant
        EventCategory.SALUD -> Icons.Default.MedicalServices
        else -> Icons.Default.Event
    }
}

@Composable
fun UpcomingEventCard(event: EventSummary, onClick: () -> Unit) {
    val categoryIcon = getCategoryIcon(event.category)

    Card(
        modifier = Modifier
            .width(248.dp)
            .height(264.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background with Category Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
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
                        .size(150.dp)
                        .offset(x = 20.dp, y = (-10).dp)
                        .rotate(-15f),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }

            // Category Badge - Moved to top-right to avoid overlap with Date Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEADDFF).copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = event.category.lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D)
                )
            }

            // Date Badge (top-left)
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .size(width = 46.dp, height = 54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.96f))
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = DateUtils.getMonth(event.date),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = DateUtils.getDay(event.date),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            // Info Section
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateUtils.formatToShort(event.date),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFCAC4D0))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${event.count.attendances} van",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EventListItem(event: EventSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Date Box
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DateUtils.getMonth(event.date),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = DateUtils.getDay(event.date),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.location,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                
                val isPast = runCatching { 
                    java.time.Instant.parse(event.date).isBefore(java.time.Instant.now()) 
                }.getOrDefault(false)
                val attendanceText = if (isPast) "asistieron" else "asistirán"
                
                Text(
                    text = "${event.category.lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} · ${event.count.attendances} $attendanceText",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
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
            category = "CULTURA",
            organizerId = "user1",
            createdAt = "",
            updatedAt = "",
            organizer = UserPublic("user1", "Ana García", "ana@example.com"),
            count = EventCounts(15, 5)
        )
    )
    AppEventosComunitariosTheme {
        EventsScreen(
            state = EventsState.Success(mockEvents, emptyList()),
            searchQuery = "",
            onSearchQueryChange = {},
            selectedCategory = null,
            onCategorySelect = {},
            onRefresh = {},
            onLogout = {},
            onEventClick = {},
            onCreateEvent = {},
            onLoadMore = {}
        )
    }
}
