package com.smartcal.app.utils

// commonMain
import kotlinx.coroutines.flow.StateFlow

interface VoiceTranscriber {
    val state: StateFlow<TranscribeState> // Idle, Listening(level), Transcribing, Error
    suspend fun startListening(languageTag: String? = null)   // ej: "es-AR"
    suspend fun stopAndGetText(): String?                     // resultado final
    fun cancel()
}

sealed interface TranscribeState {
    object Idle : TranscribeState
    data class Listening(val rms: Float? = null) : TranscribeState
    object Transcribing : TranscribeState
    data class Error(val message: String) : TranscribeState
}

expect class VoiceTranscriberFactory {
    fun create(): VoiceTranscriber
}
