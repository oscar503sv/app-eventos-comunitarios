package com.eventos.comunitarios.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eventos.comunitarios.data.model.UserProfile
import com.eventos.comunitarios.data.repository.ApiResult
import com.eventos.comunitarios.data.repository.EventsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = EventsRepository()
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state = _state.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            when (val result = repository.getProfile()) {
                is ApiResult.Success -> {
                    _state.value = ProfileState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _state.value = ProfileState.Error(result.message)
                }
            }
        }
    }

    fun updateDisplayName(newName: String) {
        if (newName.isBlank()) return
        
        viewModelScope.launch {
            // Keep current profile but show loading if possible, 
            // or just trigger the call and reload on success.
            when (val result = repository.updateProfile(newName)) {
                is ApiResult.Success -> {
                    // Refrescar el usuario local de Firebase para que el resto de la app
                    // (como el feed) vea el nuevo nombre inmediatamente.
                    runCatching {
                        FirebaseAuth.getInstance().currentUser?.reload()?.await()
                    }
                    _state.value = ProfileState.Success(result.data)
                }
                is ApiResult.Error -> {
                    // We could add an update error state, but for now we just log/ignore 
                    // or keep the previous success state.
                }
            }
        }
    }
}
