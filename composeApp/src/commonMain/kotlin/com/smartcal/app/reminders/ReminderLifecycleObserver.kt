package com.smartcal.app.reminders

import androidx.compose.runtime.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun ReminderLifecycleObserver(
    reminderManager: ReminderManager,
    onSyncReminders: suspend () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    DisposableEffect(lifecycleOwner) {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        // App viene a foreground - sincronizar recordatorios
                        coroutineScope.launch {
                            try {
                                if (reminderManager.isEnabled.value) {
                                    onSyncReminders()
                                    println("🔔 Recordatorios sincronizados al volver a foreground")
                                }
                            } catch (e: Exception) {
                                println("❌ Error sincronizando recordatorios en foreground: ${e.message}")
                            }
                        }
                    }
                    else -> { /* Otros eventos no nos interesan */ }
                }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}