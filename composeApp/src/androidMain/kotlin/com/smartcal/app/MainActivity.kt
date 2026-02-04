package com.smartcal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.smartcal.app.auth.onAppForegrounded
import com.smartcal.app.utils.ContextProvider
import com.smartcal.app.utils.VoiceTranscriberFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize context provider for browser opening
        ContextProvider.init(this)
        
        setContent {
            App(voiceTranscriberFactory = VoiceTranscriberFactory(this))
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Notify OAuth system that app is in foreground (for cancellation detection)
        onAppForegrounded()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
