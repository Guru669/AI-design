package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryOak
import com.example.ui.theme.SecondarySage
import com.example.ui.theme.TertiaryClay
import com.example.ui.viewmodel.DesignViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: DesignViewModel) {
    val email by viewModel.authEmailInput.collectAsState()
    val fullName by viewModel.authFullNameInput.collectAsState()
    val preferences by viewModel.authPreferencesInput.collectAsState()
    val isRegisterMode by viewModel.authIsRegisterMode.collectAsState()
    val message by viewModel.authMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Distinctive Architectural Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryOak.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Interior logo icon",
                    tint = PrimaryOak,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Headings
            Text(
                text = "INTERIOR AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = PrimaryOak,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "AI-Powered Interior Design Assistant",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Inputs
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.authEmailInput.value = it },
                label = { Text("Email Address") },
                placeholder = { Text("name@example.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryOak) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOak,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )

            if (isRegisterMode) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { viewModel.authFullNameInput.value = it },
                    label = { Text("Full Name") },
                    placeholder = { Text("John Doe") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryOak) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOak,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )

                OutlinedTextField(
                    value = preferences,
                    onValueChange = { viewModel.authPreferencesInput.value = it },
                    label = { Text("Style Preferences") },
                    placeholder = { Text("e.g. Modern, Minimalist") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = PrimaryOak) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOak,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )
            } else {
                // Password Simulator (not validated, but standard and realistic)
                var fakePassword by remember { mutableStateOf("••••••••") }
                OutlinedTextField(
                    value = fakePassword,
                    onValueChange = { fakePassword = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryOak) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOak,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )
            }

            if (message != null) {
                Text(
                    text = message!!,
                    color = if (message!!.contains("success", true) || message!!.startsWith("Welcome")) SecondarySage else TertiaryClay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }

            // CTA Button
            Button(
                onClick = { viewModel.onAuthenticate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_auth_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOak,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isRegisterMode) "Create Design Account" else "Sign In Safely",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Switcher
            TextButton(
                onClick = { viewModel.toggleAuthMode() },
                modifier = Modifier.testTag("auth_mode_toggle")
            ) {
                Text(
                    text = if (isRegisterMode) "Already have an account? Sign In" else "New to Interior AI? Register profile",
                    color = PrimaryOak,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
