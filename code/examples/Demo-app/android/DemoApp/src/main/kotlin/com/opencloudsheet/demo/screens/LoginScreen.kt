package com.opencloudsheet.demo.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencloudsheet.Provider
import com.opencloudsheet.demo.viewmodel.LoginUiState
import com.opencloudsheet.demo.viewmodel.LoginViewModel
import com.opencloudsheet.demo.viewmodel.ProviderInfo

@Preview
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateToWorkBooks: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Welcome to CloudSheet",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect your cloud storage providers",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            when (uiState) {
                is LoginUiState.Checking -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is LoginUiState.Ready,
                is LoginUiState.LoggingIn,
                is LoginUiState.ProviderSignedIn,
                is LoginUiState.LoginFailed -> {
                    val providers = when (uiState) {
                        is LoginUiState.Ready -> (uiState as LoginUiState.Ready).providers
                        is LoginUiState.LoggingIn -> {
                            // Need to get providers from somewhere, let's use a workaround
                            listOf(ProviderInfo(Provider.OneDrive))
                        }
                        is LoginUiState.ProviderSignedIn -> (uiState as LoginUiState.ProviderSignedIn).providers
                        is LoginUiState.LoginFailed -> (uiState as LoginUiState.LoginFailed).providers
                        else -> emptyList()
                    }

                    val loggingInProvider = if (uiState is LoginUiState.LoggingIn) {
                        (uiState as LoginUiState.LoggingIn).provider
                    } else null

                    // Provider Cards
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(providers) { providerInfo ->
                            ProviderCard(
                                providerInfo = providerInfo,
                                isLoggingIn = providerInfo.provider == loggingInProvider,
                                onSignIn = {
                                    when (providerInfo.provider) {
                                        Provider.OneDrive -> activity?.let {
                                            viewModel.loginWithOneDrive(context, it)
                                        }
                                    }
                                }
                            )
                        }

                        // Error message if login failed
                        if (uiState is LoginUiState.LoginFailed) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = (uiState as LoginUiState.LoginFailed).error,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Continue Button
                    Button(
                        onClick = onNavigateToWorkBooks,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = providers.any { it.isSignedIn }
                    ) {
                        Text("Continue", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderCard(
    providerInfo: ProviderInfo,
    isLoggingIn: Boolean,
    onSignIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (providerInfo.isSignedIn) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (providerInfo.provider) {
                            Provider.OneDrive -> "Microsoft OneDrive"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (providerInfo.isSignedIn) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Signed in",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (providerInfo.isSignedIn) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "User: ${providerInfo.userName ?: "Signed in"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sign In Button or Status
            if (isLoggingIn) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (!providerInfo.isSignedIn) {
                Button(
                    onClick = onSignIn,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Sign in")
                }
            }
        }
    }
}