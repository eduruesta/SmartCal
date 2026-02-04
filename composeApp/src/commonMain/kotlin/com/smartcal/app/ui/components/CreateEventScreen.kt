package com.smartcal.app.ui.components

import smartcal.composeapp.generated.resources.Res
import smartcal.composeapp.generated.resources.attendees_label
import smartcal.composeapp.generated.resources.attendees_placeholder
import smartcal.composeapp.generated.resources.calendar_label
import smartcal.composeapp.generated.resources.cancel_button
import smartcal.composeapp.generated.resources.cd_back
import smartcal.composeapp.generated.resources.contacts_label
import smartcal.composeapp.generated.resources.create_button
import smartcal.composeapp.generated.resources.create_event_title
import smartcal.composeapp.generated.resources.event_date_label_end
import smartcal.composeapp.generated.resources.event_date_label_start
import smartcal.composeapp.generated.resources.event_notes_label
import smartcal.composeapp.generated.resources.event_time_label_end
import smartcal.composeapp.generated.resources.event_time_label_start
import smartcal.composeapp.generated.resources.event_title_label
import smartcal.composeapp.generated.resources.ok_button
import smartcal.composeapp.generated.resources.reminder
import smartcal.composeapp.generated.resources.select_calendar_title
import smartcal.composeapp.generated.resources.set_reminder
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.smartcal.app.models.Calendar
import com.smartcal.app.models.CreateEventAttendee
import com.smartcal.app.models.CreateEventRequest
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
fun CreateEventScreen(
    calendars: List<Calendar>,
    subscriptionPlan: String?,
    onClose: () -> Unit,
    onCreate: (CreateEventRequest) -> Unit,
    onOpenContactsPicker: () -> Unit,
    prefillAttendees: List<String>? = null,
    modifier: Modifier = Modifier,
    isCreating: Boolean = false,
) {
    val isPro = subscriptionPlan?.contains("Pro", ignoreCase = true) == true
    val availableCalendars = remember(calendars, isPro) {
        if (isPro) calendars else calendars.filter { it.primary == true }
    }

    // Selected calendar
    var selectedCalendar by remember {
        mutableStateOf(availableCalendars.firstOrNull())
    }
    var showCalendarDialog by remember { mutableStateOf(false) }

    // Focus management for iOS keyboard behavior
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }
    val notesFocusRequester = remember { FocusRequester() }
    val attendeesFocusRequester = remember { FocusRequester() }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var dateStart by remember { mutableStateOf("") }
    var timeStartText by remember { mutableStateOf("") }

    var dateEnd by remember { mutableStateOf("") }
    var timeEndText by remember { mutableStateOf("") }

    // attendees as simple comma-separated emails
    val attendees = remember { mutableStateListOf<String>() }
    var attendeesText by remember { mutableStateOf("") }

    // reminder state
    var reminderMinutes by remember { mutableStateOf<Int?>(15) } // Default 15 minutes
    var reminderMethod by remember { mutableStateOf("popup") } // Default popup
    var showReminderPicker by remember { mutableStateOf(false) }

    // Keep attendees list and text in sync
    LaunchedEffect(attendeesText) {
        val list = attendeesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        attendees.clear(); attendees.addAll(list)
    }

    // Apply prefill attendees when provided
    LaunchedEffect(prefillAttendees) {
        prefillAttendees?.let { list ->
            val merged = (attendees + list).toSet().toList()
            attendees.clear(); attendees.addAll(merged)
            attendeesText = merged.joinToString(", ")
        }
    }

    // Date & time picker dialog visibility flags
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.create_event_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = com.smartcal.app.utils.componentsui.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                // Calendar selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCalendar?.summary ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.calendar_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                if (availableCalendars.isNotEmpty()) showCalendarDialog = true
                            }
                    )
                }

                if (showCalendarDialog) {
                    AlertDialog(
                        onDismissRequest = { showCalendarDialog = false },
                        confirmButton = {
                            TextButton(onClick = { showCalendarDialog = false }) {
                                Text(stringResource(Res.string.ok_button))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCalendarDialog = false }) {
                                Text(stringResource(Res.string.cancel_button))
                            }
                        },
                        title = { Text(stringResource(Res.string.select_calendar_title)) },
                        text = {
                            Column {
                                availableCalendars.forEach { cal ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedCalendar = cal }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedCalendar?.id == cal.id,
                                            onClick = { selectedCalendar = cal }
                                        )
                                        Text(
                                            text = cal.summary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

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
                                // If end date is blank or before start, set it equal to start by default
                                try {
                                    val s = LocalDate.parse(dateStr)
                                    val e =
                                        if (dateEnd.isNotBlank()) LocalDate.parse(dateEnd) else null
                                    if (e == null || e < s) {
                                        dateEnd = dateStr
                                    }
                                } catch (_: Throwable) {
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
                        AdaptiveDatePicker(state = dpState, modifier = Modifier.fillMaxWidth())
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
                        AdaptiveDatePicker(state = dpState, modifier = Modifier.fillMaxWidth())
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
                        Text(
                            text = com.smartcal.app.ui.components.formatReminderTextComposable(
                                reminderMinutes
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        fun buildDateTime(date: String, time: String): String {
                            // For create, require date; time optional
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
                                    zoneId.contains("Europe/Madrid") || zoneId.contains("Europe/Paris") -> "+01:00" // CET
                                    zoneId.contains("Asia/Tokyo") -> "+09:00" // JST
                                    else -> "-03:00" // Default to Argentina time for now
                                }

                                "${date}T${time}:00${offsetString}"
                            }
                        }

                        val req = CreateEventRequest(
                            calendarId = selectedCalendar?.id ?: "primary",
                            summary = title.ifBlank { "" },
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
                        onCreate(req)
                    },
                    enabled = !isCreating && selectedCalendar != null && dateStart.isNotBlank() && dateEnd.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.create_button))
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                ) {
                    Text(text = stringResource(Res.string.cancel_button))
                }
            }

            if (isCreating) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
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
