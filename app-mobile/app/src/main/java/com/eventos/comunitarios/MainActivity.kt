package com.eventos.comunitarios

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eventos.comunitarios.notifications.EventMessagingService
import com.eventos.comunitarios.ui.auth.AuthState
import com.eventos.comunitarios.ui.auth.AuthViewModel
import com.eventos.comunitarios.ui.auth.LoginScreen
import com.eventos.comunitarios.ui.auth.RegisterScreen
import com.eventos.comunitarios.ui.events.EventDetailScreen
import com.eventos.comunitarios.ui.events.EventDetailViewModel
import com.eventos.comunitarios.ui.events.EventFormScreen
import com.eventos.comunitarios.ui.events.EventFormState
import com.eventos.comunitarios.ui.events.EventFormViewModel
import com.eventos.comunitarios.ui.events.EventsViewModel
import com.eventos.comunitarios.ui.events.MyEventsViewModel
import com.eventos.comunitarios.ui.main.MainScreen
import com.eventos.comunitarios.ui.onboarding.OnboardingScreen
import com.eventos.comunitarios.ui.profile.ProfileViewModel
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
import com.google.firebase.messaging.FirebaseMessaging
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val message = if (isGranted) {
            "Notificaciones activadas"
        } else {
            "No recibirás recordatorios de eventos"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val authViewModel: AuthViewModel by viewModels()
    private val eventsViewModel: EventsViewModel by viewModels()
    private val myEventsViewModel: MyEventsViewModel by viewModels()
    private val detailViewModel: EventDetailViewModel by viewModels()
    private val formViewModel: EventFormViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private fun askNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun logFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    "FCM_TOKEN",
                    "No fue posible obtener el token FCM",
                    task.exception
                )
                return@addOnCompleteListener
            }

            Log.d("FCM_TOKEN", "Token: ${task.result}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventMessagingService.createNotificationChannel(this)
        askNotificationPermission()
        logFcmToken()

        try {
            val info = packageManager.getPackageInfo(
                packageName,
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            )

            @Suppress("DEPRECATION")
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

        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    authViewModel.signInWithFacebook(result.accessToken.token)
                }

                override fun onCancel() {
                    // El usuario canceló el inicio de sesión.
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error de Facebook: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
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

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

                    try {
                        val account = task.getResult(ApiException::class.java)
                        account.idToken?.let {
                            authViewModel.signInWithGoogle(it)
                        }
                    } catch (e: ApiException) {
                        Toast.makeText(
                            this,
                            "Error de Google: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val facebookLoginLauncher = rememberLauncherForActivityResult(
                    LoginManager.getInstance()
                        .createLogInActivityResultContract(callbackManager, null)
                ) {
                    // El resultado es procesado por callbackManager.
                }

                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
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
                                    popUpTo("splash") {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }

                    composable("onboarding") {
                        OnboardingScreen(
                            onBegin = {
                                prefs.edit()
                                    .putBoolean("onboarding_seen", true)
                                    .apply()

                                navController.navigate("register") {
                                    popUpTo("onboarding") {
                                        inclusive = true
                                    }
                                }
                            },
                            onAlreadyHaveAccount = {
                                prefs.edit()
                                    .putBoolean("onboarding_seen", true)
                                    .apply()

                                navController.navigate("login") {
                                    popUpTo("onboarding") {
                                        inclusive = true
                                    }
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
                                googleSignInLauncher.launch(
                                    googleSignInClient.signInIntent
                                )
                            },
                            onFacebookLogin = {
                                facebookLoginLauncher.launch(
                                    listOf("email", "public_profile")
                                )
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onLoginSuccess = {
                                navController.navigate("events") {
                                    popUpTo("onboarding") {
                                        inclusive = true
                                    }
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
                            onNavigateToLogin = {
                                navController.navigate("login")
                            },
                            onRegisterSuccess = {
                                navController.navigate("events") {
                                    popUpTo("onboarding") {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }

                    composable("events") {
                        LaunchedEffect(Unit) {
                            eventsViewModel.loadEvents()
                        }

                        MainScreen(
                            eventsViewModel = eventsViewModel,
                            myEventsViewModel = myEventsViewModel,
                            profileViewModel = profileViewModel,
                            onEventClick = { id ->
                                detailViewModel.loadEvent(id)
                                navController.navigate("event_detail")
                            },
                            onCreateEvent = {
                                formViewModel.resetForm()
                                navController.navigate("event_form")
                            },
                            onLogout = {
                                authViewModel.signOut()
                                googleSignInClient.signOut()
                                LoginManager.getInstance().logOut()

                                navController.navigate("onboarding") {
                                    popUpTo("events") {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }

                    composable("event_detail") {
                        val detailState by detailViewModel.state.collectAsState()

                        EventDetailScreen(
                            state = detailState,
                            onBack = {
                                navController.popBackStack()
                            },
                            onDelete = { id ->
                                detailViewModel.deleteEvent(id)
                            },
                            onEdit = { event ->
                                formViewModel.loadEventForEdit(event)
                                navController.navigate("event_form")
                            },
                            onToggleAttendance = { id ->
                                detailViewModel.toggleAttendance(id)
                            }
                        )
                    }

                    composable("event_form") {
                        val formState by formViewModel.state.collectAsState()
                        val editingEventId by formViewModel.editingEventId.collectAsState()
                        val title by formViewModel.title.collectAsState()
                        val description by formViewModel.description.collectAsState()
                        val location by formViewModel.location.collectAsState()
                        val date by formViewModel.date.collectAsState()
                        val category by formViewModel.category.collectAsState()

                        LaunchedEffect(formState) {
                            if (formState is EventFormState.Success) {
                                val wasEditingId = formViewModel.editingEventId.value

                                navController.popBackStack()
                                eventsViewModel.loadEvents()

                                if (wasEditingId != null) {
                                    detailViewModel.loadEvent(wasEditingId)
                                }
                            }
                        }

                        EventFormScreen(
                            state = formState,
                            title = title,
                            onTitleChange = {
                                formViewModel.title.value = it
                            },
                            description = description,
                            onDescriptionChange = {
                                formViewModel.description.value = it
                            },
                            location = location,
                            onLocationChange = {
                                formViewModel.location.value = it
                            },
                            date = date,
                            onDateChange = {
                                formViewModel.date.value = it
                            },
                            selectedCategory = category,
                            onCategoryChange = {
                                formViewModel.category.value = it
                            },
                            onClose = {
                                navController.popBackStack()
                            },
                            onSave = {
                                formViewModel.saveEvent()
                            },
                            isEditMode = editingEventId != null
                        )
                    }
                }
            }
        }
    }
}
