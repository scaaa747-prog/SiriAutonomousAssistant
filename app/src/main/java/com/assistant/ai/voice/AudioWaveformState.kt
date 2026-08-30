package com.assistant.ai.voice

import kotlinx.coroutines.flow.StateFlow

interface AudioWaveformState {
    val amplitudeFlow: StateFlow<Float>
}
