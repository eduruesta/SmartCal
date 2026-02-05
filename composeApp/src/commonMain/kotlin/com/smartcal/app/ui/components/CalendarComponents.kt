package com.smartcal.app.ui.components


import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.day_friday
import smartcalai.composeapp.generated.resources.day_monday
import smartcalai.composeapp.generated.resources.day_saturday
import smartcalai.composeapp.generated.resources.day_sunday
import smartcalai.composeapp.generated.resources.day_thursday
import smartcalai.composeapp.generated.resources.day_tuesday
import smartcalai.composeapp.generated.resources.day_wednesday
import smartcalai.composeapp.generated.resources.days_of_week_fri
import smartcalai.composeapp.generated.resources.days_of_week_mon
import smartcalai.composeapp.generated.resources.days_of_week_sat
import smartcalai.composeapp.generated.resources.days_of_week_sun
import smartcalai.composeapp.generated.resources.days_of_week_thu
import smartcalai.composeapp.generated.resources.days_of_week_tue
import smartcalai.composeapp.generated.resources.days_of_week_wed
import smartcalai.composeapp.generated.resources.month_full_april
import smartcalai.composeapp.generated.resources.month_full_august
import smartcalai.composeapp.generated.resources.month_full_december
import smartcalai.composeapp.generated.resources.month_full_february
import smartcalai.composeapp.generated.resources.month_full_january
import smartcalai.composeapp.generated.resources.month_full_july
import smartcalai.composeapp.generated.resources.month_full_june
import smartcalai.composeapp.generated.resources.month_full_march
import smartcalai.composeapp.generated.resources.month_full_may
import smartcalai.composeapp.generated.resources.month_full_november
import smartcalai.composeapp.generated.resources.month_full_october
import smartcalai.composeapp.generated.resources.month_full_september
import smartcalai.composeapp.generated.resources.no_title
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.smartcal.app.models.Calendar
import com.smartcal.app.models.Event
import com.smartcal.app.utils.daysInMonth
import com.smartcal.app.utils.eventCoversDate
import com.smartcal.app.utils.eventEndLocalDate
import com.smartcal.app.utils.eventStartLocalDate
import com.smartcal.app.utils.rememberCurrentMinutes
import com.smartcal.app.utils.rememberCurrentTime
import com.smartcal.app.utils.rememberIsToday
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

// --------- EVENT LAYOUT (col + span) ----------

data class EventLayout(
    val event: Event,
    val calendar: Calendar,
    val startMinute: Int,
    val durationMinutes: Int,
    val column: Int,
    val span: Int,            // cuántas columnas ocupa (expansión estilo Google Calendar)
    val totalColumns: Int     // máximo de columnas concurrentes en su grupo
)

private data class EventInterval(
    val event: Event,
    val calendar: Calendar,
    val startMinute: Int,
    val endMinute: Int
)

private fun eventsOverlap(a: EventInterval, b: EventInterval): Boolean {
    return a.startMinute < b.endMinute && b.startMinute < a.endMinute
}

private fun calculateEventLayouts(
    dayEvents: List<Pair<Event, Calendar>>,
    selectedDate: LocalDate,
    zone: TimeZone
): List<EventLayout> {

    val intervals = dayEvents.mapNotNull { (e, c) ->
        val (start, dur) = eventMinutesInDayForDate(e, selectedDate, zone)
        if (dur <= 0) null else EventInterval(e, c, start, start + dur)
    }.sortedBy { it.startMinute }

    if (intervals.isEmpty()) return emptyList()

    // Partición por conectividad de solapados
    val groups = mutableListOf<MutableList<EventInterval>>()
    for (ev in intervals) {
        val hits =
            groups.withIndex().filter { (_, g) -> g.any { eventsOverlap(it, ev) } }.map { it.index }
        when {
            hits.isEmpty() -> groups.add(mutableListOf(ev))
            hits.size == 1 -> groups[hits.first()].add(ev)
            else -> {
                val keep = hits.first()
                groups[keep].add(ev)
                hits.drop(1).sortedDescending().forEach { idx ->
                    groups[keep].addAll(groups[idx]); groups.removeAt(idx)
                }
            }
        }
    }

    val out = mutableListOf<EventLayout>()

    for (g in groups) {
        val columns: MutableList<MutableList<EventInterval>> = mutableListOf()
        val ordered =
            g.sortedWith(compareBy<EventInterval> { it.startMinute }.thenBy { it.endMinute - it.startMinute })
        val placed = mutableListOf<Pair<EventInterval, Int>>() // (intervalo, columna)

        // Asignación de columna greedy
        for (ev in ordered) {
            var col = 0
            while (col < columns.size && columns[col].any { eventsOverlap(it, ev) }) col++
            if (col == columns.size) columns.add(mutableListOf())
            columns[col].add(ev)
            placed.add(ev to col)
        }
        val maxCols = columns.size

        // span dinámico hacia la derecha (ocupa columnas libres)
        placed.forEach { (ev, col) ->
            var span = 1
            var test = col + 1
            while (test < maxCols) {
                val conflict = columns[test].any { eventsOverlap(it, ev) }
                if (conflict) break
                span++; test++
            }
            out.add(
                EventLayout(
                    event = ev.event,
                    calendar = ev.calendar,
                    startMinute = ev.startMinute,
                    durationMinutes = ev.endMinute - ev.startMinute,
                    column = col,
                    span = span,
                    totalColumns = maxCols
                )
            )
        }
    }

    return out
}

// --------- TIME HELPERS ----------

private fun Instant.toMillisCompat(): Long =
    this.epochSeconds * 1_000L + this.nanosecondsOfSecond / 1_000_000L

/**
 * Devuelve (offsetMin, durationMin) del segmento del evento que cae dentro de visibleDate.
 */
private fun eventMinutesInDayForDate(
    event: Event,
    visibleDate: LocalDate,
    zone: TimeZone
): Pair<Int, Int> {
    val startInstant: Instant? = when {
        event.start?.dateTime != null -> runCatching { Instant.parse(event.start.dateTime) }.getOrNull()
        event.start?.date != null -> runCatching {
            LocalDate.parse(event.start.date).atStartOfDayIn(zone)
        }.getOrNull()

        else -> null
    }
    val endInstant: Instant? = when {
        event.end?.dateTime != null -> runCatching { Instant.parse(event.end.dateTime) }.getOrNull()
        event.end?.date != null -> runCatching {
            LocalDate.parse(event.end.date).atStartOfDayIn(zone)
        }.getOrNull()

        else -> null
    }

    if (startInstant == null || endInstant == null) return 0 to 0
    if (endInstant <= startInstant) return 0 to 0

    val dayStart = visibleDate.atStartOfDayIn(zone)
    val dayEnd = visibleDate.plus(DatePeriod(days = 1)).atStartOfDayIn(zone)

    val segStart = if (startInstant > dayStart) startInstant else dayStart
    val segEnd = if (endInstant < dayEnd) endInstant else dayEnd
    if (segEnd <= segStart) return 0 to 0

    val offsetMin = ((segStart.toMillisCompat() - dayStart.toMillisCompat()) / 60_000L).toInt()
    val durationMin = ((segEnd.toMillisCompat() - segStart.toMillisCompat()) / 60_000L).toInt()
    return offsetMin.coerceIn(0, 24 * 60) to durationMin.coerceIn(0, 24 * 60)
}

// --------- RENDERER COMÚN (Día y Semana) ----------

@Composable
private fun RenderEventLayouts(
    eventLayouts: List<EventLayout>,
    hourHeight: androidx.compose.ui.unit.Dp,
    onEventClick: (Event) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = maxWidth
        val minEventHeight = 24.dp

        eventLayouts
            .sortedByDescending { it.durationMinutes }
            .forEach { layout ->

                // color por calendario
                val color = remember(layout.calendar.backgroundColor) {
                    layout.calendar.backgroundColor?.let { hex ->
                        runCatching {
                            val v = hex.removePrefix("#").toLong(16)
                            Color(0xFF000000 or v)
                        }.getOrElse { Color(0xFF2196F3) }
                    } ?: Color(0xFF2196F3)
                }

                val yOffset = hourHeight * (layout.startMinute / 60f)
                val height = max(
                    minEventHeight,
                    hourHeight * ((layout.durationMinutes / 60f).coerceAtLeast(0.25f))
                )

                // columnas sin gap dentro del mismo día + expansión por span
                val colWidth = containerWidth / layout.totalColumns
                val xOffset = colWidth * layout.column
                val width = colWidth * layout.span

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .width(width)
                        .height(height)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.95f))
                        .clickable { onEventClick(layout.event) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = layout.event.summary ?: stringResource(Res.string.no_title),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = Color.White,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
    }
}

// --------- DÍA ----------

@Composable
fun DayViewCalendarSection(
    selectedDate: LocalDate,
    eventsWithCalendars: List<Pair<Event, Calendar>>,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onEventClick: (Event) -> Unit = {}
) {
    val zone = TimeZone.currentSystemDefault()

    // Helpers to get events/layouts for any date
    @Composable
    fun DayPage(date: LocalDate) {
        val dayEvents = remember(eventsWithCalendars, date) {
            eventsWithCalendars.filter { (event, _) ->
                eventCoversDate(event, date, zone)
            }
        }
        val HOUR_HEIGHT = 70.dp
        val TOTAL_HEIGHT = 24 * HOUR_HEIGHT
        val isToday = rememberIsToday(date)
        val currentMinutes: Int? = rememberCurrentMinutes() // Always show time line
        val gridBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        val gridLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        val nowLine = MaterialTheme.colorScheme.primary
        val dayLayouts = remember(dayEvents, date) {
            calculateEventLayouts(dayEvents, date, zone)
        }
        val density = LocalDensity.current

        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${date.dayOfMonth}/${date.monthNumber}/${date.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getDayOfWeekName(date),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Internal vertical scroll to navigate to the current hour slot
            val dayScrollState = rememberScrollState()
            var viewportHeightPx by remember { mutableStateOf(0) }
            LaunchedEffect(isToday, viewportHeightPx, date) {
                if (isToday && currentMinutes != null && viewportHeightPx > 0) {
                    // wait a frame to ensure layout is measured before scrolling
                    kotlinx.coroutines.delay(16)
                    val hourHeightPx = with(density) { HOUR_HEIGHT.toPx() }
                    val totalHeightPx = 24f * hourHeightPx
                    val target = ((currentMinutes / 60f) * hourHeightPx - viewportHeightPx / 2f)
                        .coerceIn(0f, (totalHeightPx - viewportHeightPx).coerceAtLeast(0f))
                    dayScrollState.scrollTo(target.toInt())
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .onSizeChanged { viewportHeightPx = it.height }
                    .verticalScroll(dayScrollState)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left gutter: hours + now time label
                    Box(modifier = Modifier.width(50.dp).height(TOTAL_HEIGHT)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            repeat(24) { h ->
                                Box(
                                    modifier = Modifier.height(HOUR_HEIGHT).fillMaxWidth(),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Text(
                                        text = "$h",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp, end = 6.dp)
                                    )
                                }
                            }
                        }
                        if (currentMinutes != null) {
                            val yOffset = HOUR_HEIGHT * (currentMinutes / 60f)
                            Row(
                                modifier = Modifier
                                    .offset(y = yOffset - 10.dp)
                                    .fillMaxWidth()
                                    .padding(end = 2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val currentTime = rememberCurrentTime()
                                val hh = currentTime.hour
                                val mm = currentTime.minute
                                val timeText = "$hh:${mm.toString().padStart(2, '0')}"
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Grid + events
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(TOTAL_HEIGHT)
                    ) {
                        // Grid
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(gridBg, RoundedCornerShape(6.dp))
                        ) {
                            val hourHeightPx = size.height / 24f
                            for (h in 0..24) {
                                val y = h * hourHeightPx
                                drawLine(
                                    color = gridLine,
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                            }
                            currentMinutes?.let { min ->
                                val y = (min / 60f) * hourHeightPx
                                drawLine(
                                    color = nowLine,
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 3f
                                )
                            }
                        }

                        // Events
                        RenderEventLayouts(
                            eventLayouts = dayLayouts,
                            hourHeight = HOUR_HEIGHT,
                            onEventClick = onEventClick
                        )
                    }
                }
            }
        }
    }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var totalDragX by remember { mutableStateOf(0f) }
    var containerWidthPx by remember { mutableStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(selectedDate) { offsetX.snapTo(0f) }

    val prevDate = remember(selectedDate) { selectedDate.minus(DatePeriod(days = 1)) }
    val nextDate = remember(selectedDate) { selectedDate.plus(DatePeriod(days = 1)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .pointerInput(selectedDate) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                        val w = if (containerWidthPx > 0f) containerWidthPx else with(density) { 1.dp.toPx() }
                        val next = (offsetX.value + dragAmount).coerceIn(-w, w)
                        scope.launch { offsetX.snapTo(next) }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, animationSpec = tween(180)) }
                    },
                    onDragEnd = {
                        val baseThreshold = with(density) { 56.dp.toPx() }
                        val threshold = if (containerWidthPx > 0f) containerWidthPx * 0.25f else baseThreshold
                        val absDrag = kotlin.math.abs(totalDragX)
                        if (absDrag >= threshold) {
                            val direction = if (totalDragX > 0f) 1f else -1f
                            scope.launch {
                                val w = if (containerWidthPx > 0f) containerWidthPx else baseThreshold * 3f
                                // animate current off-screen once
                                offsetX.animateTo(direction * w, animationSpec = tween(220))
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (direction > 0f) onPrevDay() else onNextDay()
                                // reset offset to idle to avoid double animation; new content appears centered
                                offsetX.snapTo(0f)
                            }
                        } else {
                            scope.launch { offsetX.animateTo(0f, animationSpec = tween(180)) }
                        }
                    }
                )
            }
    ) {
        val w = containerWidthPx
        if (w > 0f) {
            Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value - w }) {
                DayPage(prevDate)
            }
        }
        Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value }) {
            DayPage(selectedDate)
        }
        if (w > 0f) {
            Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value + w }) {
                DayPage(nextDate)
            }
        }
    }
}

// --------- SEMANA ----------

@Composable
private fun DayColumn(
    date: LocalDate,
    events: List<Pair<Event, Calendar>>,
    onEventClick: (Event) -> Unit,
    currentMinutesForWeek: Int?,
    modifier: Modifier = Modifier
) {
    val zone = TimeZone.currentSystemDefault()
    val HOUR_HEIGHT = 70.dp
    val TOTAL_HEIGHT = 24 * HOUR_HEIGHT

    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val nowLine = MaterialTheme.colorScheme.primary

    // Layouts solo para este día
    val layouts = remember(events, date) {
        calculateEventLayouts(events, date, zone)
    }

    Box(modifier = modifier.height(TOTAL_HEIGHT)) {
        // Grid del día
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor, RoundedCornerShape(4.dp))
        ) {
            val hourHeightPx = size.height / 24f
            for (h in 0..24) {
                val y = h * hourHeightPx
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
            
            // Draw current time line
            currentMinutesForWeek?.let { minutes ->
                val y = (minutes / 60f) * hourHeightPx
                drawLine(
                    color = nowLine,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 3f
                )
            }
        }

        // Eventos del día
        RenderEventLayouts(
            eventLayouts = layouts,
            hourHeight = HOUR_HEIGHT,
            onEventClick = onEventClick
        )
    }
}

@Composable
fun WeekViewCalendarSection(
    selectedDate: LocalDate,
    eventsWithCalendars: List<Pair<Event, Calendar>>,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onEventClick: (Event) -> Unit = {},
    onSelectDate: (LocalDate) -> Unit = {}
) {
    val zone = TimeZone.currentSystemDefault()

    @Composable
    fun WeekPage(baseDate: LocalDate) {
        val density = LocalDensity.current
        // Inicio de semana (lunes)
        val weekStart = baseDate.minus(DatePeriod(days = baseDate.dayOfWeek.ordinal))
        val weekDays = remember(weekStart) { (0..6).map { weekStart.plus(DatePeriod(days = it)) } }

        val weekEvents = remember(eventsWithCalendars, weekDays) {
            eventsWithCalendars.filter { (event, _) ->
                weekDays.any { day -> eventCoversDate(event, day, zone) }
            }
        }

        val HOUR_HEIGHT = 70.dp
        val TOTAL_HEIGHT = 24 * HOUR_HEIGHT

        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${weekStart.dayOfMonth}/${weekStart.monthNumber} - ${weekDays.last().dayOfMonth}/${weekDays.last().monthNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            // Encabezado de días
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(50.dp)) // columna de horas
                weekDays.forEach { day ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectDate(day) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${day.dayOfMonth}/${day.monthNumber}",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (day == baseDate) FontWeight.Bold else FontWeight.Normal,
                            color = if (day == baseDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = getDayOfWeekName(day),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Cuerpo semana con scroll vertical interno
            val weekScrollState = rememberScrollState()
            var viewportHeightPx by remember { mutableStateOf(0) }
            // ¿Esta semana contiene hoy?
            val currentTime = rememberCurrentTime()
            val today = currentTime.date
            val isCurrentWeek = remember(weekStart) { today in weekDays }
            val currentMinutes: Int? = rememberCurrentMinutes() // Always show time line
            LaunchedEffect(isCurrentWeek, viewportHeightPx) {
                if (isCurrentWeek && currentMinutes != null && viewportHeightPx > 0) {
                    val hourHeightPx = with(density) { HOUR_HEIGHT.toPx() }
                    val totalHeightPx = 24f * hourHeightPx
                    val target = ((currentMinutes / 60f) * hourHeightPx - viewportHeightPx / 2f)
                        .coerceIn(0f, (totalHeightPx - viewportHeightPx).coerceAtLeast(0f))
                    weekScrollState.scrollTo(target.toInt())
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .onSizeChanged { viewportHeightPx = it.height }
                    .verticalScroll(weekScrollState)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Columna de horas con etiqueta de hora actual
                    Box(modifier = Modifier.width(50.dp).height(TOTAL_HEIGHT)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            repeat(24) { h ->
                                Box(
                                    modifier = Modifier
                                        .height(HOUR_HEIGHT)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Text(
                                        text = "$h",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp, end = 6.dp)
                                    )
                                }
                            }
                        }
                        if (currentMinutes != null) {
                            val yOffset = HOUR_HEIGHT * (currentMinutes / 60f)
                            Row(
                                modifier = Modifier
                                    .offset(y = yOffset - 10.dp)
                                    .fillMaxWidth()
                                    .padding(end = 2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val currentTime = rememberCurrentTime()
                                val hh = currentTime.hour
                                val mm = currentTime.minute
                                val timeText = "$hh:${mm.toString().padStart(2, '0')}"
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 7 columnas (una por día) con gutter entre días + línea de tiempo continua
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(TOTAL_HEIGHT)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekDays.forEach { day ->
                                val dayEvents = weekEvents.filter { (event, _) ->
                                    eventCoversDate(event, day, zone)
                                }
                                DayColumn(
                                    date = day,
                                    events = dayEvents,
                                    onEventClick = onEventClick,
                                    currentMinutesForWeek = currentMinutes, // Always show time line
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (currentMinutes != null) {
                            val lineColor = MaterialTheme.colorScheme.primary
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.matchParentSize()
                            ) {
                                val y = size.height * (currentMinutes / (24f * 60f))
                                drawLine(
                                    color = lineColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 3f
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var totalDragX by remember { mutableStateOf(0f) }
    var containerWidthPx by remember { mutableStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(selectedDate) { offsetX.snapTo(0f) }

    val prevDate = remember(selectedDate) { selectedDate.minus(DatePeriod(days = 7)) }
    val nextDate = remember(selectedDate) { selectedDate.plus(DatePeriod(days = 7)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .pointerInput(selectedDate) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                        val w = if (containerWidthPx > 0f) containerWidthPx else with(density) { 1.dp.toPx() }
                        val next = (offsetX.value + dragAmount).coerceIn(-w, w)
                        scope.launch { offsetX.snapTo(next) }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, animationSpec = tween(180)) }
                    },
                    onDragEnd = {
                        val baseThreshold = with(density) { 56.dp.toPx() }
                        val threshold = if (containerWidthPx > 0f) containerWidthPx * 0.25f else baseThreshold
                        val absDrag = kotlin.math.abs(totalDragX)
                        if (absDrag >= threshold) {
                            val direction = if (totalDragX > 0f) 1f else -1f
                            scope.launch {
                                val w = if (containerWidthPx > 0f) containerWidthPx else baseThreshold * 3f
                                offsetX.animateTo(direction * w, animationSpec = tween(220))
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (direction > 0f) onPrevWeek() else onNextWeek()
                                offsetX.snapTo(0f)
                            }
                        } else {
                            scope.launch { offsetX.animateTo(0f, animationSpec = tween(180)) }
                        }
                    }
                )
            }
    ) {
        val w = containerWidthPx
        if (w > 0f) {
            Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value - w }) {
                WeekPage(prevDate)
            }
        }
        Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value }) {
            WeekPage(selectedDate)
        }
        if (w > 0f) {
            Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value + w }) {
                WeekPage(nextDate)
            }
        }
    }
}

// --------- MES (tal cual lo tenías optimizado) ----------

@Composable
fun AllCalendarsCalendarSection(
    year: Int,
    month: Int,
    selectedDate: LocalDate,
    eventsWithCalendars: List<Pair<Event, Calendar>>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit = {}
) {
    // Shared palette per calendar id
    val calendarColors = remember(eventsWithCalendars) {
        eventsWithCalendars.map { (_, calendar) -> calendar.id }.distinct()
            .associateWith { calendarId ->
                eventsWithCalendars.find { (_, cal) -> cal.id == calendarId }?.second?.backgroundColor?.let { colorString ->
                    try {
                        val colorValue = colorString.removePrefix("#").toLong(16)
                        Color(0xFF000000 or colorValue)
                    } catch (_: Exception) {
                        Color(0xFF2196F3)
                    }
                } ?: Color(0xFF2196F3)
            }
    }

    @Composable
    fun MonthPage(y: Int, m: Int) {
        val monthName = when (m) {
            1 -> stringResource(Res.string.month_full_january)
            2 -> stringResource(Res.string.month_full_february)
            3 -> stringResource(Res.string.month_full_march)
            4 -> stringResource(Res.string.month_full_april)
            5 -> stringResource(Res.string.month_full_may)
            6 -> stringResource(Res.string.month_full_june)
            7 -> stringResource(Res.string.month_full_july)
            8 -> stringResource(Res.string.month_full_august)
            9 -> stringResource(Res.string.month_full_september)
            10 -> stringResource(Res.string.month_full_october)
            11 -> stringResource(Res.string.month_full_november)
            else -> stringResource(Res.string.month_full_december)
        }
        val monthData = remember(y, m, eventsWithCalendars) {
            OptimizedMonthData.create(y, m, eventsWithCalendars)
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$monthName $y",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(
                    stringResource(Res.string.days_of_week_mon),
                    stringResource(Res.string.days_of_week_tue),
                    stringResource(Res.string.days_of_week_wed),
                    stringResource(Res.string.days_of_week_thu),
                    stringResource(Res.string.days_of_week_fri),
                    stringResource(Res.string.days_of_week_sat),
                    stringResource(Res.string.days_of_week_sun)
                ).forEach { dayName ->
                    Text(
                        dayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val selectedForPage = remember(selectedDate, y, m) {
                if (selectedDate.year == y && selectedDate.monthNumber == m) selectedDate else LocalDate(y, m, 1)
            }
            OptimizedMonthGrid(
                monthData = monthData,
                selectedDate = selectedForPage,
                calendarColors = calendarColors,
                onSelectDate = onSelectDate,
                onEventClick = onEventClick
            )
        }
    }

    fun prevOf(y: Int, m: Int): Pair<Int, Int> = if (m == 1) (y - 1) to 12 else y to (m - 1)
    fun nextOf(y: Int, m: Int): Pair<Int, Int> = if (m == 12) (y + 1) to 1 else y to (m + 1)

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var totalDragX by remember { mutableStateOf(0f) }
    var containerWidthPx by remember { mutableStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(year, month) { offsetX.snapTo(0f) }

    val (prevY, prevM) = remember(year, month) { prevOf(year, month) }
    val (nextY, nextM) = remember(year, month) { nextOf(year, month) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .pointerInput(year, month) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                        val w = if (containerWidthPx > 0f) containerWidthPx else with(density) { 1.dp.toPx() }
                        val next = (offsetX.value + dragAmount).coerceIn(-w, w)
                        scope.launch { offsetX.snapTo(next) }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, animationSpec = tween(180)) }
                    },
                    onDragEnd = {
                        val baseThreshold = with(density) { 56.dp.toPx() }
                        val threshold = if (containerWidthPx > 0f) containerWidthPx * 0.25f else baseThreshold
                        val absDrag = kotlin.math.abs(totalDragX)
                        if (absDrag >= threshold) {
                            val direction = if (totalDragX > 0f) 1f else -1f
                            scope.launch {
                                val w = if (containerWidthPx > 0f) containerWidthPx else baseThreshold * 3f
                                offsetX.animateTo(direction * w, animationSpec = tween(220))
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (direction > 0f) onPrevMonth() else onNextMonth()
                                offsetX.snapTo(0f)
                            }
                        } else {
                            scope.launch { offsetX.animateTo(0f, animationSpec = tween(180)) }
                        }
                    }
                )
            }
    ) {
        val w = containerWidthPx
        if (w > 0f) {
            Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value - w }) {
                MonthPage(prevY, prevM)
            }
        }
        Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value }) {
            MonthPage(year, month)
        }
        if (w > 0f) {
            Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value + w }) {
                MonthPage(nextY, nextM)
            }
        }
    }
}

private data class OptimizedMonthData(
    val firstDay: LocalDate,
    val leadingBlanks: Int,
    val totalDays: Int,
    val rows: Int,
    val eventsPerDay: Map<LocalDate, List<Pair<Event, Calendar>>>
) {
    companion object {
        fun create(
            year: Int,
            month: Int,
            eventsWithCalendars: List<Pair<Event, Calendar>>
        ): OptimizedMonthData {
            val firstDay = LocalDate(year, month, 1)
            val leadingBlanks = firstDay.dayOfWeek.ordinal
            val totalDays = daysInMonth(year, month)
            val totalCells = leadingBlanks + totalDays
            val rows = (totalCells + 6) / 7

            val systemZone = TimeZone.currentSystemDefault()
            val monthStart = LocalDate(year, month, 1)
            val monthEnd = LocalDate(year, month, totalDays)
            val eventsPerDay = mutableMapOf<LocalDate, MutableList<Pair<Event, Calendar>>>()
            for ((event, calendar) in eventsWithCalendars) {
                val eStart = eventStartLocalDate(event, systemZone) ?: continue
                val rawEnd = eventEndLocalDate(event, systemZone) ?: eStart.plus(DatePeriod(days = 1))
                // Google Calendar semantics:
                // - All-day events use end.date which is EXCLUSIVE (already next day)
                // - Timed events use end.dateTime which is INSTANT-precise; their LocalDate should be treated as INCLUSIVE
                //   for the day they end, so we add +1 day to convert to an exclusive end boundary.
                val isAllDay = event.start?.date != null && event.end?.date != null
                val eEndExclusive = if (isAllDay) rawEnd else rawEnd.plus(DatePeriod(days = 1))
                var d = maxOf(eStart, monthStart)
                val limit = minOf(eEndExclusive, monthEnd.plus(DatePeriod(days = 1)))
                while (d < limit) {
                    if (d.year == year && d.monthNumber == month) {
                        eventsPerDay.getOrPut(d) { mutableListOf() }.add(event to calendar)
                    }
                    d = d.plus(DatePeriod(days = 1))
                }
            }

            return OptimizedMonthData(firstDay, leadingBlanks, totalDays, rows, eventsPerDay)
        }
    }
}

@Composable
private fun OptimizedMonthGrid(
    monthData: OptimizedMonthData,
    selectedDate: LocalDate,
    calendarColors: Map<String, Color>,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit
) {
    Column {
        var dayNum = 1
        repeat(monthData.rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { colIndex ->
                    val cellIndex = rowIndex * 7 + colIndex
                    if (cellIndex < monthData.leadingBlanks || dayNum > monthData.totalDays) {
                        Box(modifier = Modifier.weight(1f).height(48.dp).padding(2.dp))
                    } else {
                        val date = LocalDate(
                            monthData.firstDay.year,
                            monthData.firstDay.monthNumber,
                            dayNum
                        )
                        val dayEvents = monthData.eventsPerDay[date] ?: emptyList()
                        DayCell(
                            date = date,
                            dayNum = dayNum,
                            isSelected = date == selectedDate,
                            dayEvents = dayEvents,
                            calendarColors = calendarColors,
                            onSelectDate = onSelectDate,
                            onEventClick = onEventClick,
                            modifier = Modifier.weight(1f).height(48.dp).padding(2.dp)
                        )
                        dayNum++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    dayNum: Int,
    isSelected: Boolean,
    dayEvents: List<Pair<Event, Calendar>>,
    calendarColors: Map<String, Color>,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        onClick = {
            onSelectDate(date)
            if (dayEvents.isNotEmpty()) onEventClick(dayEvents.first().first)
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                dayNum.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            if (dayEvents.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    dayEvents.groupBy { (_, calendar) -> calendar.id }
                        .entries.take(3).forEach { (calendarId, events) ->
                            val color = calendarColors[calendarId]
                                ?: events.firstOrNull()?.second?.backgroundColor?.let { colorString ->
                                    try {
                                        val colorValue = colorString.removePrefix("#").toLong(16)
                                        Color(0xFF000000 or colorValue)
                                    } catch (_: Exception) { null }
                                }
                                ?: Color(0xFF10B981) // Verde por defecto
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                }
            }
        }
    }
}

// --------- UTILS ----------

@Composable
fun getDayOfWeekName(date: LocalDate): String {
    return when (date.dayOfWeek.ordinal) {
        0 -> stringResource(Res.string.day_monday)
        1 -> stringResource(Res.string.day_tuesday)
        2 -> stringResource(Res.string.day_wednesday)
        3 -> stringResource(Res.string.day_thursday)
        4 -> stringResource(Res.string.day_friday)
        5 -> stringResource(Res.string.day_saturday)
        6 -> stringResource(Res.string.day_sunday)
        else -> ""
    }
}

