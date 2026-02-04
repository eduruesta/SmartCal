package com.smartcal.app.ui.components

import smartcal.composeapp.generated.resources.Res
import smartcal.composeapp.generated.resources.calendars
import smartcal.composeapp.generated.resources.default_calendar_reminders
import smartcal.composeapp.generated.resources.event_attendees
import smartcal.composeapp.generated.resources.event_description
import smartcal.composeapp.generated.resources.event_end
import smartcal.composeapp.generated.resources.event_link
import smartcal.composeapp.generated.resources.event_location
import smartcal.composeapp.generated.resources.event_organizer
import smartcal.composeapp.generated.resources.event_start
import smartcal.composeapp.generated.resources.has_video_call
import smartcal.composeapp.generated.resources.join_meet
import smartcal.composeapp.generated.resources.join_video_call
import smartcal.composeapp.generated.resources.no_time_specified
import smartcal.composeapp.generated.resources.no_title
import smartcal.composeapp.generated.resources.reminder
import smartcal.composeapp.generated.resources.reminders
import smartcal.composeapp.generated.resources.unknown_attendee
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartcal.app.models.Calendar
import com.smartcal.app.models.Event
import com.smartcal.app.utils.componentsui.Alarm
import com.smartcal.app.utils.componentsui.Clock
import com.smartcal.app.utils.componentsui.Groups
import com.smartcal.app.utils.componentsui.Link
import com.smartcal.app.utils.componentsui.Person
import com.smartcal.app.utils.componentsui.notes
import com.smartcal.app.utils.formatEventDate
import com.smartcal.app.utils.formatEventDateTime
import com.smartcal.app.utils.getMeetUrl
import org.jetbrains.compose.resources.stringResource

@Composable
fun CalendarFilterSection(
    calendars: List<Calendar>,
    enabledCalendars: Set<String>,
    subscriptionPlan: String? = null,
    onCalendarToggle: (String, Boolean) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.calendars),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Filter calendars based on subscription plan
        println("🔍 CalendarFilterSection - subscriptionPlan: '$subscriptionPlan'")
        println("🔍 CalendarFilterSection - total calendars: ${calendars.size}")

        val filteredCalendars =
            if (subscriptionPlan?.contains("Free") == true || subscriptionPlan?.contains("Starter") == true) {

                val primaryCalendars = calendars.filter { it.primary == true }
                println("🔍 CalendarFilterSection - Free/Starter: filtered to ${primaryCalendars.size} primary calendars")
                primaryCalendars

            } else {
                // For Pro plan, show all calendars
                println("🔍 CalendarFilterSection - Pro: showing all ${calendars.size} calendars")
                calendars
            }

        println("🔍 CalendarFilterSection - final chips to show: ${filteredCalendars.size}")

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(filteredCalendars.sortedWith(compareBy<Calendar> { it.primary != true }.thenBy { it.summary })) { calendar ->
                CalendarFilterChip(
                    calendar = calendar,
                    isEnabled = enabledCalendars.contains(calendar.id),
                    onToggle = { enabled -> onCalendarToggle(calendar.id, enabled) }
                )
            }
        }
    }
}

@Composable
fun CalendarFilterChip(
    calendar: Calendar,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // Parse calendar color from API or use default
    val calendarColor = remember(calendar.backgroundColor) {
        calendar.backgroundColor?.let { colorString ->
            try {
                // Try to parse color from hex string like "#FF5722"
                val colorValue = colorString.removePrefix("#").toLong(16)
                Color(0xFF000000 or colorValue)
            } catch (_: Exception) {
                Color(0xFF2196F3)
            }
        } ?: Color(0xFF2196F3)
    }

    Surface(
        modifier = Modifier
            .clickable { onToggle(!isEnabled) },
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled) calendarColor.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(
            width = 2.dp,
            color = if (isEnabled) calendarColor else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = calendarColor,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.size(16.dp)
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(calendarColor, CircleShape)
            )

            Text(
                text = calendar.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EventItemWithCalendar(
    event: Event,
    calendar: Calendar,
    isPast: Boolean = false,
    onClick: () -> Unit = {}
) {
    val calendarColor = remember(calendar.backgroundColor) {
        calendar.backgroundColor?.let { colorString ->
            try {
                val colorValue = colorString.removePrefix("#").toLong(16)
                Color(0xFF000000 or colorValue)
            } catch (_: Exception) {
                Color(0xFF2196F3)
            }
        } ?: Color(0xFF2196F3)
    }

    val contentAlpha = if (isPast) 0.75f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Calendar color indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(60.dp)
                        .background(calendarColor, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title
                    Text(
                        text = event.summary ?: stringResource(Res.string.no_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Calendar name
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = calendar.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = calendarColor,
                        fontWeight = FontWeight.Medium
                    )

                    // Date/time
                    event.start?.let { start ->
                        Spacer(modifier = Modifier.height(6.dp))
                        val timeText = when {
                            start.dateTime != null -> formatEventDateTime(start.dateTime)
                            start.date != null -> formatEventDate(start.date)
                            else -> stringResource(Res.string.no_time_specified)
                        }
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Location
                    event.location?.takeIf { it.isNotBlank() }?.let { location ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Meet indicator
                    getMeetUrl(event)?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Link,
                                contentDescription = stringResource(Res.string.has_video_call),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(Res.string.join_meet),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeaderCompact(
    title: String,
    titleColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = titleColor
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (expanded) "▲" else "▼",
            style = MaterialTheme.typography.bodyMedium,
            color = titleColor,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onToggle)
                .padding(2.dp)
        )
    }
}

@Composable
fun EventDetailsContent(
    event: Event, 
    isHost: Boolean = false, 
    onEdit: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Event title + edit icon
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.summary ?: stringResource(Res.string.no_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            if (isHost) {
                // Edit icon (on the right end)
                onEdit?.let {
                    Icon(
                        imageVector = com.smartcal.app.utils.componentsui.Edit_calendar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onEdit() }
                    )
                }
            }
        }

        // Start and end times
        event.start?.let { start ->
            DetailRow(
                icon = Clock,
                label = stringResource(Res.string.event_start),
                value = when {
                    start.dateTime != null -> formatEventDateTime(start.dateTime)
                    start.date != null -> formatEventDate(start.date)
                    else -> stringResource(Res.string.no_time_specified)
                }
            )
        }

        event.end?.let { end ->
            DetailRow(
                icon = Clock,
                label = stringResource(Res.string.event_end),
                value = when {
                    end.dateTime != null -> formatEventDateTime(end.dateTime)
                    end.date != null -> formatEventDate(end.date)
                    else -> stringResource(Res.string.no_time_specified)
                }
            )
        }

        // Location
        event.location?.let { location ->
            if (location.isNotBlank()) {
                DetailRow(
                    icon = com.smartcal.app.utils.componentsui.location,
                    label = stringResource(Res.string.event_location),
                    value = location
                )
            }
        }

        // Description
        event.description?.let { description ->
            if (description.isNotBlank()) {
                DetailRow(
                    icon = notes,
                    label = stringResource(Res.string.event_description),
                    value = description
                )
            }
        }

        // Organizer
        event.organizer?.email?.let { organizerEmail ->
            val organizerName = event.organizer.displayName ?: organizerEmail
            DetailRow(
                icon = Person,
                label = stringResource(Res.string.event_organizer),
                value = organizerName
            )
        }

        // Meet/Conference Link
        getMeetUrl(event)?.let { meetUrl ->
            DetailRow(
                icon = Link,
                label = stringResource(Res.string.join_meet),
                value = stringResource(Res.string.join_video_call),
                isClickable = true,
                onClick = { uriHandler.openUri(meetUrl) }
            )
        }

        val unknown = stringResource(Res.string.unknown_attendee)

        // Attendees
        event.attendees?.let { attendees ->
            if (attendees.isNotEmpty()) {
                DetailRow(
                    icon = Groups,
                    label = stringResource(Res.string.event_attendees),
                    value = attendees.joinToString("\n") { attendee ->
                        attendee.displayName ?: attendee.email ?: unknown
                    }
                )
            }
        }

        // Conversation Link
        event.htmlLink?.let { htmlLink ->
            if (htmlLink.isNotBlank()) {
                DetailRow(
                    icon = Link,
                    label = stringResource(Res.string.event_link),
                    value = htmlLink,
                    isClickable = true,
                    onClick = { uriHandler.openUri(htmlLink) }
                )
            }
        }

        // Reminders
        event.reminders?.let { reminders ->
            if (reminders.useDefault == true) {
                DetailRow(
                    icon = Alarm,
                    label = stringResource(Res.string.reminder),
                    value = stringResource(Res.string.default_calendar_reminders)
                )
            } else {
                reminders.overrides?.let { overrides ->
                    if (overrides.isNotEmpty()) {
                        // Pre-compute localized reminder texts
                        val reminderTexts = overrides.map { reminder ->
                            "${formatReminderTextComposable(reminder.minutes)} (${reminder.method})"
                        }
                        DetailRow(
                            icon = Alarm,
                            label = stringResource(Res.string.reminders),
                            value = reminderTexts.joinToString("\n")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .let { modifier ->
                if (isClickable) {
                    modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onClick() }
                        .padding(4.dp)
                } else {
                    modifier
                }
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .padding(top = 2.dp, end = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isClickable) androidx.compose.ui.text.style.TextDecoration.Underline else null
            )
        }
    }
}