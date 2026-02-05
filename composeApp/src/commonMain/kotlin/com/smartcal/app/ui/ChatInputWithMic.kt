package com.smartcal.app.ui

import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.cd_hold_to_talk
import smartcalai.composeapp.generated.resources.cd_listening
import smartcalai.composeapp.generated.resources.cd_microphone_error
import smartcalai.composeapp.generated.resources.cd_transcribing
import smartcalai.composeapp.generated.resources.message_placeholder
import smartcalai.composeapp.generated.resources.release_to_send
import smartcalai.composeapp.generated.resources.send_button
import smartcalai.composeapp.generated.resources.transcribing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartcal.app.utils.TranscribeState
import com.smartcal.app.utils.VoiceTranscriberFactory
import com.smartcal.app.utils.componentsui.Microphone
import com.smartcal.app.utils.componentsui.Send
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ChatInputWithMic(
    factory: VoiceTranscriberFactory,
    messageInput: String,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transcriber = remember { 
        factory.create()
    }
    val transciberState by transcriber.state.collectAsState(TranscribeState.Idle)
    
    // Local state to handle transcribing during delay
    var isLocalTranscribing by remember { mutableStateOf(false) }
    
    // Override transcriber state when we're in local transcribing mode
    val state = if (isLocalTranscribing) TranscribeState.Transcribing else transciberState
    
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    println("DEBUG: ChatInputWithMic rendered - current state: $state")

    // Cleanup transcriber on dispose
    DisposableEffect(transcriber) {
        onDispose {
            transcriber.cancel()
        }
    }

    // Note: We no longer force keyboard hiding to preserve user's keyboard state

    // Row layout with TextField and icons side by side
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Text field without trailing icon
        OutlinedTextField(
            value = messageInput,
            onValueChange = onMessageInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { 
                when (state) {
                    is TranscribeState.Listening -> VoiceWaveAnimation(state = state)
                    is TranscribeState.Transcribing -> TranscribingAnimation()
                    
                    else -> Text(stringResource(Res.string.message_placeholder))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { 
                    keyboardController?.hide()
                    onSendMessage() 
                }
            ),
            enabled = !isLoading && state !is TranscribeState.Transcribing
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Icons outside the text field
        if (messageInput.isNotBlank()) {
            // Show send button when there's text
            SendButton(
                onClick = {
                    keyboardController?.hide()
                    onSendMessage()
                },
                enabled = !isLoading && state !is TranscribeState.Listening && state !is TranscribeState.Transcribing
            )
        } else {
            // Show microphone when text is empty
            MicrophoneButton(
                state = state,
                isEnabled = !isLoading,
                onStartListening = {
                    scope.launch {
                        try {
                            transcriber.startListening(null) // Use device default language
                        } catch (e: Exception) {
                            println("DEBUG: Error starting transcriber: ${e.message}")
                        }
                    }
                },
                onStopListening = {
                    scope.launch {
                        try {
                            // Set local transcribing state to show animation
                            isLocalTranscribing = true
                            val text = transcriber.stopAndGetText()
                            if (!text.isNullOrBlank()) {
                                // Add a forced delay to show the transcribing animation for 2 seconds
                                delay(1500)
                                onMessageInputChange(text)
                            }
                        } catch (e: Exception) {
                            println("DEBUG: Error stopping transcriber: ${e.message}")
                        } finally {
                            // Always reset the local transcribing state
                            isLocalTranscribing = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SendButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (enabled) onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Send,
            contentDescription = stringResource(Res.string.send_button),
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
                .scale(if (enabled) 1f else 0.8f)
        )
    }
}

@Composable
private fun MicrophoneButton(
    state: TranscribeState,
    isEnabled: Boolean = true,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = state is TranscribeState.Listening
    val isTranscribing = state is TranscribeState.Transcribing
    val isError = state is TranscribeState.Error
    

    // Pulse animation for listening state
    val scale = if (isListening) {
        val infiniteTransition = rememberInfiniteTransition(label = "mic_transition")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mic_pulse"
        )
        animatedScale
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .pointerInput(isEnabled) {
                if (isEnabled) {
                    detectTapGestures(
                        onPress = {
                            onStartListening()
                            // Wait for release
                            tryAwaitRelease()
                            onStopListening()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Microphone,
            contentDescription = when {
                isListening -> stringResource(Res.string.cd_listening)
                isTranscribing -> stringResource(Res.string.cd_transcribing)
                isError -> stringResource(Res.string.cd_microphone_error)
                else -> stringResource(Res.string.cd_hold_to_talk)
            },
            tint = when {
                !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                isListening -> MaterialTheme.colorScheme.primary
                isTranscribing -> MaterialTheme.colorScheme.secondary
                isError -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
        )
    }
}

@Composable
private fun TranscribingAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "transcribing")
    val animatedFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.transcribing),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Animated dots
        repeat(3) { index ->
            val delay = index * 200 // Stagger the animation
            val dotAlpha = remember(animatedFloat) {
                val progress = (animatedFloat * 1000 - delay).coerceIn(0f, 300f) / 300f
                if (progress <= 0.5f) {
                    progress * 2f // Fade in
                } else {
                    2f - progress * 2f // Fade out
                }
            }
            
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dotAlpha),
                modifier = Modifier.padding(horizontal = 1.dp)
            )
        }
    }
}

@Composable
private fun VoiceWaveAnimation(
    state: TranscribeState,
    modifier: Modifier = Modifier,
    bars: Int = 12,
    barWidth: Dp = 4.dp,
    gap: Dp = 3.dp,
    minHeight: Dp = 4.dp,
    maxHeight: Dp = 24.dp,
    animationSpeedMs: Int = 1800
) {
    // Get RMS level from state
    val rmsLevel = when (state) {
        is TranscribeState.Listening -> rmsToLevel(state.rms)
        is TranscribeState.Transcribing -> 0.3f
        else -> 0f
    }
    
    // Smooth the level with simpler logic
    var smoothLevel by remember { mutableStateOf(0f) }
    LaunchedEffect(rmsLevel) {
        val alpha = if (rmsLevel > smoothLevel) 0.85f else 0.7f
        smoothLevel = alpha * rmsLevel + (1 - alpha) * smoothLevel
    }

    // Continuous wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "voice-wave")
    val animationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(durationMillis = animationSpeedMs, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "wave-phase"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Modern wave visualizer
        ModernWaveVisualizer(
            voiceLevel = smoothLevel,
            animationPhase = animationPhase,
            bars = bars,
            barWidth = barWidth,
            gap = gap,
            minHeight = minHeight,
            maxHeight = maxHeight,
            color = Color.Black,
            modifier = Modifier.height(maxHeight)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = stringResource(Res.string.release_to_send),
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ModernWaveVisualizer(
    voiceLevel: Float,
    animationPhase: Float,
    bars: Int,
    barWidth: Dp,
    gap: Dp,
    minHeight: Dp,
    maxHeight: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bw = with(density) { barWidth.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val minHeightPx = with(density) { minHeight.toPx() }
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val totalWidth = bars * bw + (bars - 1) * gapPx

    Canvas(
        modifier = modifier.width(with(density) { (totalWidth / density.density).dp })
    ) {
        val centerY = size.height / 2f

        for (i in 0 until bars) {
            // Create a smooth wave pattern across bars
            val normalizedPosition = i.toFloat() / (bars - 1) // 0 to 1
            val waveOffset = sin(animationPhase + normalizedPosition * PI.toFloat() * 2) * 0.5f + 0.5f // 0 to 1
            
            // Base height depends on voice level
            val baseHeight = if (voiceLevel > 0.05f) {
                // Voice detected: use voice level as primary height
                val voiceHeight = voiceLevel * (maxHeightPx - minHeightPx)
                // Add subtle wave variation for natural look
                val waveVariation = waveOffset * voiceLevel * 0.3f * (maxHeightPx - minHeightPx)
                minHeightPx + voiceHeight + waveVariation
            } else {
                // Silence: show gentle breathing animation
                val breathingHeight = waveOffset * 0.15f * (maxHeightPx - minHeightPx)
                minHeightPx + breathingHeight
            }
            
            val barHeight = baseHeight.coerceIn(minHeightPx, maxHeightPx)
            val x = i * (bw + gapPx) + bw / 2f
            
            // Draw rounded bar
            drawLine(
                color = color,
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = bw,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

// Helper function to convert RMS to normalized level
private fun rmsToLevel(rms: Float?): Float {
    if (rms == null) return 0f
    
    // More conservative threshold - only respond to clear voice
    val silenceThreshold = -25f // Higher threshold to avoid false positives
    
    // If RMS is below threshold, treat as silence
    if (rms < silenceThreshold) return 0f
    
    // Map RMS above threshold to [0..1] with more responsive curve
    // Typical range for actual speech: [-25..0]
    val speechRange = 20f // Smaller range for more sensitivity
    val norm = (rms - silenceThreshold) / speechRange
    
    // Use a more responsive curve - between linear and square root
    val normalized = norm.coerceIn(0f, 1f)
    // Approximate x^0.7 using sqrt(sqrt(x)) * sqrt(x) which is roughly x^0.75
    val curved = kotlin.math.sqrt(kotlin.math.sqrt(normalized)) * kotlin.math.sqrt(normalized)
    return curved
}