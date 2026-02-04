package com.smartcal.app.utils

import androidx.compose.runtime.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.datetime.*

@Composable
fun rememberCurrentTime(): LocalDateTime {
    val zone = TimeZone.currentSystemDefault()
    var currentTime by remember { mutableStateOf(Clock.System.now().toLocalDateTime(zone)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Update time when app comes to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                        // App comes to foreground - update current time
                        currentTime = Clock.System.now().toLocalDateTime(zone)
                    }
                    else -> { /* Other events not needed */ }
                }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Also update every minute to keep it fresh
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000) // 1 minute
            currentTime = Clock.System.now().toLocalDateTime(zone)
        }
    }
    
    return currentTime
}

@Composable
fun rememberCurrentMinutes(): Int? {
    val currentTime = rememberCurrentTime()
    return currentTime.hour * 60 + currentTime.minute
}

@Composable
fun rememberIsToday(date: LocalDate): Boolean {
    val currentTime = rememberCurrentTime()
    return currentTime.date == date
}