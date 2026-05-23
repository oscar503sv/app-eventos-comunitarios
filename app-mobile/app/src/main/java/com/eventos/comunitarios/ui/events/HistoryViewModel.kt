package com.eventos.comunitarios.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventos.comunitarios.data.network.AttendedEvent
import com.eventos.comunitarios.data.repository.ApiResult
import com.eventos.comunitarios.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HistoryState {
    object Loading : HistoryState()
    data class Success(
        val attendedEvents: List<AttendedEvent>
    ) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

class HistoryViewModel(
    private val repository: EventsRepository = EventsRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val state: StateFlow<HistoryState> = _state

    fun loadHistory() {
        viewModelScope.launch {
            _state.value = HistoryState.Loading

            when (val result = repository.getMyEvents()) {
                is ApiResult.Success -> {
                    _state.value = HistoryState.Success(
                        attendedEvents = result.data.attended
                    )
                }

                is ApiResult.Error -> {
                    _state.value = HistoryState.Error(result.message)
                }
            }
        }
    }
}