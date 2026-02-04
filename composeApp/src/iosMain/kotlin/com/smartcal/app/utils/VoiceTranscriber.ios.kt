@file:OptIn(ExperimentalForeignApi::class)

package com.smartcal.app.utils

// iosMain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFAudio.*
import platform.Foundation.*
import platform.Speech.*
import kotlinx.cinterop.*
import kotlin.math.ln
import kotlin.math.sqrt
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private fun log10(x: Double): Double = ln(x) / ln(10.0)

// Mapea dBFS [-60, 0] a [0,1]
fun dbfsToLevel(dbfs: Float, floorDb: Float = -60f, ceilDb: Float = 0f): Float {
    val clamped = dbfs.coerceIn(floorDb, ceilDb)
    return (clamped - floorDb) / (ceilDb - floorDb)
}

// RMS de un buffer Float32 mono o estéreo (promedia canales)
private fun rmsFromPcmBufferFloat32(buffer: AVAudioPCMBuffer): Float {
    val chCount = buffer.format.channelCount.toInt()
    val frameCount = buffer.frameLength.toInt()
    if (frameCount == 0 || chCount == 0) return 0f

    val channels = buffer.floatChannelData ?: return 0f
    var sumSq = 0.0
    // Promediamos canales sumando energía de todos
    for (c in 0 until chCount) {
        val ptr = channels[c]!!
        // Leer frameCount floats
        for (i in 0 until frameCount) {
            val sample = ptr[i].toDouble()
            sumSq += sample * sample
        }
    }
    val n = frameCount * chCount
    val rms = sqrt(sumSq / n)
    return rms.toFloat().coerceIn(0f, 1f)
}

private fun rmsToDbfs(rms: Float): Float {
    // Evitar log10(0)
    val safe = if (rms <= 1e-9f) 1e-9f else rms
    return (20.0 * log10(safe.toDouble())).toFloat() // ~0 = full scale
}

actual class VoiceTranscriberFactory {
    actual fun create(): VoiceTranscriber = IOSVoiceTranscriber()
}

private class IOSVoiceTranscriber : VoiceTranscriber {
    private val _state = MutableStateFlow<TranscribeState>(TranscribeState.Idle)
    override val state: StateFlow<TranscribeState> = _state

    private val scope = MainScope()
    private val audioEngine = AVAudioEngine()
    private var recognizer: SFSpeechRecognizer? = SFSpeechRecognizer(NSLocale.currentLocale())
    private var request: SFSpeechAudioBufferRecognitionRequest? = null
    private var task: SFSpeechRecognitionTask? = null
    private var finalText = CompletableDeferred<String?>()

    override suspend fun startListening(languageTag: String?) {
        // Clean up any existing state first
        if (audioEngine.isRunning()) {
            cleanup()
        }
        
        // Autorizaciones
        SFSpeechRecognizer.requestAuthorization { /* check statuses if needed */ }
        val sess = AVAudioSession.sharedInstance()
        memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            sess.setCategory(AVAudioSessionCategoryRecord, error = err.ptr)
            sess.setMode(AVAudioSessionModeMeasurement, error = err.ptr)
            sess.setActive(true, error = err.ptr)
        }

        _state.value = TranscribeState.Listening()

        request = SFSpeechAudioBufferRecognitionRequest().apply {
            shouldReportPartialResults = false
        }
        // Force Spanish locale for Argentina
        val spanishLocale = NSLocale.localeWithLocaleIdentifier("es-AR")
        recognizer = SFSpeechRecognizer(spanishLocale)
        println("DEBUG: Using Spanish locale es-AR instead of default ${NSLocale.currentLocale().localeIdentifier}")

        finalText = CompletableDeferred()
        task = recognizer?.recognitionTaskWithRequest(request!!, resultHandler = { result, error ->
            if (error != null) {
                _state.value = TranscribeState.Error(error.localizedDescription ?: "Speech error")
                if (!finalText.isCompleted) finalText.complete(null)
            } else if (result != null && result.isFinal()) {
                if (!finalText.isCompleted) finalText.complete(result.bestTranscription.formattedString)
            }
        })

        val input = audioEngine.inputNode
        val format = input.outputFormatForBus(0u)
        
        // Make sure there's no existing tap before installing a new one
        try {
            input.removeTapOnBus(0u)
        } catch (e: Exception) {
            // Ignore if no tap exists
        }
        
        // Install tap with proper RMS calculation
        input.installTapOnBus(0u, 1024u, format) { buffer, _ ->
            buffer?.let {
                // Calculate RMS using proper method
                val rms: Float = rmsFromPcmBufferFloat32(buffer)
                
                val dbfs = rmsToDbfs(rms)                  // ~[-inf, 0]
                val level = dbfsToLevel(dbfs, -40f, 0f)    // [0..1], recorte a -40 dBFS para ser más conservador

                // Update state with normalized level
                scope.launch {
                    // Debug: print RMS values to understand the range
                    if (level > 0.05f) { // Only print when there's meaningful audio
                        println("DEBUG iOS RMS: raw=$rms, dbfs=$dbfs, normalized=$level")
                    }
                    _state.value = TranscribeState.Listening(rms = level)
                }

                // Pass audio to Speech Recognition
                request?.appendAudioPCMBuffer(it)
            }
        }
        
        audioEngine.prepare()
        memScoped {
            val err2 = alloc<ObjCObjectVar<NSError?>>()
            audioEngine.startAndReturnError(err2.ptr)
        }
    }

    override suspend fun stopAndGetText(): String? {
        _state.value = TranscribeState.Transcribing
        
        if (audioEngine.isRunning()) {
            audioEngine.stop()
        }
        request?.endAudio()
        val text = finalText.await()
        cleanup()
        _state.value = TranscribeState.Idle
        return text
    }

    override fun cancel() {
        if (audioEngine.isRunning()) {
            audioEngine.stop()
        }
        task?.cancel()
        cleanup()
        _state.value = TranscribeState.Idle
    }

    private fun cleanup() {
        try {
            audioEngine.inputNode.removeTapOnBus(0u)
        } catch (e: Exception) {
            // Ignore if no tap exists or engine is not running
        }
        task = null
        request = null
    }
}