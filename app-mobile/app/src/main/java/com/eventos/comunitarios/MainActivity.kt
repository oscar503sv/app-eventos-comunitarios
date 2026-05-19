package com.eventos.comunitarios

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eventos.comunitarios.ui.auth.AuthState
import com.eventos.comunitarios.ui.auth.AuthViewModel
import com.eventos.comunitarios.ui.auth.LoginScreen
import com.eventos.comunitarios.ui.auth.RegisterScreen
import com.eventos.comunitarios.ui.events.EventsScreen
import com.eventos.comunitarios.ui.events.EventsViewModel
import com.eventos.comunitarios.ui.theme.AppEventosComunitariosTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val eventsViewModel: EventsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        enableEdgeToEdge()
        setContent {
            AppEventosComunitariosTheme {
                val navController = rememberNavController()
                val authState by authViewModel.authState.collectAsState()
                val eventsState by eventsViewModel.state.collectAsState()

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        account.idToken?.let { idToken ->
                            authViewModel.signInWithGoogle(idToken)
                        }
                    } catch (e: ApiException) {
                        Toast.makeText(this, "Error de Google: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                authState = authState,
                                onEmailLogin = { email, pass -> authViewModel.signInWithEmail(email, pass) },
                                onGoogleLogin = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                                onNavigateToRegister = { navController.navigate("register") },
                                onLoginSuccess = { navController.navigate("events") }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                authState = authState,
                                onRegister = { email, pass, name -> authViewModel.signUpWithEmail(email, pass, name) },
                                onNavigateToLogin = { navController.popBackStack() },
                                onRegisterSuccess = { navController.navigate("events") }
                            )
                        }
                        composable("events") {
                            LaunchedEffect(Unit) {
                                eventsViewModel.loadEvents()
                            }
                            EventsScreen(
                                state = eventsState,
                                onRefresh = { eventsViewModel.loadEvents() },
                                onLogout = {
                                    authViewModel.signOut()
                                    googleSignInClient.signOut()
                                    navController.navigate("login") {
                                        popUpTo("events") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
