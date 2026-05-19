package com.eventos.comunitarios.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventos.comunitarios.data.model.EventSummary
import com.eventos.comunitarios.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EventsState {
    object Loading : EventsState()
    data class Success(val events: List<EventSummary>) : EventsState()
    data class Error(val message: String) : EventsState()
}

class EventsViewModel : ViewModel() {
    private val _state = MutableStateFlow<EventsState>(EventsState.Loading)
    val state = _state.asStateFlow()

    fun loadEvents() {
        viewModelScope.launch {
            _state.value = EventsState.Loading
            try {
                val response = RetrofitClient.api.getEvents()
                if (response.success) {
                    _state.value = EventsState.Success(response.events)
                } else {
                    _state.value = EventsState.Error("Error al cargar eventos")
                }
            } catch (e: Exception) {
                _state.value = EventsState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
