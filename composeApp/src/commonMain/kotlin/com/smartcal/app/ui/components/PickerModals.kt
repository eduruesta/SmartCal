package com.smartcal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mohamedrejeb.calf.ui.datepicker.AdaptiveDatePicker
import com.mohamedrejeb.calf.ui.datepicker.rememberAdaptiveDatePickerState
import com.mohamedrejeb.calf.ui.timepicker.AdaptiveTimePicker
import com.mohamedrejeb.calf.ui.timepicker.rememberAdaptiveTimePickerState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.ok_button
import smartcalai.composeapp.generated.resources.cancel_button
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.rememberScrollState
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    isVisible: Boolean,
    initialDateMillis: Long?,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Date"
) {
    if (!isVisible) return

    val state = rememberAdaptiveDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars)
                .windowInsetsPadding(WindowInsets.ime)
                .scrollable(
                    rememberScrollState(),
                    orientation = Orientation.Vertical
                )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            AdaptiveDatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel_button))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        val selectedMillis = state.selectedDateMillis ?: initialDateMillis
                        selectedMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC).date
                            onDateSelected(date.toString())
                        }
                    }
                ) {
                    Text(stringResource(Res.string.ok_button))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModal(
    isVisible: Boolean,
    initialHour: Int = 12,
    initialMinute: Int = 0,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Time"
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .wrapContentHeight()
                        .clickable(enabled = false) { }, // Prevent click through
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Title with padding
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(top = 20.dp, bottom = 16.dp)
                        )

                        // Time Picker - using full width
                        val state = rememberAdaptiveTimePickerState(
                            initialHour = initialHour,
                            initialMinute = initialMinute
                        )

                        AdaptiveTimePicker(
                            state = state,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons with padding
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(Res.string.cancel_button))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    val formatted = state.hour.toString().padStart(2, '0') +
                                            ":" + state.minute.toString().padStart(2, '0')
                                    onTimeSelected(formatted)
                                }
                            ) {
                                Text(stringResource(Res.string.ok_button))
                            }
                        }
                    }
                }
            }
        }
    }
}