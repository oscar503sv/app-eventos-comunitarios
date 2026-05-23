package com.eventos.comunitarios.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eventos.comunitarios.data.model.Event
import com.eventos.comunitarios.data.repository.ApiResult
import com.eventos.comunitarios.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

sealed class MyEventsState {
    object Loading : MyEventsState()
    data class Success(
        val upcoming: List<Event>,
        val past: List<Event>
    ) : MyEventsState()
    data class Error(val message: String) : MyEventsState()
}

class MyEventsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = EventsRepository()
    private val _state = MutableStateFlow<MyEventsState>(MyEventsState.Loading)
    val state = _state.asStateFlow()

    fun loadMyEvents() {
        viewModelScope.launch {
            _state.value = MyEventsState.Loading
            when (val result = repository.getMyEvents()) {
                is ApiResult.Success -> {
                    val organized = result.data.organized
                    val now = Instant.now()
                    
                    val upcoming = organized.filter { 
                        runCatching { Instant.parse(it.date).isAfter(now) }.getOrDefault(true)
                    }.sortedBy { it.date }
                    
                    val past = organized.filter { 
                        runCatching { Instant.parse(it.date).isBefore(now) }.getOrDefault(false)
                    }.sortedByDescending { it.date }
                    
                    _state.value = MyEventsState.Success(upcoming, past)
                }
                is ApiResult.Error -> {
                    _state.value = MyEventsState.Error(result.message)
                }
            }
        }
    }
}
