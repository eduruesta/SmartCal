package com.smartcal.app.ui

import smartcal.composeapp.generated.resources.Res
import smartcal.composeapp.generated.resources.cd_calendars
import smartcal.composeapp.generated.resources.cd_chat
import smartcal.composeapp.generated.resources.cd_settings
import smartcal.composeapp.generated.resources.chat_welcome_message
import smartcal.composeapp.generated.resources.credits_exhausted_button
import smartcal.composeapp.generated.resources.credits_exhausted_dismiss
import smartcal.composeapp.generated.resources.credits_exhausted_message
import smartcal.composeapp.generated.resources.credits_exhausted_message_with_renewal
import smartcal.composeapp.generated.resources.credits_exhausted_message_renew_tomorrow
import smartcal.composeapp.generated.resources.credits_exhausted_message_renew_today
import smartcal.composeapp.generated.resources.credits_exhausted_title
import smartcal.composeapp.generated.resources.initializing_agent
import smartcal.composeapp.generated.resources.message_placeholder
import smartcal.composeapp.generated.resources.nav_calendar
import smartcal.composeapp.generated.resources.nav_chat
import smartcal.composeapp.generated.resources.nav_settings
import smartcal.composeapp.generated.resources.clear_button
import smartcal.composeapp.generated.resources.send_button
import smartcal.composeapp.generated.resources.title_calendar_assistant
import smartcal.composeapp.generated.resources.error_general
import smartcal.composeapp.generated.resources.error_agent_timeout
import smartcal.composeapp.generated.resources.error_insufficient_credits
import smartcal.composeapp.generated.resources.error_message_send_failed
import smartcal.composeapp.generated.resources.error_connection_lost
import smartcal.composeapp.generated.resources.error_no_internet
import smartcal.composeapp.generated.resources.error_session_expired
import smartcal.composeapp.generated.resources.error_network
import smartcal.composeapp.generated.resources.error_server
import smartcal.composeapp.generated.resources.error_unknown
import smartcal.composeapp.generated.resources.error_authentication_failed
import smartcal.composeapp.generated.resources.error_no_token_received
import smartcal.composeapp.generated.resources.error_oauth_incomplete
import smartcal.composeapp.generated.resources.error_oauth_cancelled
import smartcal.composeapp.generated.resources.error_oauth_verification_failed
import smartcal.composeapp.generated.resources.error_calendar_access
import smartcal.composeapp.generated.resources.error_events_load
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcal.app.repository.ChatMessage
import com.smartcal.app.repository.ConversationState
import com.smartcal.app.utils.VoiceTranscriberFactory
import com.smartcal.app.utils.componentsui.Chat
import com.smartcal.app.utils.componentsui.Contactless
import com.smartcal.app.utils.componentsui.Event_note
import com.smartcal.app.utils.componentsui.Settings
import com.smartcal.app.utils.getErrorMessage
import com.smartcal.app.viewmodel.CalendarViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// Bottom navigation tabs
enum class BottomNavTab {
    CALENDARS, CHAT, SETTINGS
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier,
    sessionToken: String,
    userEmail: String? = null,
    fullName: String? = null,
    profilePicture: String? = null,
    subscriptionPlan: String? = null,
    creditsRemaining: Int? = null,
    voiceTranscriberFactory: VoiceTranscriberFactory? = null,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onTokenExpired: () -> Unit = {},
    onCreditsUpdate: ((Int?, String?) -> Unit)? = null,
    onUserProfileUpdate: ((String?, String?) -> Unit)? = null,
    onUpgradePlan: () -> Unit = {},
    navigateToSettings: Boolean = false,
    settingsSuccessPlanLabel: String? = null,
    onNavigatedToSettings: () -> Unit = {},
    onSuccessMessageConsumed: () -> Unit = {}
) {
    val conversationState by viewModel.conversationState.collectAsState()
    val userProfile = viewModel.userProfile
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Bottom navigation state
    var selectedTab by remember { mutableStateOf(BottomNavTab.CALENDARS) }

    // Navigate to Settings after successful subscription and show message
    LaunchedEffect(navigateToSettings) {
        if (navigateToSettings) {
            selectedTab = BottomNavTab.SETTINGS
            onNavigatedToSettings()
        }
    }

    // Auto-refresh events whenever entering Calendar tab
    LaunchedEffect(selectedTab) {
        if (selectedTab == BottomNavTab.CALENDARS) {
            println("🔄 CalendarScreen: Auto-refreshing events on Calendar tab entry")
            viewModel.refreshCalendarData()
        }
    }

    // Simple navigation state for Contacts picker as separate screen
    var showContactsScreen by remember { mutableStateOf(false) }
    var contactsResultCallback by remember { mutableStateOf<((List<String>) -> Unit)?>(null) }
    var contactsOpenedFromCreateEvent by remember { mutableStateOf(false) }
    var showCreateEventAfterContacts by remember { mutableStateOf(false) }
    var createEventAttendeesBuffer by remember { mutableStateOf<List<String>>(emptyList()) }

    // Navigation state for Edit Event screen
    var showEditEventScreen by remember { mutableStateOf(false) }
    var selectedEventForEdit by remember {
        mutableStateOf<com.smartcal.app.models.Event?>(
            null
        )
    }
    var selectedEventCalendarId by remember { mutableStateOf<String?>(null) }
    var contactsBuffer by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUpdatingEvent by remember { mutableStateOf(false) }
    var isDeletingEvent by remember { mutableStateOf(false) }
    var showEditContactsPicker by remember { mutableStateOf(false) }
    
    // Credits renewal state
    var creditsRenewalDate by remember { mutableStateOf<String?>(null) }
    var daysUntilRenewal by remember { mutableStateOf<Int?>(null) }


    // Buffers to pass selected dates back to EditEventScreen
    var dateStartBuffer by remember { mutableStateOf<String?>(null) }
    var dateEndBuffer by remember { mutableStateOf<String?>(null) }

    // Set session token and callbacks in view model
    LaunchedEffect(sessionToken) {
        viewModel.setSessionToken(sessionToken)
        viewModel.setOnTokenExpiredCallback(onTokenExpired)
        onCreditsUpdate?.let { viewModel.setOnCreditsUpdateCallback(it) }
        onUserProfileUpdate?.let { viewModel.setOnUserProfileUpdateCallback(it) }
    }

    // Replace welcome message and error placeholders with localized versions
    val welcomeMessage = stringResource(Res.string.chat_welcome_message)
    
    // Pre-compute all error messages to avoid @Composable calls inside remember
    val errorMessages = mapOf(
        "error_agent_timeout" to stringResource(Res.string.error_agent_timeout),
        "error_insufficient_credits" to stringResource(Res.string.error_insufficient_credits),
        "error_message_send_failed" to stringResource(Res.string.error_message_send_failed),
        "error_connection_lost" to stringResource(Res.string.error_connection_lost),
        "error_no_internet" to stringResource(Res.string.error_no_internet),
        "error_session_expired" to stringResource(Res.string.error_session_expired),
        "error_network" to stringResource(Res.string.error_network),
        "error_server" to stringResource(Res.string.error_server),
        "error_authentication_failed" to stringResource(Res.string.error_authentication_failed),
        "error_no_token_received" to stringResource(Res.string.error_no_token_received),
        "error_oauth_incomplete" to stringResource(Res.string.error_oauth_incomplete),
        "error_oauth_cancelled" to stringResource(Res.string.error_oauth_cancelled),
        "error_oauth_verification_failed" to stringResource(Res.string.error_oauth_verification_failed),
        "error_calendar_access" to stringResource(Res.string.error_calendar_access),
        "error_events_load" to stringResource(Res.string.error_events_load),
        "error_unknown" to stringResource(Res.string.error_unknown),
        "error_general" to stringResource(Res.string.error_general)
    )
    
    val messagesWithLocalizedWelcome = remember(conversationState.messages, welcomeMessage, errorMessages) {
        conversationState.messages.map { message ->
            when {
                message.content == "WELCOME_MESSAGE_PLACEHOLDER" -> {
                    message.copy(content = welcomeMessage)
                }
                message.content.startsWith("ERROR_PLACEHOLDER:") -> {
                    val errorKey = message.content.removePrefix("ERROR_PLACEHOLDER:")
                    val localizedError = errorMessages[errorKey] ?: errorMessages["error_general"]!!
                    message.copy(content = localizedError)
                }
                else -> message
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive (only when on chat tab)
    LaunchedEffect(conversationState.messages.size, selectedTab) {
        if (conversationState.messages.isNotEmpty() && selectedTab == BottomNavTab.CHAT) {
            coroutineScope.launch {
                listState.animateScrollToItem(conversationState.messages.size - 1)
            }
        }
    }
    
    // Refresh user profile and credits info when entering Settings tab
    LaunchedEffect(selectedTab) {
        if (selectedTab == BottomNavTab.SETTINGS) {
            coroutineScope.launch {
                try {
                    println("🔄 Refreshing user profile for Settings screen")
                    viewModel.getUserProfile()
                    
                    // Fetch credits renewal information
                    println("🔄 Fetching credits renewal info for Settings screen")
                    val creditsInfoResult = viewModel.getUserCreditsInfo()
                    creditsInfoResult.fold(
                        onSuccess = { creditsInfo ->
                            creditsRenewalDate = creditsInfo.nextRenewalDate
                            daysUntilRenewal = creditsInfo.daysUntilRenewal
                            println("✅ Credits renewal info loaded: ${creditsInfo.daysUntilRenewal} days until renewal")
                        },
                        onFailure = { e ->
                            println("❌ Failed to fetch credits renewal info: ${e.message}")
                            creditsRenewalDate = null
                            daysUntilRenewal = null
                        }
                    )
                } catch (e: Exception) {
                    println("❌ Failed to refresh user profile: ${e.message}")
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Content based on selected tab - takes most of the space
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                BottomNavTab.CALENDARS -> {
                    // Navigate between Contacts, Edit Event, and Calendar list
                    when {
                        showContactsScreen -> {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                com.smartcal.app.ui.components.ContactsPickerScreen(
                                    loadContacts = { viewModel.fetchContacts() },
                                    onCancel = {
                                        showContactsScreen = false
                                        if (contactsOpenedFromCreateEvent) {
                                            showCreateEventAfterContacts = true
                                        }
                                        contactsOpenedFromCreateEvent = false
                                        // If we were editing, return to edit screen
                                        if (selectedEventForEdit != null) {
                                            showEditEventScreen = true
                                        }
                                    },
                                    onDone = { emails ->
                                        contactsResultCallback?.invoke(emails)
                                        contactsResultCallback = null
                                        showContactsScreen = false
                                        
                                        if (selectedEventForEdit != null) {
                                            showEditEventScreen = true
                                        }
                                        
                                        contactsOpenedFromCreateEvent = false
                                    }
                                )
                            }
                        }

                        showEditEventScreen && selectedEventForEdit != null -> {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                com.smartcal.app.ui.components.EditEventScreen(
                                    event = selectedEventForEdit!!,
                                    calendarId = selectedEventCalendarId ?: "primary",
                                    prefillAttendees = contactsBuffer,
                                    prefillDateStart = dateStartBuffer,
                                    prefillDateEnd = dateEndBuffer,
                                    isSaving = isUpdatingEvent,
                                    isDeleting = isDeletingEvent,
                                    onClose = {
                                        showEditEventScreen = false
                                        contactsBuffer = emptyList()
                                        selectedEventForEdit = null
                                        selectedEventCalendarId = null
                                        // Clear date buffers when closing editor entirely
                                        dateStartBuffer = null
                                        dateEndBuffer = null
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            selectedEventForEdit?.let { ev ->
                                                isDeletingEvent = true
                                                val result = viewModel.deleteEvent(ev.id, selectedEventCalendarId ?: "primary")
                                                isDeletingEvent = false
                                                
                                                if (result.isSuccess) {
                                                    // Navigate to calendar events screen first
                                                    showEditEventScreen = false
                                                    contactsBuffer = emptyList()
                                                    selectedEventForEdit = null
                                                    selectedEventCalendarId = null
                                                    dateStartBuffer = null
                                                    dateEndBuffer = null
                                                    
                                                    // Then refresh calendar data (this will show the refresh spinner)
                                                    viewModel.refreshCalendarData()
                                                }
                                            }
                                        }
                                    },
                                    onOpenContactsPicker = {
                                        // Use internal contacts picker to preserve EditEventScreen state
                                        showEditContactsPicker = true
                                    },
                                    onOpenDatePicker = { isStart, initial ->
                                        showEditEventScreen = false
                                    },
                                    onSave = { updateReq: com.smartcal.app.models.UpdateEventRequest ->
                                        coroutineScope.launch {
                                            selectedEventForEdit?.let { ev ->
                                                isUpdatingEvent = true
                                                val res = viewModel.updateEvent(ev.id, updateReq)
                                                if (res.isSuccess) {
                                                    showEditEventScreen = false
                                                    contactsBuffer = emptyList()
                                                    selectedEventForEdit = null
                                                    selectedEventCalendarId = null
                                                    viewModel.refreshCalendarData()
                                                    dateStartBuffer = null
                                                    dateEndBuffer = null
                                                } else {
                                                    // Keep screen open on error; could show a snackbar/toast later
                                                }
                                                isUpdatingEvent = false
                                            }
                                        }
                                    }
                                )
                            }
                            
                            // ContactsPicker overlay for EditEventScreen
                            if (showEditContactsPicker) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    com.smartcal.app.ui.components.ContactsPickerScreen(
                                        loadContacts = { viewModel.fetchContacts() },
                                        onCancel = {
                                            showEditContactsPicker = false
                                        },
                                        onDone = { emails ->
                                            contactsBuffer = emails
                                            showEditContactsPicker = false
                                        }
                                    )
                                }
                            }
                        }

                        else -> {
                            AllCalendarsEventsScreen(
                                viewModel = viewModel,
                                profilePictureUrl = userProfile?.profilePicture,
                                subscriptionPlan = subscriptionPlan,
                                modifier = Modifier.fillMaxSize(),
                                forceShowCreateEvent = showCreateEventAfterContacts,
                                externalAttendeesBuffer = createEventAttendeesBuffer,
                                loadContacts = { viewModel.fetchContacts() },
                                onCreateEventClosed = {
                                    showCreateEventAfterContacts = false
                                    createEventAttendeesBuffer = emptyList()
                                },
                                openContactsScreen = { onResult ->
                                    contactsResultCallback = { emails ->
                                        createEventAttendeesBuffer = emails
                                        onResult(emails)
                                        showCreateEventAfterContacts = true
                                    }
                                    contactsOpenedFromCreateEvent = true
                                    showContactsScreen = true
                                },
                                openEditEvent = { ev, calendarId ->
                                    selectedEventForEdit = ev
                                    selectedEventCalendarId = calendarId
                                    contactsBuffer = emptyList()
                                    showEditEventScreen = true
                                }
                            )
                        }
                    }
                }

                BottomNavTab.CHAT -> {
                    ChatScreen(
                        conversationState = conversationState,
                        viewModel = viewModel,
                        userProfile = userProfile,
                        listState = listState,
                        messagesWithLocalizedWelcome = messagesWithLocalizedWelcome,
                        creditsRemaining = creditsRemaining,
                        daysUntilRenewal = daysUntilRenewal,
                        onUpgradePlan = onUpgradePlan
                    )
                }

                BottomNavTab.SETTINGS -> {
                    SettingsScreen(
                        userEmail = userEmail,
                        fullName = fullName,
                        profilePicture = profilePicture,
                        subscriptionPlan = subscriptionPlan,
                        creditsRemaining = creditsRemaining,
                        creditsRenewalDate = creditsRenewalDate,
                        daysUntilRenewal = daysUntilRenewal,
                        isLoadingProfile = viewModel.isLoadingProfile,
                        onLogout = onLogout,
                        onDeleteAccount = onDeleteAccount,
                        onUpgradePlan = onUpgradePlan,
                        successPlanLabel = settingsSuccessPlanLabel,
                        onSuccessMessageShown = { onSuccessMessageConsumed() }
                    )
                }
            }
        }

        // Input field for chat (only show when on chat tab)
        if (selectedTab == BottomNavTab.CHAT && voiceTranscriberFactory != null) {
            ChatInputWithMic(
                factory = voiceTranscriberFactory,
                messageInput = viewModel.messageInput,
                onMessageInputChange = viewModel::updateMessageInput,
                onSendMessage = viewModel::sendMessage,
                isLoading = conversationState.isLoading
            )
        } else if (selectedTab == BottomNavTab.CHAT) {
            // Fallback for when voice transcriber is not available
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.messageInput,
                    onValueChange = viewModel::updateMessageInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.message_placeholder)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { viewModel.sendMessage() }
                    ),
                    enabled = !conversationState.isLoading
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.sendMessage() },
                    enabled = viewModel.messageInput.isNotBlank() && !conversationState.isLoading
                ) {
                    Text(stringResource(Res.string.send_button))
                }
            }
        }

        // Bottom navigation bar - fixed at bottom
        Column {
            // Gray line divider at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Event_note,
                            contentDescription = stringResource(Res.string.cd_calendars)
                        )
                    },
                    label = { Text(stringResource(Res.string.nav_calendar)) },
                    selected = selectedTab == BottomNavTab.CALENDARS,
                    onClick = {
                        selectedTab = BottomNavTab.CALENDARS
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Chat, contentDescription = stringResource(Res.string.cd_chat)) },
                    label = { Text(stringResource(Res.string.nav_chat)) },
                    selected = selectedTab == BottomNavTab.CHAT,
                    onClick = { selectedTab = BottomNavTab.CHAT }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Settings,
                            contentDescription = stringResource(Res.string.cd_settings)
                        )
                    },
                    label = { Text(stringResource(Res.string.nav_settings)) },
                    selected = selectedTab == BottomNavTab.SETTINGS,
                    onClick = { selectedTab = BottomNavTab.SETTINGS }
                )
            }
        }
    }
}

/*@Composable
private fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val arrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF65C466) // Green color for agent messages
    }
    val contentColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color.White // White text for agent messages
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = arrangement
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = contentColor,
                fontSize = 14.sp
            )
        }
    }
}*/

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 320.dp
) {
    val isUser = message.isUser
    val bg = if (isUser) MaterialTheme.colorScheme.primary else Color(0xFF65C466)
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimary else Color.White

    val shape =
        if (isUser) {
            RoundedCornerShape(
                topStart = 16.dp, topEnd = 12.dp,
                bottomEnd = 0.dp, bottomStart = 16.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 12.dp, topEnd = 16.dp,
                bottomEnd = 16.dp, bottomStart = 0.dp
            )
        }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bg,
            contentColor = fg,
            shape = shape,
            modifier = Modifier.widthIn(max = maxWidth)
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    conversationState: ConversationState,
    viewModel: CalendarViewModel,
    userProfile: com.smartcal.app.models.UserProfileResponse?,
    listState: LazyListState,
    messagesWithLocalizedWelcome: List<ChatMessage>,
    creditsRemaining: Int? = null,
    daysUntilRenewal: Int? = null,
    onUpgradePlan: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.title_calendar_assistant),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        userProfile?.fullName?.takeIf { it.isNotEmpty() }?.let { fullName ->
                            Text(
                                text = fullName,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                actions = {
                    // Show credits remaining
                    creditsRemaining?.let { credits ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Contactless,
                                contentDescription = "Credits",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = credits.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Show Clear button when there are actual conversation messages (excluding welcome message)
                    val hasRealMessages = conversationState.messages.any { message ->
                        message.content != "WELCOME_MESSAGE_PLACEHOLDER" && 
                        !message.content.startsWith("ERROR_PLACEHOLDER:") &&
                        !message.content.contains("🗓️ Hi! I'm your Calendar Assistant") &&
                        !message.content.contains("🗓️ ¡Hola! Soy tu Asistente de Calendario") &&
                        !message.content.contains("🗓️ Bonjour ! Je suis votre assistant de calendrier")
                    }
                    if (hasRealMessages) {
                        TextButton(onClick = { viewModel.clearConversation() }) {
                            Text(stringResource(Res.string.clear_button))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        ChatContent(
            conversationState = conversationState,
            viewModel = viewModel,
            listState = listState,
            messagesWithLocalizedWelcome = messagesWithLocalizedWelcome,
            paddingValues = paddingValues,
            onUpgradePlan = onUpgradePlan,
            daysUntilRenewal
        )
    }
}

@Composable
private fun ChatContent(
    conversationState: ConversationState,
    viewModel: CalendarViewModel,
    listState: LazyListState,
    messagesWithLocalizedWelcome: List<ChatMessage>,
    paddingValues: PaddingValues,
    onUpgradePlan: () -> Unit = {},
    daysUntilRenewal: Int?
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
            .pointerInput(Unit) {
                detectTapGestures {
                    // Hide keyboard when tapping anywhere in the chat area
                    keyboardController?.hide()
                }
            }
    ) {
        when {
            conversationState.showCreditsExhausted -> {
                // Credits exhausted state
                CreditsExhaustedCard(
                    daysUntilRenewal = daysUntilRenewal,
                    onUpgradePlan = onUpgradePlan,
                    onDismiss = { viewModel.dismissCreditsExhausted() }
                )
            }

            conversationState.isLoading && conversationState.messages.isEmpty() -> {
                // Loading state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(Res.string.initializing_agent))
                }
            }

            conversationState.error != null && conversationState.messages.isEmpty() -> {
                // Error state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        getErrorMessage(conversationState.error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = { viewModel.clearConversation() }) {
                        Text(stringResource(Res.string.clear_button))
                    }
                }
            }

            else -> {
                // —— Variables para streaming ——
                val streamingText = conversationState.streamingMessage.orEmpty()
                val showStream = streamingText.isNotBlank()
                val showTyping = conversationState.isLoading && !showStream

                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messagesWithLocalizedWelcome) { message ->
                        MessageBubble(message = message)
                    }

                    // —— ÚNICA burbuja del asistente mientras responde ——
                    if (showStream || showTyping) {
                        item {
                            StreamingAssistantBubble(
                                text = streamingText,                 // se muestra si hay stream
                                showTyping = showTyping,              // sino, puntito titilando
                                bubbleColor = Color(0xFF65C466),      // mismo verde del agente
                                textColor = Color.White
                            )
                        }
                    }
                }

                // Auto-scroll mientras llega el stream
                LaunchedEffect(streamingText, showTyping) {
                    if (showStream || showTyping) {
                        // Scroll to the streaming message position
                        val targetIndex = kotlin.math.max(0, messagesWithLocalizedWelcome.size)
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val animatedAlpha = infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200 // Delay cada punto
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = Color.White.copy(alpha = animatedAlpha.value),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun StreamingAssistantBubble(
    text: String,
    showTyping: Boolean,
    bubbleColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (text.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            text = text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (showTyping) {
                    TypingIndicator()
                }
            }
        }
    }
}

@Composable
private fun CreditsExhaustedCard(
    daysUntilRenewal: Int? = null,
    onUpgradePlan: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = Contactless,
                    contentDescription = "Credits",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = stringResource(Res.string.credits_exhausted_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = when (daysUntilRenewal) {
                        0 -> stringResource(Res.string.credits_exhausted_message_renew_today)
                        1 -> stringResource(Res.string.credits_exhausted_message_renew_tomorrow)
                        in 2..Int.MAX_VALUE -> stringResource(Res.string.credits_exhausted_message_with_renewal, daysUntilRenewal ?: 0)
                        else -> stringResource(Res.string.credits_exhausted_message)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.credits_exhausted_dismiss))
                    }

                    Button(
                        onClick = onUpgradePlan,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.credits_exhausted_button))
                    }
                }
            }
        }
    }
}