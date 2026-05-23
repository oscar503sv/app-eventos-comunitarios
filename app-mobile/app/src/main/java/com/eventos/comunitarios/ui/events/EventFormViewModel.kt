package com.eventos.comunitarios.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eventos.comunitarios.data.model.EventCategory
import com.eventos.comunitarios.data.repository.ApiResult
import com.eventos.comunitarios.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

sealed class EventFormState {
    object Idle : EventFormState()
    object Loading : EventFormState()
    object Success : EventFormState()
    data class Error(val message: String) : EventFormState()
}

class EventFormViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository: EventsRepository = EventsRepository()
    private val _state = MutableStateFlow<EventFormState>(EventFormState.Idle)
    val state = _state.asStateFlow()

    // Form Fields
    var editingEventId = MutableStateFlow<String?>(null)
    var title = MutableStateFlow("")
    var description = MutableStateFlow("")
    var location = MutableStateFlow("")
    var date = MutableStateFlow<LocalDateTime>(LocalDateTime.now())
    var category = MutableStateFlow(EventCategory.OTRO)

    fun resetForm() {
        editingEventId.value = null
        title.value = ""
        description.value = ""
        location.value = ""
        date.value = LocalDateTime.now()
        category.value = EventCategory.OTRO
        _state.value = EventFormState.Idle
    }

    fun loadEventForEdit(event: com.eventos.comunitarios.data.model.EventDetail) {
        editingEventId.value = event.id
        title.value = event.title
        description.value = event.description ?: ""
        location.value = event.location
        category.value = event.category
        
        runCatching {
            val instant = java.time.Instant.parse(event.date)
            date.value = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        }
        
        _state.value = EventFormState.Idle
    }

    fun saveEvent() {
        if (title.value.isBlank() || location.value.isBlank()) {
            _state.value = EventFormState.Error("Título y ubicación son requeridos")
            return
        }

        viewModelScope.launch {
            _state.value = EventFormState.Loading
            // Convert current local date time to UTC for the server
            val isoDate = date.value.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT)
            
            val result = if (editingEventId.value == null) {
                repository.createEvent(title.value, description.value, isoDate, location.value, category.value)
            } else {
                repository.updateEvent(editingEventId.value!!, title.value, description.value, isoDate, location.value, category.value)
            }

            when (result) {
                is ApiResult.Success -> _state.value = EventFormState.Success
                is ApiResult.Error -> _state.value = EventFormState.Error(result.message)
            }
        }
    }
}
