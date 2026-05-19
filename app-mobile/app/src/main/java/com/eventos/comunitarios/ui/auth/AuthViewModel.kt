package com.eventos.comunitarios.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String, val code: Int? = null) : AuthState()
}

interface IAuthViewModel {
    val authState: StateFlow<AuthState>
    fun signInWithEmail(email: String, pass: String)
    fun signUpWithEmail(email: String, pass: String, fullName: String)
    fun signInWithGoogle(idToken: String)
    fun signOut()
    fun resetState()
}

class AuthViewModel : ViewModel(), IAuthViewModel {
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState = _authState.asStateFlow()

    override fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authState.value = AuthState.Success(auth.currentUser)
                    } else {
                        _authState.value = AuthState.Error(task.exception?.message ?: "Login failed")
                    }
                }
        }
    }

    override fun signUpWithEmail(email: String, pass: String, fullName: String) {
        if (email.isBlank() || pass.isBlank() || fullName.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        
                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    _authState.value = AuthState.Success(auth.currentUser)
                                } else {
                                    // Aunque falle el perfil, el usuario se creó, pero notificamos error
                                    _authState.value = AuthState.Error(profileTask.exception?.message ?: "Failed to set display name")
                                }
                            }
                    } else {
                        _authState.value = AuthState.Error(task.exception?.message ?: "Registration failed")
                    }
                }
        }
    }

    override fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authState.value = AuthState.Success(auth.currentUser)
                    } else {
                        _authState.value = AuthState.Error(task.exception?.message ?: "Google Sign-In failed")
                    }
                }
        }
    }

    override fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    override fun resetState() {
        _authState.value = AuthState.Idle
    }
}
