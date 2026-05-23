package com.eventos.comunitarios.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eventos.comunitarios.data.model.EventSummary
import com.eventos.comunitarios.data.repository.ApiResult
import com.eventos.comunitarios.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

sealed class EventsState {
    object Loading : EventsState()
    data class Success(
        val upcomingEvents: List<EventSummary>,
        val pastEvents: List<EventSummary>,
        val hasMore: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : EventsState()
    data class Error(val message: String) : EventsState()
}

class EventsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository: EventsRepository = EventsRepository()
    private val _state = MutableStateFlow<EventsState>(EventsState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private var allEvents: List<EventSummary> = emptyList()
    private var currentPage = 1
    private var hasMore = true

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
        applyFilters()
    }

    private fun applyFilters() {
        val query = _searchQuery.value.lowercase()
        val category = _selectedCategory.value
        val now = Instant.now()

        val filtered = allEvents.filter { event ->
            val matchesQuery = event.title.lowercase().contains(query) || 
                              (event.description?.lowercase()?.contains(query) == true)
            val matchesCategory = category == null || event.category == category
            matchesQuery && matchesCategory
        }

        val upcoming = filtered.filter { 
            runCatching { Instant.parse(it.date).isAfter(now) }.getOrDefault(true)
        }.sortedBy { it.date }
        
        val past = filtered.filter { 
            runCatching { Instant.parse(it.date).isBefore(now) }.getOrDefault(false)
        }.sortedByDescending { it.date }
        
        _state.value = EventsState.Success(
            upcomingEvents = upcoming,
            pastEvents = past,
            hasMore = hasMore && _searchQuery.value.isEmpty() && _selectedCategory.value == null
        )
    }

    fun loadEvents(refresh: Boolean = true) {
        if (refresh) {
            currentPage = 1
            hasMore = true
            // If already success, show refreshing state
            val currentState = _state.value
            if (currentState is EventsState.Success) {
                _state.value = currentState.copy(isRefreshing = true)
            } else {
                _state.value = EventsState.Loading
            }
        } else {
            if (!hasMore || _state.value !is EventsState.Success) return
            val currentState = _state.value as EventsState.Success
            if (currentState.isLoadingMore) return
            _state.value = currentState.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            when (val result = repository.getEvents(page = if (refresh) 1 else currentPage + 1)) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (refresh) {
                        allEvents = response.events
                    } else {
                        allEvents = allEvents + response.events
                    }
                    currentPage = response.pagination.page
                    hasMore = response.pagination.hasMore
                    applyFilters()
                }
                is ApiResult.Error -> {
                    if (refresh) {
                        _state.value = EventsState.Error(result.message)
                    } else {
                        // On load more error, just stop loading more state
                        val currentState = _state.value
                        if (currentState is EventsState.Success) {
                            _state.value = currentState.copy(isLoadingMore = false)
                        }
                    }
                }
            }
        }
    }
}
