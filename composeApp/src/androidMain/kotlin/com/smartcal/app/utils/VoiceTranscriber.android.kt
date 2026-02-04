package com.smartcal.app.utils

// androidMain
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class VoiceTranscriberFactory(private val activity: ComponentActivity) {
    actual fun create(): VoiceTranscriber = AndroidVoiceTranscriber(activity)
}

private class AndroidVoiceTranscriber(
    private val activity: Activity
) : VoiceTranscriber {
    private val _state = MutableStateFlow<TranscribeState>(TranscribeState.Idle)
    override val state: StateFlow<TranscribeState> = _state

    private var recognizer: SpeechRecognizer? = null
    private var resultDef = CompletableDeferred<String?>()

    override suspend fun startListening(languageTag: String?) {
        println("DEBUG Android: startListening called with languageTag: $languageTag")
        
        // Check permissions first
        val hasPermission = ContextCompat.checkSelfPermission(
            activity, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        println("DEBUG Android: Has microphone permission: $hasPermission")
        
        if (!hasPermission) {
            println("DEBUG Android: Requesting microphone permission")
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 123)
            _state.value = TranscribeState.Error("Microphone permission required")
            return
        }

        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            println("DEBUG Android: Speech recognition not available")
            _state.value = TranscribeState.Error("Speech recognition not available")
            return
        }

        resultDef = CompletableDeferred()
        _state.value = TranscribeState.Listening()
        println("DEBUG Android: State set to Listening")

        recognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {
                    println("DEBUG Android: onReadyForSpeech")
                }
                override fun onBeginningOfSpeech() {
                    println("DEBUG Android: onBeginningOfSpeech")
                }
                override fun onRmsChanged(rmsdB: Float) {
                    // Debug: show when we get meaningful voice levels
                    if (rmsdB > -30f) {
                        println("DEBUG Android RMS: $rmsdB dB")
                    }
                    _state.value = TranscribeState.Listening(rmsdB)
                }
                override fun onBufferReceived(b: ByteArray?) {
                    println("DEBUG Android: onBufferReceived")
                }
                override fun onEndOfSpeech() {
                    println("DEBUG Android: onEndOfSpeech")
                }
                override fun onError(error: Int) {
                    println("DEBUG Android: onError - error code: $error")
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error: $error"
                    }
                    _state.value = TranscribeState.Error(errorMsg)
                    if (!resultDef.isCompleted) resultDef.complete(null)
                }
                override fun onResults(bundle: Bundle) {
                    val results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = results?.firstOrNull()
                    println("DEBUG Android: onResults - text: '$text'")
                    if (!resultDef.isCompleted) resultDef.complete(text)
                }
                override fun onPartialResults(p: Bundle) {
                    val results = p.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = results?.firstOrNull()
                    println("DEBUG Android: onPartialResults - text: '$text'")
                }
                override fun onEvent(t: Int, p: Bundle?) {
                    println("DEBUG Android: onEvent - type: $t")
                }
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            languageTag?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
        }
        
        println("DEBUG Android: Starting speech recognizer")
        recognizer?.startListening(intent)
    }

    override suspend fun stopAndGetText(): String? {
        _state.value = TranscribeState.Transcribing
        recognizer?.stopListening()
        val text = resultDef.await()
        cleanup()
        _state.value = TranscribeState.Idle
        return text
    }

    override fun cancel() {
        recognizer?.cancel()
        cleanup()
        _state.value = TranscribeState.Idle
    }

    private fun cleanup() {
        recognizer?.destroy()
        recognizer = null
    }
}
