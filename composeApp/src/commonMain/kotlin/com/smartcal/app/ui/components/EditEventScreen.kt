package com.smartcal.app.ui.components

import smartcal.composeapp.generated.resources.Res
import smartcal.composeapp.generated.resources.attendees_label
import smartcal.composeapp.generated.resources.attendees_placeholder
import smartcal.composeapp.generated.resources.cancel_button
import smartcal.composeapp.generated.resources.cd_back
import smartcal.composeapp.generated.resources.contacts_label
import smartcal.composeapp.generated.resources.delete_button
import smartcal.composeapp.generated.resources.delete_event_message
import smartcal.composeapp.generated.resources.delete_event_title
import smartcal.composeapp.generated.resources.edit_event_title
import smartcal.composeapp.generated.resources.event_date_label_end
import smartcal.composeapp.generated.resources.event_date_label_start
import smartcal.composeapp.generated.resources.event_notes_label
import smartcal.composeapp.generated.resources.event_time_label_end
import smartcal.composeapp.generated.resources.event_time_label_start
import smartcal.composeapp.generated.resources.event_title_label
import smartcal.composeapp.generated.resources.ok_button
import smartcal.composeapp.generated.resources.save_button
import smartcal.composeapp.generated.resources.set_reminder
import smartcal.composeapp.generated.resources.reminder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.smartcal.app.models.CreateEventAttendee
import com.smartcal.app.models.Event
import com.smartcal.app.models.UpdateEventRequest
import com.smartcal.app.utils.formatDateForDisplay
import com.mohamedrejeb.calf.ui.datepicker.AdaptiveDatePicker
import com.mohamedrejeb.calf.ui.datepicker.rememberAdaptiveDatePickerState
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    event: Event,
    calendarId: String,
    onClose: () -> Unit,
    onSave: (UpdateEventRequest) -> Unit,
    onDelete: (() -> Unit)? = null,
    onOpenContactsPicker: () -> Unit,
    onOpenDatePicker: (isStart: Boolean, initialDate: String?) -> Unit,
    prefillAttendees: List<String>? = null,
    prefillDateStart: String? = null,
    prefillDateEnd: String? = null,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    isDeleting: Boolean = false
) {
    // Focus management for iOS keyboard behavior
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }
    val notesFocusRequester = remember { FocusRequester() }
    val attendeesFocusRequester = remember { FocusRequester() }
    
    var title by remember { mutableStateOf(event.summary ?: "") }
    var notes by remember { mutableStateOf(event.description ?: "") }
    // Split date and time into separate fields
    // These store the actual ISO format dates for backend
    var dateStart by remember {
        mutableStateOf(
            event.start?.date ?: event.start?.dateTime?.substringBefore("T") ?: ""
        )
    }
    var timeStartText by remember {
        mutableStateOf(
            event.start?.dateTime?.substringAfter("T")?.substring(0, 5) ?: ""
        )
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var dateEnd by remember {
        mutableStateOf(
            event.end?.date ?: event.end?.dateTime?.substringBefore("T") ?: ""
        )
    }
    var timeEndText by remember {
        mutableStateOf(
            event.end?.dateTime?.substringAfter("T")?.substring(0, 5) ?: ""
        )
    }

    // attendees as simple comma-separated emails
    val initialAttendees = remember(event.attendees) {
        event.attendees?.mapNotNull { it.email } ?: emptyList()
    }
    val attendees = remember { mutableStateListOf<String>().also { it.addAll(initialAttendees) } }
    var attendeesText by remember { mutableStateOf(attendees.joinToString(", ")) }
    
    // reminder state - initialize from existing event data
    var reminderMinutes by remember { 
        mutableStateOf<Int?>(
            event.reminders?.overrides?.firstOrNull()?.minutes ?: 15
        ) 
    }
    var reminderMethod by remember { 
        mutableStateOf(
            event.reminders?.overrides?.firstOrNull()?.method ?: "popup"
        ) 
    }
    var showReminderPicker by remember { mutableStateOf(false) }

    // Apply prefill attendees when provided
    LaunchedEffect(prefillAttendees) {
        prefillAttendees?.let { list ->
            val merged = (attendees + list).toSet().toList()
            attendees.clear(); attendees.addAll(merged)
            attendeesText = merged.joinToString(", ")
        }
    }

    // Keep attendees list and text in sync
    LaunchedEffect(attendeesText) {
        val list = attendeesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        attendees.clear(); attendees.addAll(list)
    }

    // Apply prefill dates when provided (from DatePickerScreen)
    LaunchedEffect(prefillDateStart) {
        prefillDateStart?.let { dateStart = it }
    }
    LaunchedEffect(prefillDateEnd) {
        prefillDateEnd?.let { dateEnd = it }
    }

    // Date & time picker dialog visibility flags
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.edit_event_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = com.smartcal.app.utils.componentsui.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    onDelete?.let {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isSaving && !isDeleting
                        ) {
                            Icon(
                                imageVector = com.smartcal.app.utils.componentsui.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(16.dp)
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize()
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(Res.string.event_title_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { notesFocusRequester.requestFocus() }
                    ),
                    singleLine = true
                )
                // Dates row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp, end = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = formatDateForDisplay(dateStart),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.event_date_label_start)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showStartDatePicker = true }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp, start = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = formatDateForDisplay(dateEnd),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.event_date_label_end)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showEndDatePicker = true }
                        )
                    }
                }

                // Times row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp, end = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = timeStartText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.event_time_label_start)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showStartTimePicker = true }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp, start = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = timeEndText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.event_time_label_end)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showEndTimePicker = true }
                        )
                    }
                }



                TimePickerModal(
                    isVisible = showStartTimePicker,
                    initialHour = timeStartText.split(":").getOrNull(0)?.toIntOrNull() ?: 12,
                    initialMinute = timeStartText.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
                    onTimeSelected = { time ->
                        timeStartText = time
                        
                        // Automatically set end time to 1 hour after start time
                        val timeParts = time.split(":")
                        val startHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 12
                        val startMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                        
                        // Calculate end time (1 hour later)
                        val endHour = (startHour + 1) % 24
                        val endTimeFormatted = "${endHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}"
                        timeEndText = endTimeFormatted
                        
                        showStartTimePicker = false
                    },
                    onDismiss = { showStartTimePicker = false },
                    title = stringResource(Res.string.event_time_label_start)
                )

                TimePickerModal(
                    isVisible = showEndTimePicker,
                    initialHour = timeEndText.split(":").getOrNull(0)?.toIntOrNull() ?: 12,
                    initialMinute = timeEndText.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
                    onTimeSelected = { time ->
                        timeEndText = time
                        showEndTimePicker = false
                    },
                    onDismiss = { showEndTimePicker = false },
                    title = stringResource(Res.string.event_time_label_end)
                )

                // Start date dialog
                if (showStartDatePicker) {
                    val initialMillis = remember(dateStart) {
                        try {
                            if (dateStart.isNotBlank()) LocalDate.parse(dateStart)
                                .atStartOfDayIn(TimeZone.UTC)
                                .toEpochMilliseconds() else null
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    val dpState =
                        rememberAdaptiveDatePickerState(initialSelectedDateMillis = initialMillis)
                    DatePickerDialog(
                        onDismissRequest = { showStartDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val millis = dpState.selectedDateMillis ?: initialMillis
                                    ?: kotlinx.datetime.Clock.System.now().toEpochMilliseconds() // Fallback to current date
                                val dateStr = Instant.fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.UTC).date.toString()
                                dateStart = dateStr

                                // Auto-update end date if it's before the new start date
                                try {
                                    val startDate = LocalDate.parse(dateStr)
                                    val currentEndDate =
                                        if (dateEnd.isNotBlank()) LocalDate.parse(dateEnd) else null
                                    if (currentEndDate == null || currentEndDate < startDate) {
                                        dateEnd = dateStr // Set end date to same as start date
                                    }
                                } catch (_: Throwable) {
                                    // If parsing fails, just use the new start date for end date
                                    dateEnd = dateStr
                                }
                                showStartDatePicker = false
                            }) { Text(stringResource(Res.string.ok_button)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartDatePicker = false }) {
                                Text(stringResource(Res.string.cancel_button))
                            }
                        }
                    ) {
                        AdaptiveDatePicker(
                            state = dpState,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }

                // End date dialog
                if (showEndDatePicker) {
                    val initialMillis = remember(dateEnd) {
                        try {
                            if (dateEnd.isNotBlank()) LocalDate.parse(dateEnd)
                                .atStartOfDayIn(TimeZone.UTC)
                                .toEpochMilliseconds() else null
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    val dpState =
                        rememberAdaptiveDatePickerState(initialSelectedDateMillis = initialMillis)
                    DatePickerDialog(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val millis = dpState.selectedDateMillis ?: initialMillis
                                    ?: kotlinx.datetime.Clock.System.now().toEpochMilliseconds() // Fallback to current date
                                val dateStr = Instant.fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.UTC).date.toString()
                                dateEnd = dateStr
                                showEndDatePicker = false
                            }) { Text(stringResource(Res.string.ok_button)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndDatePicker = false }) {
                                Text(stringResource(Res.string.cancel_button))
                            }
                        }
                    ) {
                        AdaptiveDatePicker(
                            state = dpState,
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                        )

                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(Res.string.event_notes_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(notesFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { attendeesFocusRequester.requestFocus() }
                    )
                )

                // Attendees with Contacts action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.attendees_label),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onOpenContactsPicker) {
                        Text(text = stringResource(Res.string.contacts_label))
                    }
                }
                OutlinedTextField(
                    value = attendeesText,
                    onValueChange = { attendeesText = it },
                    placeholder = { Text(stringResource(Res.string.attendees_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(attendeesFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus()
                            keyboardController?.hide() 
                        }
                    )
                )

                // Reminder section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.reminder),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { showReminderPicker = true }) {
                        Text(text = com.smartcal.app.ui.components.formatReminderTextComposable(reminderMinutes))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        fun buildDateTime(date: String, time: String): String? {
                            if (date.isBlank()) return null
                            // Google Calendar API requires ISO 8601 format with timezone offset
                            return if (time.isBlank()) {
                                date
                            } else {
                                // Use common timezone offsets based on system timezone
                                val systemZone = TimeZone.currentSystemDefault()
                                val zoneId = systemZone.id

                                val offsetString = when {
                                    zoneId.contains("Argentina") || zoneId.contains("Buenos_Aires") -> "-03:00"
                                    zoneId.contains("America/New_York") -> "-05:00" // EST
                                    zoneId.contains("America/Chicago") -> "-06:00" // CST
                                    zoneId.contains("America/Denver") -> "-07:00" // MST
                                    zoneId.contains("America/Los_Angeles") -> "-08:00" // PST
                                    zoneId.contains("Europe/London") -> "+00:00" // GMT
                                    zoneId.contains("Europe/Paris") || zoneId.contains("Europe/Madrid") -> "+01:00" // CET
                                    zoneId.contains("Asia/Tokyo") -> "+09:00" // JST
                                    zoneId.contains("Australia/Sydney") -> "+10:00" // AEST
                                    else -> "-05:00" // Default to EST
                                }

                                "${date}T${time}:00${offsetString}"
                            }
                        }

                        val req = UpdateEventRequest(
                            calendarId = calendarId,
                            summary = title.ifBlank { null },
                            description = notes.ifBlank { null },
                            start = buildDateTime(dateStart, timeStartText),
                            end = buildDateTime(dateEnd, timeEndText),
                            attendees = if (attendees.isEmpty()) null else attendees.map { email ->
                                CreateEventAttendee(email)
                            },
                            reminders = reminderMinutes?.let { minutes ->
                                com.smartcal.app.models.EventReminders(
                                    useDefault = false,
                                    overrides = listOf(
                                        com.smartcal.app.models.EventReminder(
                                            method = reminderMethod,
                                            minutes = minutes
                                        )
                                    )
                                )
                            }
                        )
                        onSave(req)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.save_button))
                }

                Button(
                    onClick = onClose, 
                    modifier = Modifier.fillMaxWidth(), 
                    enabled = !isSaving && !isDeleting
                ) {
                    Text(text = stringResource(Res.string.cancel_button))
                }
            }
            if (isSaving || isDeleting) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(Res.string.delete_event_title)) },
                text = { Text(stringResource(Res.string.delete_event_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete?.invoke()
                        }
                    ) {
                        Text(
                            text = stringResource(Res.string.delete_button),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(stringResource(Res.string.cancel_button))
                    }
                }
            )
        }
        
        // Reminder Picker Modal
        if (showReminderPicker) {
            AlertDialog(
                onDismissRequest = { showReminderPicker = false },
                title = { Text(stringResource(Res.string.set_reminder)) },
                text = {
                    com.smartcal.app.ui.components.ReminderPicker(
                        selectedMinutes = reminderMinutes,
                        selectedMethod = reminderMethod,
                        onReminderSelected = { selection ->
                            reminderMinutes = selection.minutes
                            reminderMethod = selection.method
                            showReminderPicker = false
                        }
                    )
                },
                confirmButton = {}
            )
        }
    }
}