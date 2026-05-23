package com.eventos.comunitarios.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.eventos.comunitarios.ui.events.*
import com.eventos.comunitarios.ui.profile.ProfileScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Inicio : BottomNavItem("home", Icons.Default.Home, "Inicio")
    object MisEventos : BottomNavItem("my_events", Icons.Default.Event, "Mis eventos")
    object Historial : BottomNavItem("history", Icons.Default.History, "Historial")
    object Perfil : BottomNavItem("profile", Icons.Default.Person, "Perfil")
}

@Composable
fun MainScreen(
    eventsViewModel: EventsViewModel,
    myEventsViewModel: MyEventsViewModel,
    profileViewModel: com.eventos.comunitarios.ui.profile.ProfileViewModel,
    onEventClick: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedItem by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Inicio) }
    val items = listOf(
        BottomNavItem.Inicio,
        BottomNavItem.MisEventos,
        BottomNavItem.Historial,
        BottomNavItem.Perfil
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Deshabilitar insets automáticos del Scaffold
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedItem == item,
                        onClick = { selectedItem = item }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedItem) {
                BottomNavItem.Inicio -> {
                    val state by eventsViewModel.state.collectAsState()
                    val searchQuery by eventsViewModel.searchQuery.collectAsState()
                    val selectedCategory by eventsViewModel.selectedCategory.collectAsState()
                    
                    EventsScreen(
                        state = state,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { eventsViewModel.onSearchQueryChange(it) },
                        selectedCategory = selectedCategory,
                        onCategorySelect = { eventsViewModel.onCategorySelect(it) },
                        onRefresh = { eventsViewModel.loadEvents() },
                        onLogout = onLogout,
                        onEventClick = onEventClick,
                        onCreateEvent = onCreateEvent,
                        onLoadMore = { eventsViewModel.loadEvents(refresh = false) }
                    )
                }
                BottomNavItem.MisEventos -> {
                    val state by myEventsViewModel.state.collectAsState()
                    LaunchedEffect(Unit) { myEventsViewModel.loadMyEvents() }
                    MyEventsScreen(
                        state = state,
                        onEventClick = onEventClick,
                        onCreateEvent = onCreateEvent,
                        onLogout = onLogout,
                        onRefresh = { myEventsViewModel.loadMyEvents() }
                    )
                }
                BottomNavItem.Historial -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Historial: Próximamente disponible")
                    }
                }
                BottomNavItem.Perfil -> {
                    val state by profileViewModel.state.collectAsState()
                    LaunchedEffect(Unit) { profileViewModel.loadProfile() }
                    ProfileScreen(
                        state = state,
                        onLogout = onLogout,
                        onUpdateName = { profileViewModel.updateDisplayName(it) },
                        onRefresh = { profileViewModel.loadProfile() }
                    )
                }
            }
        }
    }
}
