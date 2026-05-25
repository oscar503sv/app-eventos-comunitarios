package com.eventos.comunitarios.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventos.comunitarios.data.model.ReviewStats
import com.eventos.comunitarios.data.model.ReviewWithUser
import com.eventos.comunitarios.data.repository.ApiResult
import com.eventos.comunitarios.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReviewsState {
    object Loading : ReviewsState()
    data class Success(
        val reviews: List<ReviewWithUser>,
        val stats: ReviewStats
    ) : ReviewsState()
    data class Error(val message: String) : ReviewsState()
}

class ReviewsViewModel(
    private val repository: EventsRepository = EventsRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<ReviewsState>(ReviewsState.Loading)
    val state = _state.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    fun loadReviews(eventId: String) {
        viewModelScope.launch {
            _state.value = ReviewsState.Loading
            when (val result = repository.getReviews(eventId)) {
                is ApiResult.Success -> {
                    _state.value = ReviewsState.Success(result.data.reviews, result.data.stats)
                }
                is ApiResult.Error -> {
                    _state.value = ReviewsState.Error(result.message)
                }
            }
        }
    }

    fun submitReview(eventId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.createReview(eventId, rating, comment)) {
                is ApiResult.Success -> {
                    // Recargar reseñas tras éxito para actualizar la lista y estadísticas
                    loadReviews(eventId)
                }
                is ApiResult.Error -> {
                    // Podríamos manejar un estado de error específico para el envío si fuera necesario,
                    // por ahora simplemente dejamos de cargar.
                }
            }
            _isSubmitting.value = false
        }
    }
}
