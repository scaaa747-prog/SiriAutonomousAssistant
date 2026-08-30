package com.assistant.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assistant.ai.agent.AgentState
import com.assistant.ai.stt.SttState
import com.assistant.ai.ui.components.AssistantOrb
import com.assistant.ai.ui.components.AudioWaveformVisualizer
import com.assistant.ai.ui.components.ConfirmationDialog
import com.assistant.ai.ui.components.TopGreetingHeader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAssistantScreen(
    agentState: AgentState,
    sttState: SttState,
    isListening: Boolean,
    spokenText: String,
    lastResponse: String,
    rmsAmplitude: Float,
    batteryLevel: Int,
    isAccessibilityEnabled: Boolean,
    onMicClick: () -> Unit,
    onStopClick: () -> Unit,
    onQuickActionClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onConfirmAction: (String, Map<String, Any?>) -> Unit,
    onCancelAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = agentState is AgentState.Thinking || agentState is AgentState.Acting

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Background Subtle Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // Top Header
                TopGreetingHeader(
                    batteryLevel = batteryLevel,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onOpenSettings = onOpenSettings
                )

                // Center Glowing Orb & State Prompt
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                ) {
                    AssistantOrb(
                        agentState = agentState,
                        onClick = onMicClick
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val statusHeadline = when {
                        isListening -> "Listening..."
                        agentState is AgentState.Thinking -> "Thinking..."
                        agentState is AgentState.Acting -> "Autonomous Execution"
                        agentState is AgentState.Speaking -> "Responding..."
                        agentState is AgentState.NeedsConfirmation -> "Action Required"
                        agentState is AgentState.Error -> "Notice"
                        else -> "How can I help?"
                    }

                    Text(
                        text = statusHeadline,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val subtitleText = when {
                        spokenText.isNotBlank() && isListening -> "\"$spokenText\""
                        agentState is AgentState.Acting -> agentState.stepDescription
                        agentState is AgentState.Speaking -> agentState.message
                        agentState is AgentState.Error -> agentState.message
                        lastResponse.isNotBlank() && agentState is AgentState.Idle -> lastResponse
                        else -> "Tap the microphone or orb to start"
                    }

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Audio Waveform Visualizer
                AnimatedVisibility(
                    visible = isListening,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    AudioWaveformVisualizer(
                        rmsAmplitude = rmsAmplitude,
                        isListening = true,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Suggestion Chips (when Idle and not listening)
                AnimatedVisibility(
                    visible = agentState is AgentState.Idle && !isListening,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf(
                            "Check battery status",
                            "Open Settings",
                            "Set volume 50%",
                            "Wi-Fi Settings"
                        ).forEach { action ->
                            AssistChip(
                                onClick = { onQuickActionClick(action) },
                                label = { Text(action) },
                                modifier = Modifier.padding(4.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mic Action Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop button (visible when busy)
                    if (isBusy || isListening) {
                        FloatingActionButton(
                            onClick = onStopClick,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Cancel")
                        }
                    }

                    // Main mic button
                    FloatingActionButton(
                        onClick = onMicClick,
                        containerColor = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Voice Input",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Sensitive Confirmation Modal
            if (agentState is AgentState.NeedsConfirmation) {
                ConfirmationDialog(
                    actionDescription = agentState.actionDescription,
                    onConfirm = { onConfirmAction(agentState.toolName, agentState.arguments) },
                    onDismiss = onCancelAction
                )
            }
        }
    }
}
