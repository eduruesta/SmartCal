package com.smartcal.app

import androidx.compose.ui.window.ComposeUIViewController
import com.smartcal.app.di.appModule
import com.smartcal.app.utils.VoiceTranscriberFactory
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController { 
    // Initialize Koin for iOS
    startKoin {
        modules(appModule)
    }
    
    App(voiceTranscriberFactory = VoiceTranscriberFactory()) 
}