package com.assistant.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.assistant.ai.ui.screens.MainAssistantScreen
import com.assistant.ai.ui.screens.OnboardingScreen
import com.assistant.ai.ui.screens.SettingsScreen
import com.assistant.ai.ui.theme.SiriAssistantTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SiriAssistantTheme {
                AssistantApp(
                    viewModel = viewModel,
                    onRequestMicPermission = {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onOpenAccessibilitySettings = {
                        viewModel.permissionManager.openAccessibilitySettings()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
}

@Composable
fun AssistantApp(
    viewModel: AssistantViewModel,
    onRequestMicPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val navController = rememberNavController()

    val agentState by viewModel.agentState.collectAsState()
    val sttState by viewModel.sttState.collectAsState()
    val spokenText by viewModel.spokenText.collectAsState()
    val rmsAmplitude by viewModel.rmsAmplitude.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val hasMicPermission by viewModel.hasMicPermission.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()

    val startDestination = if (hasMicPermission && isAccessibilityEnabled) "main" else "onboarding"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("onboarding") {
            OnboardingScreen(
                hasMicPermission = hasMicPermission,
                isAccessibilityEnabled = isAccessibilityEnabled,
                onRequestMicPermission = onRequestMicPermission,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onContinue = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainAssistantScreen(
                agentState = agentState,
                sttState = sttState,
                spokenText = spokenText,
                rmsAmplitude = rmsAmplitude,
                batteryLevel = batteryLevel,
                isAccessibilityEnabled = isAccessibilityEnabled,
                onMicClick = {
                    if (!hasMicPermission) {
                        onRequestMicPermission()
                    } else {
                        viewModel.onMicClick()
                    }
                },
                onStopClick = viewModel::onStopClick,
                onQuickActionClick = viewModel::onQuickActionClick,
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onConfirmAction = viewModel::confirmAction,
                onCancelAction = viewModel::cancelAction
            )
        }

        composable("settings") {
            SettingsScreen(
                settingsState = settingsState,
                isAccessibilityEnabled = isAccessibilityEnabled,
                onVoiceLanguageSelected = viewModel::setVoiceLanguage,
                onLowEndOptimizationToggled = viewModel::setLowEndOptimization,
                onConfirmSensitiveToggled = viewModel::setConfirmSensitiveActions,
                onWakeWordToggled = viewModel::setWakeWordEnabled,
                onContinuousListeningToggled = viewModel::setContinuousListeningEnabled,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
