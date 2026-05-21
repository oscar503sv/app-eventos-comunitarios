package com.eventos.comunitarios.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventos.comunitarios.R
import com.eventos.comunitarios.ui.theme.AppEventosComunitariosTheme
import com.eventos.comunitarios.ui.theme.AppPrimary
import com.eventos.comunitarios.ui.theme.ScreenBg

private fun passwordStrength(password: String): Pair<Float, String> = when {
    password.length >= 8 && password.any { it.isDigit() } && password.any { it.isUpperCase() } -> 1f to "Fuerte"
    password.length >= 6 -> 0.55f to "Media"
    password.isNotEmpty() -> 0.25f to "Débil"
    else -> 0f to ""
}

private fun strengthColor(label: String): Color = when (label) {
    "Fuerte" -> Color(0xFF2E7D32)
    "Media"  -> Color(0xFFF57C00)
    else     -> Color(0xFFB00020)
}

@Composable
fun RegisterScreen(
    authState: AuthState,
    onRegister: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }

    val (strengthFraction, strengthLabel) = remember(password) { passwordStrength(password) }
    val passwordsMatch = confirmPassword.isNotEmpty() && password == confirmPassword
    val canSubmit = fullName.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && passwordsMatch && termsAccepted &&
            authState !is AuthState.Loading

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onRegisterSuccess()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AppPrimary,
        unfocusedBorderColor = Color(0xFFD0C8E8),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        IconButton(onClick = onNavigateToLogin, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back),
                tint = Color(0xFF1A1A2E))
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(AppPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, null,
                    tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.app_brand_name), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E))
        }

        Spacer(Modifier.height(28.dp))

        Text(stringResource(R.string.register_title), fontSize = 30.sp,
            fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.register_subtitle),
            fontSize = 14.sp, color = Color(0xFF666677))

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(stringResource(R.string.register_full_name)) },
            leadingIcon = {
                Icon(Icons.Default.Person, null,
                    tint = Color(0xFF888899), modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.field_email)) },
            leadingIcon = {
                Icon(Icons.Default.Email, null,
                    tint = Color(0xFF888899), modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = fieldColors,
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.field_password)) },
            leadingIcon = {
                Icon(Icons.Default.Lock, null,
                    tint = Color(0xFF888899), modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = Color(0xFF888899)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors,
            singleLine = true
        )

        if (password.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { strengthFraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = strengthColor(strengthLabel),
                trackColor = Color(0xFFE0D8F0)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.register_password_strength),
                    fontSize = 11.sp, color = Color(0xFF888899))
                Text(strengthLabel, fontSize = 11.sp,
                    color = strengthColor(strengthLabel), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.register_confirm_password)) },
            leadingIcon = {
                Icon(Icons.Default.Lock, null,
                    tint = Color(0xFF888899), modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(
                        if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = Color(0xFF888899)
                    )
                }
            },
            visualTransformation = if (confirmVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors,
            singleLine = true
        )

        if (confirmPassword.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (passwordsMatch) stringResource(R.string.register_passwords_match)
                       else stringResource(R.string.register_passwords_no_match),
                fontSize = 12.sp,
                color = if (passwordsMatch) Color(0xFF2E7D32) else Color(0xFFB00020)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                colors = CheckboxDefaults.colors(checkedColor = AppPrimary)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.register_terms_prefix), fontSize = 13.sp, color = Color(0xFF555566))
            Spacer(Modifier.width(3.dp))
            Text(stringResource(R.string.register_terms_link), fontSize = 13.sp,
                color = AppPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(3.dp))
            Text(stringResource(R.string.register_privacy_mid), fontSize = 13.sp, color = Color(0xFF555566))
            Spacer(Modifier.width(3.dp))
            Text(stringResource(R.string.register_privacy_link), fontSize = 13.sp,
                color = AppPrimary, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { onRegister(email, password, fullName) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppPrimary),
            enabled = canSubmit
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White,
                    modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.register_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (authState is AuthState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = Color(0xFFB00020),
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    AppEventosComunitariosTheme {
        RegisterScreen(
            authState = AuthState.Idle,
            onRegister = { _, _, _ -> },
            onNavigateToLogin = {},
            onRegisterSuccess = {}
        )
    }
}
