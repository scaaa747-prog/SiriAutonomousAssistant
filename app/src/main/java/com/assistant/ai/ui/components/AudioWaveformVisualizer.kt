package com.assistant.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.assistant.ai.ui.theme.SiriBlue
import com.assistant.ai.ui.theme.SiriCyan
import com.assistant.ai.ui.theme.SiriPink

@Composable
fun AudioWaveformVisualizer(
    rmsAmplitude: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val barCount = 24
        val totalWidth = size.width
        val barWidth = 6.dp.toPx()
        val spacing = (totalWidth - (barCount * barWidth)) / (barCount + 1)
        val centerY = size.height / 2f

        val activeAmp = if (isListening) rmsAmplitude.coerceIn(0.1f, 1f) else 0.05f

        for (i in 0 until barCount) {
            val multiplier = Math.sin((i.toDouble() / barCount) * Math.PI + (phase * Math.PI)).toFloat()
            val barHeight = (size.height * 0.8f * activeAmp * multiplier).coerceAtLeast(8f)

            val startX = spacing + i * (barWidth + spacing)
            val startY = centerY - (barHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(SiriCyan, SiriBlue, SiriPink)
                ),
                topLeft = Offset(startX, startY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
