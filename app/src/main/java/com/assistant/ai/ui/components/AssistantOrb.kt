package com.assistant.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.assistant.ai.agent.AgentState
import com.assistant.ai.ui.theme.SiriBlue
import com.assistant.ai.ui.theme.SiriCyan
import com.assistant.ai.ui.theme.SiriGold
import com.assistant.ai.ui.theme.SiriOrange
import com.assistant.ai.ui.theme.SiriPink
import com.assistant.ai.ui.theme.SiriPurple
import com.assistant.ai.ui.theme.SiriViolet

@Composable
fun AssistantOrb(
    agentState: AgentState,
    modifier: Modifier = Modifier,
    orbSize: Dp = 180.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulse"
    )

    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val (primaryColorTarget, secondaryColorTarget) = when (agentState) {
        is AgentState.Idle -> SiriBlue to SiriCyan
        is AgentState.Listening -> SiriCyan to SiriPurple
        is AgentState.Thinking -> SiriPurple to SiriPink
        is AgentState.Acting -> SiriPink to SiriOrange
        is AgentState.Speaking -> SiriBlue to SiriPurple
        is AgentState.NeedsConfirmation -> SiriGold to SiriOrange
        is AgentState.Error -> SiriOrange to SiriViolet
    }

    val primaryColor by animateColorAsState(primaryColorTarget, tween(800), label = "Color1")
    val secondaryColor by animateColorAsState(secondaryColorTarget, tween(800), label = "Color2")

    Box(
        modifier = modifier
            .size(orbSize)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(orbSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * pulseScale

            // Outer Glow Layer
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.6f),
                        secondaryColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.3f
                ),
                radius = baseRadius * 1.3f,
                center = center
            )

            // Inner Animated Core
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, secondaryColor, primaryColor),
                    center = center
                ),
                radius = baseRadius * 0.85f,
                center = center
            )

            // Center Highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(center.x - baseRadius * 0.2f, center.y - baseRadius * 0.2f),
                    radius = baseRadius * 0.5f
                ),
                radius = baseRadius * 0.5f,
                center = Offset(center.x - baseRadius * 0.2f, center.y - baseRadius * 0.2f)
            )
        }
    }
}
