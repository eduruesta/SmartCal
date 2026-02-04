package com.smartcal.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.datetime.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.runBlocking
import com.smartcal.app.utils.NotificationStrings

@RequiresApi(Build.VERSION_CODES.O)
class AndroidReminderScheduler(
    private val context: Context
) : ReminderScheduler {

    init {
        ensureReminderChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureReminderChannel() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recordatorios de eventos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones para recordatorios de eventos del calendario"
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            enableLights(true)
            lightColor = android.graphics.Color.BLUE
        }
        notificationManager.createNotificationChannel(channel)
    }

    override suspend fun schedule(instance: ReminderInstance) {
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("nid", instance.notificationId)
            putExtra("title", instance.title)
            putExtra("body", instance.body)
            putExtra("deeplink", instance.deeplink)
            putExtra("eventId", instance.eventId)
            putExtra("eventStartUtcMillis", instance.eventStartUtcMillis)
            putExtra("eventEndUtcMillis", instance.eventEndUtcMillis ?: -1L)
            putExtra("eventTimeZone", instance.eventTimeZone)
            putExtra("isAllDay", instance.isAllDay)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            instance.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                instance.fireAtUtcMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback si no tenemos permisos de alarma exacta
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                instance.fireAtUtcMillis,
                pendingIntent
            )
        }
    }

    override suspend fun cancelByEvent(eventId: String, instances: List<ReminderInstance>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationManager = NotificationManagerCompat.from(context)
        
        instances.forEach { instance ->
            // Cancelar alarma
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                instance.notificationId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
            
            // Cancelar notificación si ya está mostrada
            notificationManager.cancel(instance.notificationId)
        }
    }

    companion object {
        const val CHANNEL_ID = "calendar_reminders"
    }
}

class ReminderReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("nid", (kotlinx.datetime.Clock.System.now().toEpochMilliseconds() % Int.MAX_VALUE).toInt())
        val title = intent.getStringExtra("title") ?: "Evento próximo"
        val body = intent.getStringExtra("body") ?: "Un evento está por comenzar"
        val deeplink = intent.getStringExtra("deeplink")
        val eventId = intent.getStringExtra("eventId")
        val eventStartUtcMillis = intent.getLongExtra("eventStartUtcMillis", 0L)
        val eventEndUtcMillis = intent.getLongExtra("eventEndUtcMillis", -1L)
        val eventTimeZone = intent.getStringExtra("eventTimeZone") ?: "UTC"
        val isAllDay = intent.getBooleanExtra("isAllDay", false)

        // Formatear fechas y horas
        val eventDetails = formatEventDetails(eventStartUtcMillis, eventEndUtcMillis, eventTimeZone, isAllDay)
        val detailedBody = "$body\n$eventDetails"

        // Intent para abrir la aplicación en el evento específico
        val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launchIntent ->
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Agregar el eventId como extra para navegar al evento
            launchIntent.putExtra("eventId", eventId)
            launchIntent.putExtra("fromNotification", true)
            PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        
        // Agregar acciones para las notificaciones
        val actions = mutableListOf<NotificationCompat.Action>()
        
        // Acción "Ver evento"
        contentIntent?.let { viewIntent ->
            actions.add(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_view,
                    runBlocking { NotificationStrings.getViewEventAction() },
                    viewIntent
                ).build()
            )
        }
        
        // Acción adicional para unirse a la reunión si hay deeplink
        if (deeplink != null) {
            val joinIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(deeplink))
            val joinPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 1,
                joinIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_call,
                    runBlocking { NotificationStrings.getJoinMeetingAction() },
                    joinPendingIntent
                ).build()
            )
        }

        val notificationBuilder = NotificationCompat.Builder(context, AndroidReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentText(detailedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(contentIntent, false)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
        
        // Agregar acciones si las hay
        actions.forEach { action ->
            notificationBuilder.addAction(action)
        }
        
        val notification = notificationBuilder.build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Sin permisos de notificación, no hacer nada
        }
    }
    
    private fun formatEventDetails(
        eventStartUtcMillis: Long, 
        eventEndUtcMillis: Long, 
        eventTimeZone: String, 
        isAllDay: Boolean
    ): String {
        return try {
            val timeZone = TimeZone.of(eventTimeZone)
            val startInstant = Instant.fromEpochMilliseconds(eventStartUtcMillis)
            val startLocal = startInstant.toLocalDateTime(timeZone)
            
            if (isAllDay) {
                val dateFormatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                val date = Date(eventStartUtcMillis)
                dateFormatter.format(date)
            } else {
                val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateFormatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                
                val startDate = Date(eventStartUtcMillis)
                val startTime = timeFormatter.format(startDate)
                
                val endTime = if (eventEndUtcMillis > 0) {
                    val endDate = Date(eventEndUtcMillis)
                    " - ${timeFormatter.format(endDate)}"
                } else ""
                
                "${dateFormatter.format(startDate)}\n$startTime$endTime"
            }
        } catch (e: Exception) {
            "Evento programado"
        }
    }

}