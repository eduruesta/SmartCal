package com.smartcal.app.ui.components

import smartcal.composeapp.generated.resources.Res
import smartcal.composeapp.generated.resources.cancel_button
import smartcal.composeapp.generated.resources.custom_reminder
import smartcal.composeapp.generated.resources.hours
import smartcal.composeapp.generated.resources.minutes
import smartcal.composeapp.generated.resources.ok_button
import smartcal.composeapp.generated.resources.reminder_10_minutes
import smartcal.composeapp.generated.resources.reminder_15_minutes
import smartcal.composeapp.generated.resources.reminder_1_day
import smartcal.composeapp.generated.resources.reminder_1_hour
import smartcal.composeapp.generated.resources.reminder_1_week
import smartcal.composeapp.generated.resources.reminder_2_hours
import smartcal.composeapp.generated.resources.reminder_30_minutes
import smartcal.composeapp.generated.resources.reminder_5_minutes
import smartcal.composeapp.generated.resources.reminder_at_time
import smartcal.composeapp.generated.resources.reminder_custom
import smartcal.composeapp.generated.resources.reminder_none
import smartcal.composeapp.generated.resources.set_reminder_time
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

data class ReminderOption(
    val minutes: Int?,
    val titleKey: String,
    val isCustom: Boolean = false
)

data class ReminderSelection(
    val minutes: Int?,
    val method: String = "popup" // "popup" or "email"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPicker(
    selectedMinutes: Int?,
    selectedMethod: String = "popup",
    onReminderSelected: (ReminderSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    val reminderOptions = listOf(
        ReminderOption(null, "reminder_none"),
        ReminderOption(0, "reminder_at_time"),
        ReminderOption(5, "reminder_5_minutes"),
        ReminderOption(10, "reminder_10_minutes"),
        ReminderOption(15, "reminder_15_minutes"),
        ReminderOption(30, "reminder_30_minutes"),
        ReminderOption(60, "reminder_1_hour"),
        ReminderOption(120, "reminder_2_hours"),
        ReminderOption(1440, "reminder_1_day"),
        ReminderOption(10080, "reminder_1_week"),
        ReminderOption(-1, "reminder_custom", true)
    )

    var showCustomDialog by remember { mutableStateOf(false) }
    var customHours by remember { mutableStateOf("") }
    var customMinutes by remember { mutableStateOf("") }
    var currentMethod by remember { mutableStateOf(selectedMethod) }

    Column(modifier = modifier) {
        Text(
            text = "Reminder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Method selection
        Text(
            text = "Method",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { currentMethod = "popup" },
                shape = RoundedCornerShape(8.dp),
                color = if (currentMethod == "popup") MaterialTheme.colorScheme.primaryContainer
                       else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp, 
                    if (currentMethod == "popup") MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outline
                )
            ) {
                Text(
                    text = "Popup",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentMethod == "popup") MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurface
                )
            }
            
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { currentMethod = "email" },
                shape = RoundedCornerShape(8.dp),
                color = if (currentMethod == "email") MaterialTheme.colorScheme.primaryContainer
                       else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp, 
                    if (currentMethod == "email") MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outline
                )
            ) {
                Text(
                    text = "Email",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentMethod == "email") MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(reminderOptions) { option ->
                ReminderOptionItem(
                    option = option,
                    isSelected = when {
                        option.minutes == null -> selectedMinutes == null
                        option.isCustom -> selectedMinutes != null && reminderOptions.none { it.minutes == selectedMinutes }
                        else -> option.minutes == selectedMinutes
                    },
                    onClick = {
                        if (option.isCustom) {
                            showCustomDialog = true
                        } else {
                            onReminderSelected(ReminderSelection(option.minutes, currentMethod))
                        }
                    }
                )
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(stringResource(Res.string.custom_reminder)) },
            text = {
                Column {
                    Text(stringResource(Res.string.set_reminder_time))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customHours,
                            onValueChange = { customHours = it },
                            label = { Text(stringResource(Res.string.hours)) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = customMinutes,
                            onValueChange = { customMinutes = it },
                            label = { Text(stringResource(Res.string.minutes)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hours = customHours.toIntOrNull() ?: 0
                        val minutes = customMinutes.toIntOrNull() ?: 0
                        val totalMinutes = hours * 60 + minutes
                        onReminderSelected(ReminderSelection(totalMinutes, currentMethod))
                        showCustomDialog = false
                        customHours = ""
                        customMinutes = ""
                    }
                ) {
                    Text(stringResource(Res.string.ok_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(stringResource(Res.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun ReminderOptionItem(
    option: ReminderOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getReminderDisplayText(option),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun getReminderDisplayText(option: ReminderOption): String {
    return when (option.titleKey) {
        "reminder_none" -> stringResource(Res.string.reminder_none)
        "reminder_at_time" -> stringResource(Res.string.reminder_at_time)
        "reminder_5_minutes" -> stringResource(Res.string.reminder_5_minutes)
        "reminder_10_minutes" -> stringResource(Res.string.reminder_10_minutes)
        "reminder_15_minutes" -> stringResource(Res.string.reminder_15_minutes)
        "reminder_30_minutes" -> stringResource(Res.string.reminder_30_minutes)
        "reminder_1_hour" -> stringResource(Res.string.reminder_1_hour)
        "reminder_2_hours" -> stringResource(Res.string.reminder_2_hours)
        "reminder_1_day" -> stringResource(Res.string.reminder_1_day)
        "reminder_1_week" -> stringResource(Res.string.reminder_1_week)
        "reminder_custom" -> stringResource(Res.string.reminder_custom)
        else -> option.titleKey
    }
}

// Non-Composable version for use in non-Composable contexts
fun formatReminderText(minutes: Int?): String {
    return when (minutes) {
        null -> "No reminder"
        0 -> "At time of event"
        5 -> "5 minutes before"
        10 -> "10 minutes before"
        15 -> "15 minutes before"
        30 -> "30 minutes before"
        60 -> "1 hour before"
        120 -> "2 hours before"
        1440 -> "1 day before"
        10080 -> "1 week before"
        else -> {
            when {
                minutes < 60 -> "$minutes minutes before"
                minutes < 1440 -> {
                    val hours = minutes / 60
                    val remainingMinutes = minutes % 60
                    if (remainingMinutes == 0) {
                        "$hours hour${if (hours > 1) "s" else ""} before"
                    } else {
                        "$hours hour${if (hours > 1) "s" else ""} $remainingMinutes minute${if (remainingMinutes > 1) "s" else ""} before"
                    }
                }
                else -> {
                    val days = minutes / 1440
                    val remainingHours = (minutes % 1440) / 60
                    if (remainingHours == 0) {
                        "$days day${if (days > 1) "s" else ""} before"
                    } else {
                        "$days day${if (days > 1) "s" else ""} $remainingHours hour${if (remainingHours > 1) "s" else ""} before"
                    }
                }
            }
        }
    }
}

// Composable version for use in Composable contexts
@Composable
fun formatReminderTextComposable(minutes: Int?): String {
    return when (minutes) {
        null -> stringResource(Res.string.reminder_none)
        0 -> stringResource(Res.string.reminder_at_time)
        5 -> stringResource(Res.string.reminder_5_minutes)
        10 -> stringResource(Res.string.reminder_10_minutes)
        15 -> stringResource(Res.string.reminder_15_minutes)
        30 -> stringResource(Res.string.reminder_30_minutes)
        60 -> stringResource(Res.string.reminder_1_hour)
        120 -> stringResource(Res.string.reminder_2_hours)
        1440 -> stringResource(Res.string.reminder_1_day)
        10080 -> stringResource(Res.string.reminder_1_week)
        else -> {
            when {
                minutes < 60 -> "$minutes ${stringResource(Res.string.minutes).lowercase()} before"
                minutes < 1440 -> {
                    val hours = minutes / 60
                    val remainingMinutes = minutes % 60
                    if (remainingMinutes == 0) {
                        "$hours ${stringResource(Res.string.hours).lowercase()} before"
                    } else {
                        "$hours ${stringResource(Res.string.hours).lowercase()} $remainingMinutes ${stringResource(Res.string.minutes).lowercase()} before"
                    }
                }
                else -> {
                    val days = minutes / 1440
                    val remainingHours = (minutes % 1440) / 60
                    if (remainingHours == 0) {
                        "$days days before"
                    } else {
                        "$days days $remainingHours ${stringResource(Res.string.hours).lowercase()} before"
                    }
                }
            }
        }
    }
}