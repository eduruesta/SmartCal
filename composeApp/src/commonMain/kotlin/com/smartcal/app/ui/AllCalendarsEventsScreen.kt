package com.smartcal.app.ui

import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.calendars
import smartcalai.composeapp.generated.resources.cd_menu
import smartcalai.composeapp.generated.resources.cd_back
import smartcalai.composeapp.generated.resources.events
import smartcalai.composeapp.generated.resources.failed_load_events
import smartcalai.composeapp.generated.resources.loading_events
import smartcalai.composeapp.generated.resources.no_events_selected_date
import smartcalai.composeapp.generated.resources.past_events
import smartcalai.composeapp.generated.resources.profile_picture
import smartcalai.composeapp.generated.resources.retry_button
import smartcalai.composeapp.generated.resources.upcoming_events
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil3.compose.AsyncImage
import com.smartcal.app.models.Calendar
import com.smartcal.app.models.CalendarsResponse
import com.smartcal.app.models.Event
import com.smartcal.app.models.EventsResponse
import com.smartcal.app.storage.SessionStorage
import com.smartcal.app.ui.components.AllCalendarsCalendarSection
import com.smartcal.app.ui.components.CalendarDrawer
import com.smartcal.app.ui.components.DayViewCalendarSection
import com.smartcal.app.ui.components.EventDetailsContent
import com.smartcal.app.ui.components.EventItemWithCalendar
import com.smartcal.app.ui.components.PullHintBar
import com.smartcal.app.ui.components.SectionHeaderCompact
import com.smartcal.app.ui.components.WeekViewCalendarSection
import com.smartcal.app.utils.componentsui.Event_note
import com.smartcal.app.utils.componentsui.HamburgerMenu
import com.smartcal.app.utils.componentsui.ArrowBack
import com.smartcal.app.utils.daysInMonth
import com.smartcal.app.utils.eventEndInstant
import com.smartcal.app.utils.eventStartInstant
import com.smartcal.app.utils.getErrorMessage
import com.smartcal.app.utils.nextMonth
import com.smartcal.app.utils.prevMonth
import com.smartcal.app.viewmodel.CalendarViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.smartcal.app.utils.rememberCurrentTime
import org.jetbrains.compose.resources.stringResource

// 🚀 OPTIMIZATION: Helper class for efficient event filtering
object EventFilteringHelper {
    fun filterEvents(
        allEvents: List<Pair<Event, Calendar>>,
        year: Int,
        month: Int,
        selectedDate: LocalDate,
        filterType: EventFilterType,
        systemZone: TimeZone
    ): List<Pair<Event, Calendar>> {
        val (startDate, endDate) = when (filterType) {
            EventFilterType.DAY -> selectedDate to selectedDate
            EventFilterType.WEEK -> {
                val weekStart =
                    selectedDate.minus(DatePeriod(days = selectedDate.dayOfWeek.ordinal))
                val weekEnd = weekStart.plus(DatePeriod(days = 6))
                weekStart to weekEnd
            }

            EventFilterType.MONTH -> {
                val monthStart = LocalDate(year, month, 1)
                val monthEnd = LocalDate(year, month, daysInMonth(year, month))
                monthStart to monthEnd
            }
        }

        return allEvents
            .mapNotNull { (event, calendar) ->
                val startInst = eventStartInstant(event, systemZone) ?: return@mapNotNull null
                val endInst = eventEndInstant(event, systemZone) ?: startInst
                Triple(event, calendar, startInst) to endInst
            }
            .filter { (triple, endInst) ->
                val eventStart = triple.third.toLocalDateTime(systemZone).date
                val eventEnd = endInst.toLocalDateTime(systemZone).date
                // Event overlaps with range if it starts before range ends AND ends after range starts
                eventStart <= endDate && eventEnd >= startDate
            }
            .sortedBy { it.first.third }
            .map { (triple, _) -> triple.first to triple.second }
    }
}

// 🚀 OPTIMIZATION: Helper for efficient event separation
object EventSeparationHelper {
    fun separateEvents(
        filteredEvents: List<Pair<Event, Calendar>>,
        currentTime: kotlinx.datetime.Instant,
        systemZone: TimeZone
    ): Pair<List<Pair<Event, Calendar>>, List<Pair<Event, Calendar>>> {
        val (past, upcoming) = filteredEvents.partition { (event, _) ->
            val eventEndTime =
                eventEndInstant(event, systemZone) ?: eventStartInstant(event, systemZone)
            eventEndTime?.let { it < currentTime } ?: false
        }

        // Past events: most recent first (descending), limit to 50 for performance
        val sortedPastEvents = past.sortedByDescending { (event, _) ->
            eventStartInstant(event, systemZone)
        }.take(50)

        // Upcoming events: closest first (ascending) - already in correct order
        val sortedUpcomingEvents = upcoming

        return sortedPastEvents to sortedUpcomingEvents
    }
}

// Filter types for events display
enum class EventFilterType {
    DAY, WEEK, MONTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCalendarsEventsScreen(
    viewModel: CalendarViewModel,
    profilePictureUrl: String? = null,
    subscriptionPlan: String? = null,
    modifier: Modifier = Modifier,
    openContactsScreen: (((List<String>) -> Unit) -> Unit)? = null,
    openEditEvent: ((Event, String) -> Unit)? = null,
    forceShowCreateEvent: Boolean = false,
    onCreateEventClosed: (() -> Unit)? = null,
    externalAttendeesBuffer: List<String>? = null,
    loadContacts: (suspend () -> Result<com.smartcal.app.models.ContactsResponse>)? = null
) {

    // Get data from ViewModel
    val calendarsResponse = viewModel.calendarsResponse
    val allEventsResponses = viewModel.allEventsResponses
    val isLoading = viewModel.isLoadingCalendar
    val isRefreshing = viewModel.isRefreshing
    val error = viewModel.calendarError
    val enabledCalendars = viewModel.enabledCalendars
    val scope = rememberCoroutineScope()

    // Create Event flow state
    var showCreateEvent by remember { mutableStateOf(false) }
    
    // Handle force show create event
    LaunchedEffect(forceShowCreateEvent) {
        if (forceShowCreateEvent) {
            showCreateEvent = true
        }
    }
    var attendeesBuffer by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Update attendees buffer when external buffer changes
    LaunchedEffect(externalAttendeesBuffer) {
        if (externalAttendeesBuffer != null) {
            attendeesBuffer = externalAttendeesBuffer
        }
    }
    var isCreating by remember { mutableStateOf(false) }
    
    // Internal contacts picker state
    var showInternalContactsPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading && calendarsResponse == null -> {
                    // Initial loading state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(Res.string.loading_events))
                    }
                }

                error != null && calendarsResponse == null -> {
                    // Error state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = getErrorMessage(error),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            scope.launch {
                                viewModel.refreshCalendarData()
                            }
                        }) {
                            Text(stringResource(Res.string.retry_button))
                        }
                    }
                }

                calendarsResponse?.success == true -> {
                    // Main content with blur overlay when refreshing
                    Box {
                        CalendarContent(
                            calendarsResponse = calendarsResponse,
                            allEventsResponses = allEventsResponses,
                            enabledCalendars = enabledCalendars,
                            onCalendarToggle = { calendarId, enabled ->
                                viewModel.toggleCalendar(calendarId, enabled)
                            },
                            profilePictureUrl = profilePictureUrl,
                            subscriptionPlan = viewModel.userProfile?.subscriptionPlan,
                            currentUserEmail = viewModel.userProfile?.email,
                            onRefresh = { viewModel.refreshCalendarData() },
                            openEditEvent = openEditEvent,
                            viewModel = viewModel
                        )

                        // Blur overlay when refreshing
                        if (isRefreshing) {
                            BlurLoadingOverlay()
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(Res.string.failed_load_events))
                    }
                }
            }
        }

        // Create Event overlay
        if (showCreateEvent && calendarsResponse?.calendars != null) {
            // Filter calendars according to subscription plan
            val allCals = calendarsResponse.calendars
            val filteredCalendars = remember(allCals, subscriptionPlan) {
                if (subscriptionPlan?.contains("Pro") == true) {
                    allCals
                } else {
                    allCals.filter { it.primary == true }
                }
            }

            // Render CreateEventScreen fullscreen over content
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                com.smartcal.app.ui.components.CreateEventScreen(
                    calendars = filteredCalendars,
                    subscriptionPlan = subscriptionPlan,
                    prefillAttendees = attendeesBuffer,
                    isCreating = isCreating,
                    onOpenContactsPicker = {
                        if (loadContacts != null) {
                            showInternalContactsPicker = true
                        } else {
                            openContactsScreen?.invoke { emails -> attendeesBuffer = emails }
                        }
                    },
                    onClose = {
                        showCreateEvent = false
                        attendeesBuffer = emptyList()
                        onCreateEventClosed?.invoke()
                    },
                    onCreate = { req ->
                        scope.launch {
                            isCreating = true
                            val res = viewModel.createEvent(req)
                            if (res.isSuccess) {
                                showCreateEvent = false
                                attendeesBuffer = emptyList()
                                viewModel.refreshCalendarData()
                                onCreateEventClosed?.invoke()
                            }
                            isCreating = false
                        }
                    }
                )
            }
        }

        // Internal ContactsPicker overlay
        if (showInternalContactsPicker && loadContacts != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                com.smartcal.app.ui.components.ContactsPickerScreen(
                    loadContacts = loadContacts,
                    onCancel = {
                        showInternalContactsPicker = false
                    },
                    onDone = { emails ->
                        attendeesBuffer = emails
                        showInternalContactsPicker = false
                    }
                )
            }
        }

        // Floating Action Button for creating a new event
        if (!showCreateEvent && !showInternalContactsPicker) {
            FloatingActionButton(
                onClick = { showCreateEvent = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = com.smartcal.app.utils.componentsui.Plus,
                    contentDescription = null,
                    Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarContent(
    calendarsResponse: CalendarsResponse,
    allEventsResponses: Map<String, EventsResponse>,
    enabledCalendars: Set<String>,
    onCalendarToggle: (String, Boolean) -> Unit,
    profilePictureUrl: String?,
    subscriptionPlan: String?,
    currentUserEmail: String?,
    onRefresh: suspend () -> Unit,
    openEditEvent: ((Event, String) -> Unit)?,
    viewModel: CalendarViewModel
) {
    val scope = rememberCoroutineScope()

    val calendars = calendarsResponse.calendars.orEmpty()
    val allEvents = mutableListOf<Pair<Event, Calendar>>()

    // Collect events from enabled calendars only
    calendars.filter { enabledCalendars.contains(it.id) }.forEach { calendar ->
        allEventsResponses[calendar.id]?.events?.forEach { event ->
            allEvents.add(event to calendar)
        }
    }

    val systemZone = TimeZone.currentSystemDefault()
    val currentTime = rememberCurrentTime()
    val now = currentTime.date

    // State: current month and selected date
    var selectedDate by remember { mutableStateOf(now) }
    var year by remember { mutableStateOf(selectedDate.year) }
    var month by remember { mutableStateOf(selectedDate.monthNumber) }

    // Collapsible sections state
    var pastEventsExpanded by remember { mutableStateOf(true) }
    var upcomingEventsExpanded by remember { mutableStateOf(true) }

    // Filter type state
    var filterType by remember { mutableStateOf(EventFilterType.DAY) }
    
    // Navigation state - to track previous filter when navigating to DAY view
    var previousFilterType by remember { mutableStateOf<EventFilterType?>(null) }

    // Top app bar scroll behavior (reserved)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Event detail bottom sheet state
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showEventDetails by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    // 🚀 OPTIMIZATION: Cached filtered events based on filter type
    val filteredEvents = remember(allEvents, year, month, selectedDate, filterType) {
        EventFilteringHelper.filterEvents(
            allEvents,
            year,
            month,
            selectedDate,
            filterType,
            systemZone
        )
    }

    // 🚀 OPTIMIZATION: Separate events into past and upcoming with efficient processing
    // Use currentTime to trigger re-separation when app comes back from background
    val (pastEvents, upcomingEvents) = remember(filteredEvents, currentTime) {
        EventSeparationHelper.separateEvents(filteredEvents, Clock.System.now(), systemZone)
    }

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Animar el contenido principal cuando se abre/cierra el drawer
    val drawerOffset = if (drawerState.targetValue == DrawerValue.Open) 240.dp else 0.dp
    val contentOffset = animateDpAsState(
        targetValue = drawerOffset,
        label = "drawerAnimation"
    )

    // Drawer content (left side)
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.3f),
        drawerContent = {
            CalendarDrawer(
                filterType = filterType,
                calendars = calendars,
                enabledCalendars = enabledCalendars,
                subscriptionPlan = subscriptionPlan,
                onFilterTypeChange = { type ->
                    filterType = type
                    // Clear previous filter when manually changing from drawer
                    previousFilterType = null
                    scope.launch { drawerState.close() }
                },
                onCalendarToggle = onCalendarToggle
            )
        }
    )
    {

        // Main content (with offset; shadow handled by ModalNavigationDrawer scrim)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = contentOffset.value)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.calendars)) },
                        navigationIcon = {
                            if (filterType == EventFilterType.DAY && previousFilterType != null) {
                                // Show back button when in DAY view and came from another view
                                IconButton(onClick = {
                                    // Return to previous filter type
                                    filterType = previousFilterType!!
                                    previousFilterType = null
                                }) {
                                    Icon(
                                        imageVector = ArrowBack,
                                        contentDescription = stringResource(Res.string.cd_back),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                // Show hamburger menu for drawer
                                IconButton(onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                }) {
                                    Icon(
                                        imageVector = HamburgerMenu,
                                        contentDescription = stringResource(Res.string.cd_menu),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        },
                        actions = {
                            // Profile picture (Right)
                            AsyncImage(
                                model = profilePictureUrl,
                                contentDescription = stringResource(Res.string.profile_picture),
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)

                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = Color.White
                        ),
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { paddingValues ->
                val scope = rememberCoroutineScope()
                val density = LocalDensity.current
                val haptic = LocalHapticFeedback.current

                // PTR hint state
                var hasUserPerformedPTR by remember { mutableStateOf(false) }

                // Initialize PTR state from storage
                LaunchedEffect(Unit) {
                    try {
                        val stored = SessionStorage.hasUserPerformedPTR()
                        println("🔍 PTR Debug - Stored value: $stored")
                        hasUserPerformedPTR = stored
                        println("🔍 PTR Debug - hasUserPerformedPTR: $hasUserPerformedPTR")
                        println("🔍 PTR Debug - !hasUserPerformedPTR (visible): ${!hasUserPerformedPTR}")
                    } catch (e: Exception) {
                        println("SessionStorage error: ${e.message}")
                        hasUserPerformedPTR = false
                    }
                }

                // Estado PTR
                val pullToRefreshState = rememberPullToRefreshState()

                // Haptic al cruzar umbral (una vez por gesto)
                var firedHaptic by remember { mutableStateOf(false) }

                // Track if refresh was actually started by user
                var hasStartedRefreshing by remember { mutableStateOf(false) }
                LaunchedEffect(pullToRefreshState.distanceFraction, viewModel.isRefreshing) {
                    val crossed = pullToRefreshState.distanceFraction >= 1f
                    if (crossed && !firedHaptic && !viewModel.isRefreshing) {
                        runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                        firedHaptic = true
                    }
                    if (!crossed && !viewModel.isRefreshing) firedHaptic = false
                }

                // Track when refresh starts
                LaunchedEffect(viewModel.isRefreshing) {
                    if (viewModel.isRefreshing) {
                        hasStartedRefreshing = true
                    }
                }

                // Offset visual del contenido (suave, con easing)
                val maxDragOffset = 40.dp
                val easedFraction by animateFloatAsState(
                    targetValue = pullToRefreshState.distanceFraction.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing),
                    label = "ptrFraction"
                )
                val contentOffsetPx =
                    with(density) { lerp(0.dp, maxDragOffset, easedFraction).toPx() }

                // Cuando termina el refresh, escondé rápido el indicador (si tu versión lo soporta)
                LaunchedEffect(viewModel.isRefreshing, hasStartedRefreshing) {
                    if (!viewModel.isRefreshing && hasStartedRefreshing) {
                        // Algunas versiones exponen animateToHidden(); si no, el propio Box lo resuelve.
                        runCatching { pullToRefreshState.animateToHidden() }

                        // Mark that user has performed PTR and hide hint
                        if (!hasUserPerformedPTR) {
                            hasUserPerformedPTR = true
                            try {
                                SessionStorage.setUserPerformedPTR(true)
                                println("🔍 PTR completed - hiding hint forever")
                            } catch (e: Exception) {
                                println("Error saving PTR flag: ${e.message}")
                            }
                        }
                        hasStartedRefreshing = false
                    }
                }

                PullToRefreshBox(
                    state = pullToRefreshState,
                    isRefreshing = viewModel.isRefreshing,
                    onRefresh = {
                        if (!viewModel.isRefreshing) {
                            scope.launch { onRefresh() }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PullHintBar(
                            visible = !hasUserPerformedPTR,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = paddingValues.calculateTopPadding())
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationY = contentOffsetPx }
                                .padding(top = if (hasUserPerformedPTR) paddingValues.calculateTopPadding() else 0.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // --------- tu contenido tal cual ----------
                            item {
                                when (filterType) {
                                    EventFilterType.DAY -> {
                                        DayViewCalendarSection(
                                            selectedDate = selectedDate,
                                            eventsWithCalendars = filteredEvents,
                                            onPrevDay = {
                                                selectedDate =
                                                    selectedDate.minus(DatePeriod(days = 1))
                                                year = selectedDate.year; month =
                                                selectedDate.monthNumber
                                            },
                                            onNextDay = {
                                                selectedDate =
                                                    selectedDate.plus(DatePeriod(days = 1))
                                                year = selectedDate.year; month =
                                                selectedDate.monthNumber
                                            },
                                            onEventClick = { event ->
                                                selectedEvent = event; showEventDetails = true
                                            }
                                        )
                                    }

                                    EventFilterType.WEEK -> {
                                        WeekViewCalendarSection(
                                            selectedDate = selectedDate,
                                            eventsWithCalendars = filteredEvents,
                                            onPrevWeek = {
                                                selectedDate =
                                                    selectedDate.minus(DatePeriod(days = 7))
                                                year = selectedDate.year; month =
                                                selectedDate.monthNumber
                                            },
                                            onNextWeek = {
                                                selectedDate =
                                                    selectedDate.plus(DatePeriod(days = 7))
                                                year = selectedDate.year; month =
                                                selectedDate.monthNumber
                                            },
                                            onEventClick = { event ->
                                                selectedEvent = event; showEventDetails = true
                                            },
                                            onSelectDate = {
                                                selectedDate = it; year = it.year; month =
                                                it.monthNumber
                                                // Remember current filter before changing to DAY
                                                if (filterType != EventFilterType.DAY) {
                                                    previousFilterType = filterType
                                                }
                                                filterType = EventFilterType.DAY
                                            }
                                        )
                                    }

                                    EventFilterType.MONTH -> {
                                        AllCalendarsCalendarSection(
                                            year = year,
                                            month = month,
                                            selectedDate = selectedDate,
                                            eventsWithCalendars = allEvents.filter { (_, c) ->
                                                enabledCalendars.contains(
                                                    c.id
                                                )
                                            },
                                            onPrevMonth = {
                                                val (ny, nm) = prevMonth(year, month)
                                                year = ny; month = nm
                                                selectedDate = LocalDate(
                                                    year,
                                                    month,
                                                    minOf(
                                                        selectedDate.dayOfMonth,
                                                        daysInMonth(year, month)
                                                    )
                                                )
                                            },
                                            onNextMonth = {
                                                val (ny, nm) = nextMonth(year, month)
                                                year = ny; month = nm
                                                selectedDate = LocalDate(
                                                    year,
                                                    month,
                                                    minOf(
                                                        selectedDate.dayOfMonth,
                                                        daysInMonth(year, month)
                                                    )
                                                )
                                            },
                                            onSelectDate = {
                                                selectedDate = it; year = it.year; month =
                                                it.monthNumber
                                                // Remember current filter before changing to DAY
                                                if (filterType != EventFilterType.DAY) {
                                                    previousFilterType = filterType
                                                }
                                                filterType = EventFilterType.DAY
                                            },
                                            onEventClick = { event ->
                                                selectedEvent = event; showEventDetails = true
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = stringResource(Res.string.events),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 16.dp)
                                )
                            }

                            if (pastEvents.isEmpty() && upcomingEvents.isEmpty()) {
                                item {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Event_note,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            stringResource(Res.string.no_events_selected_date),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                if (pastEvents.isNotEmpty()) {
                                    item {
                                        SectionHeaderCompact(
                                            title = stringResource(Res.string.past_events),
                                            titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            expanded = pastEventsExpanded,
                                            onToggle = { pastEventsExpanded = !pastEventsExpanded }
                                        )
                                    }
                                    if (pastEventsExpanded) {
                                        itemsIndexed(
                                            pastEvents,
                                            key = { index, (e, c) -> "past_${c.id}_${e.id}_$index" }) { _, (event, calendar) ->
                                            EventItemWithCalendar(event, calendar, isPast = true) {
                                                selectedEvent = event; showEventDetails = true
                                            }
                                        }
                                    }
                                }
                                if (upcomingEvents.isNotEmpty()) {
                                    item {
                                        SectionHeaderCompact(
                                            title = stringResource(Res.string.upcoming_events),
                                            titleColor = MaterialTheme.colorScheme.primary,
                                            expanded = upcomingEventsExpanded,
                                            onToggle = {
                                                upcomingEventsExpanded = !upcomingEventsExpanded
                                            }
                                        )
                                    }
                                    if (upcomingEventsExpanded) {
                                        itemsIndexed(
                                            upcomingEvents,
                                            key = { index, (e, c) -> "upcoming_${c.id}_${e.id}_$index" }) { _, (event, calendar) ->
                                            EventItemWithCalendar(event, calendar, isPast = false) {
                                                selectedEvent = event; showEventDetails = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // Event detail bottom sheet
                    if (showEventDetails) {
                        ModalBottomSheet(
                            onDismissRequest = { showEventDetails = false },
                            sheetState = bottomSheetState
                        ) {
                            selectedEvent?.let { event ->
                                // Determine if current user is host/organizer
                                val isHost = (event.organizer?.self == true) ||
                                        (currentUserEmail != null && event.organizer?.email == currentUserEmail)

                                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                    EventDetailsContent(
                                        event = event,
                                        isHost = isHost,
                                        onEdit = {
                                            showEventDetails = false
                                            // Find the calendar ID for this event
                                            val calendarId =
                                                allEvents.find { it.first.id == event.id }?.second?.id
                                                    ?: "primary"
                                            openEditEvent?.invoke(event, calendarId)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Contacts picker navigates to dedicated screen via callback
                    // Using openContactsScreen provided by parent to navigate
                    // No in-place overlay here anymore
                }
            }
        }
    }
}


@Composable
private fun BlurLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}