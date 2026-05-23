package com.eventos.comunitarios.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eventos.comunitarios.data.model.EventDetail
import com.eventos.comunitarios.data.repository.ApiResult
import com.eventos.comunitarios.data.repository.EventsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EventDetailState {
    object Loading : EventDetailState()
    data class Success(
        val event: EventDetail,
        val isUserAttending: Boolean,
        val isOrganizer: Boolean,
        val isPast: Boolean
    ) : EventDetailState()
    object Deleted : EventDetailState()
    data class Error(val message: String) : EventDetailState()
}

class EventDetailViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository: EventsRepository = EventsRepository()
    private val _state = MutableStateFlow<EventDetailState>(EventDetailState.Loading)
    val state = _state.asStateFlow()

    fun loadEvent(id: String) {
        viewModelScope.launch {
            _state.value = EventDetailState.Loading
            when (val result = repository.getEventById(id)) {
                is ApiResult.Success -> {
                    val event = result.data
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val currentUserId = currentUser?.uid
                    
                    val isAttending = event.attendances.any { 
                        it.user.firebaseUid == currentUserId && it.status == "confirmed" 
                    }
                    val isOrganizer = event.organizer.firebaseUid == currentUserId
                    val isPast = runCatching { 
                        java.time.Instant.parse(event.date).isBefore(java.time.Instant.now()) 
                    }.getOrDefault(false)

                    _state.value = EventDetailState.Success(event, isAttending, isOrganizer, isPast)
                }
                is ApiResult.Error -> {
                    _state.value = EventDetailState.Error(result.message)
                }
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _state.value = EventDetailState.Loading
            when (val result = repository.deleteEvent(eventId)) {
                is ApiResult.Success -> _state.value = EventDetailState.Deleted
                is ApiResult.Error -> _state.value = EventDetailState.Error(result.message)
            }
        }
    }

    fun toggleAttendance(eventId: String) {
        val currentState = _state.value
        if (currentState is EventDetailState.Success) {
            viewModelScope.launch {
                val isAttending = currentState.isUserAttending
                val result = if (isAttending) {
                    repository.cancelAttendance(eventId)
                } else {
                    repository.attend(eventId)
                }

                when (result) {
                    is ApiResult.Success -> {
                        // We reload the event to get the updated lists and counts from the server
                        loadEvent(eventId)
                    }
                    is ApiResult.Error -> {
                        _state.value = EventDetailState.Error(result.message)
                    }
                }
            }
        }
    }
}
