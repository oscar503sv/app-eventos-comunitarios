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
        val tituloLimpio = title.value.trim()
        val descripcionLimpia = description.value.trim()
        val ubicacionLimpia = location.value.trim()
        val fechaSeleccionada = date.value

        if (tituloLimpio.isBlank()) {
            _state.value = EventFormState.Error("El título del evento es obligatorio.")
            return
        }

        if (tituloLimpio.length < 4) {
            _state.value = EventFormState.Error("El título debe tener al menos 4 caracteres.")
            return
        }

        if (ubicacionLimpia.isBlank()) {
            _state.value = EventFormState.Error("La ubicación del evento es obligatoria.")
            return
        }

        if (descripcionLimpia.isBlank()) {
            _state.value = EventFormState.Error("La descripción del evento es obligatoria.")
            return
        }

        if (descripcionLimpia.length < 10) {
            _state.value = EventFormState.Error("La descripción debe tener al menos 10 caracteres.")
            return
        }

        if (category.value.isBlank()) {
            _state.value = EventFormState.Error("Debe seleccionar una categoría para el evento.")
            return
        }

        if (fechaSeleccionada.isBefore(LocalDateTime.now())) {
            _state.value = EventFormState.Error("La fecha y hora del evento no pueden ser anteriores al momento actual.")
            return
        }

        viewModelScope.launch {
            _state.value = EventFormState.Loading

            val isoDate = fechaSeleccionada.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT)

            val result = if (editingEventId.value == null) {
                repository.createEvent(
                    tituloLimpio,
                    descripcionLimpia,
                    isoDate,
                    ubicacionLimpia,
                    category.value
                )
            } else {
                repository.updateEvent(
                    editingEventId.value!!,
                    tituloLimpio,
                    descripcionLimpia,
                    isoDate,
                    ubicacionLimpia,
                    category.value
                )
            }

            when (result) {
                is ApiResult.Success -> _state.value = EventFormState.Success
                is ApiResult.Error -> _state.value = EventFormState.Error(
                    result.message.ifBlank { "No se pudo guardar el evento. Intente nuevamente." }
                )
            }
        }
    }
}
