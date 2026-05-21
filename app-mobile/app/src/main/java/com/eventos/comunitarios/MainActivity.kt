package com.eventos.comunitarios

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eventos.comunitarios.ui.auth.AuthState
import com.eventos.comunitarios.ui.auth.AuthViewModel
import com.eventos.comunitarios.ui.auth.LoginScreen
import com.eventos.comunitarios.ui.auth.RegisterScreen
import com.eventos.comunitarios.ui.events.EventsScreen
import com.eventos.comunitarios.ui.events.EventsViewModel
import com.eventos.comunitarios.ui.onboarding.OnboardingScreen
import com.eventos.comunitarios.ui.splash.SplashScreen
import com.eventos.comunitarios.ui.theme.AppEventosComunitariosTheme
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val eventsViewModel: EventsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d("KeyHash", "KeyHash: $hashKey")
            }
        } catch (e: Exception) {
            Log.d("KeyHash", "Error: ${e.message}")
        }

        // Configurar Facebook Login
        val callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    authViewModel.signInWithFacebook(result.accessToken.token)
                }
                override fun onCancel() {}
                override fun onError(error: FacebookException) {
                    Toast.makeText(this@MainActivity,
                        "Error de Facebook: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

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
                        account.idToken?.let { authViewModel.signInWithGoogle(it) }
                    } catch (e: ApiException) {
                        Toast.makeText(this, "Error de Google: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                val facebookLoginLauncher = rememberLauncherForActivityResult(
                    LoginManager.getInstance().createLogInActivityResultContract(callbackManager, null)
                ) { /* resultado procesado por callbackManager */ }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onSplashComplete = {
                                    val destination = when {
                                        FirebaseAuth.getInstance().currentUser != null -> "events"
                                        prefs.getBoolean("onboarding_seen", false) -> "login"
                                        else -> "onboarding"
                                    }
                                    navController.navigate(destination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("onboarding") {
                            OnboardingScreen(
                                onBegin = {
                                    prefs.edit().putBoolean("onboarding_seen", true).apply()
                                    navController.navigate("register") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                },
                                onAlreadyHaveAccount = {
                                    prefs.edit().putBoolean("onboarding_seen", true).apply()
                                    navController.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                authState = authState,
                                onEmailLogin = { email, pass ->
                                    authViewModel.signInWithEmail(email, pass)
                                },
                                onGoogleLogin = {
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                },
                                onFacebookLogin = {
                                    facebookLoginLauncher.launch(listOf("email", "public_profile"))
                                },
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateBack = { navController.popBackStack() },
                                onLoginSuccess = {
                                    navController.navigate("events") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                authState = authState,
                                onRegister = { email, pass, name ->
                                    authViewModel.signUpWithEmail(email, pass, name)
                                },
                                onNavigateToLogin = { navController.navigate("login") },
                                onRegisterSuccess = {
                                    navController.navigate("events") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("events") {
                            LaunchedEffect(Unit) { eventsViewModel.loadEvents() }
                            EventsScreen(
                                state = eventsState,
                                onRefresh = { eventsViewModel.loadEvents() },
                                onLogout = {
                                    authViewModel.signOut()
                                    googleSignInClient.signOut()
                                    LoginManager.getInstance().logOut()
                                    navController.navigate("onboarding") {
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
