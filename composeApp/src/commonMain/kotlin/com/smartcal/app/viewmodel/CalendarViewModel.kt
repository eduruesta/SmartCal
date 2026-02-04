package com.smartcal.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcal.app.reminders.ReminderManager
import com.smartcal.app.repository.CalendarRepository
import com.smartcal.app.repository.ConversationState
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val repository: CalendarRepository,
    private val reminderManager: ReminderManager
) : ViewModel() {
    private val calendarService = com.smartcal.app.services.CalendarService()
    
    // Callback for when token expires during chat
    private var onTokenExpiredCallback: (() -> Unit)? = null
    
    // Callback for when credits/subscription info is updated
    private var onCreditsUpdateCallback: ((Int?, String?) -> Unit)? = null
    
    // Callback for when user profile info is updated
    private var onUserProfileUpdateCallback: ((String?, String?) -> Unit)? = null
    
    fun setOnTokenExpiredCallback(callback: () -> Unit) {
        onTokenExpiredCallback = callback
        repository.setOnTokenExpiredCallback(callback)
    }
    
    fun setOnCreditsUpdateCallback(callback: (Int?, String?) -> Unit) {
        println("🔗 CalendarViewModel: setOnCreditsUpdateCallback called")
        onCreditsUpdateCallback = callback
        repository.setOnCreditsUpdateCallback(callback)
    }
    
    fun setOnUserProfileUpdateCallback(callback: (String?, String?) -> Unit) {
        println("🔗 CalendarViewModel: setOnUserProfileUpdateCallback called")
        onUserProfileUpdateCallback = callback
    }
    
    // Expose the repository's state
    val conversationState: StateFlow<ConversationState> = repository.conversationState
    
    // UI state for input
    var messageInput by mutableStateOf("")
        private set
    
    // User profile state
    var userProfile by mutableStateOf<com.smartcal.app.models.UserProfileResponse?>(null)
        private set
    
    // Calendar and events state
    var calendarsResponse by mutableStateOf<com.smartcal.app.models.CalendarsResponse?>(null)
        private set
        
    var allEventsResponses by mutableStateOf<Map<String, com.smartcal.app.models.EventsResponse>>(emptyMap())
        private set
        
    var isLoadingCalendar by mutableStateOf(false)
        private set
        
    var isLoadingProfile by mutableStateOf(false)
        private set
        
    var isRefreshing by mutableStateOf(false)
        private set
        
    var calendarError by mutableStateOf<String?>(null)
        private set
        
    var enabledCalendars by mutableStateOf<Set<String>>(emptySet())
        private set
    
    // Session token for authentication
    private var sessionToken: String? = null

    init {
        viewModelScope.launch {
            loadCalendarData()
        }
    }
    
    fun setSessionToken(token: String) {
        sessionToken = token
        repository.setSessionToken(token)
        // Initialize welcome message since backend agent is ready
        (repository as? com.smartcal.app.repository.CalendarRepositoryImpl)?.initializeWelcomeIfNeeded()
        // Set up callbacks if available
        onTokenExpiredCallback?.let { 
            println("🔗 CalendarViewModel: Configuring token expired callback")
            repository.setOnTokenExpiredCallback(it) 
        }
        onCreditsUpdateCallback?.let { 
            println("🔗 CalendarViewModel: Configuring credits update callback")
            repository.setOnCreditsUpdateCallback(it) 
        } ?: println("⚠️ CalendarViewModel: No credits update callback available")
        onUserProfileUpdateCallback?.let { 
            println("🔗 CalendarViewModel: Configuring user profile update callback")
        } ?: println("⚠️ CalendarViewModel: No user profile update callback available")
    }
    
    fun updateMessageInput(input: String) {
        messageInput = input
    }
    
    fun sendMessage() {
        if (messageInput.isBlank()) return
        
        val message = messageInput
        messageInput = ""
        
        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }
    
    // Backend agent handles initialization - no local setup needed
    
    fun clearConversation() {
        repository.clearConversation()
    }
    
    fun dismissCreditsExhausted() {
        repository.dismissCreditsExhausted()
    }
    
    
    fun retry() {
        // Simply clear conversation - backend agent is always available
        clearConversation()
    }
    
    suspend fun getCalendars(): Result<com.smartcal.app.models.CalendarsResponse> {
        return repository.getCalendars()
    }
    
    suspend fun getEvents(
        calendarId: String = "primary",
        timeMin: String? = null,
        timeMax: String? = null
    ): Result<com.smartcal.app.models.EventsResponse> {
        return repository.getEvents(calendarId, timeMin, timeMax)
    }
    
    suspend fun getUpcomingEvents(): Result<com.smartcal.app.models.EventsResponse> {
        return repository.getUpcomingEvents()
    }
    
    /**
     * Load calendar data (user profile, calendars, and events)
     * Called once when ViewModel is initialized with session token
     */
    private suspend fun loadCalendarData() {
        if (isLoadingCalendar) return
        
        isLoadingCalendar = true
        calendarError = null
        
        try {
            // First fetch user profile
            getUserProfile()
            
            // Small delay to ensure profile data is updated
            delay(500)
            
            // Get and process calendars
            val calendarsResult = getCalendars()
            calendarsResult.fold(
                onSuccess = { response ->
                    calendarsResponse = response
                    val calendars = response.calendars.orEmpty()
                    
                    // Filter calendars based on subscription plan
                    val filteredCalendars = filterCalendarsBySubscription(calendars)
                    
                    // Enable filtered calendars
                    enabledCalendars = filteredCalendars.map { it.id }.toSet()
                    println("🔍 Enabled calendars set: $enabledCalendars")
                    
                    // Load events for filtered calendars and wait for completion
                    loadEventsForCalendars(filteredCalendars)
                    
                    // Only mark loading as complete after all events are loaded
                    isLoadingCalendar = false
                },
                onFailure = { e ->
                    handleCalendarLoadingError(e)
                    isLoadingCalendar = false
                }
            )
        } catch (e: Exception) {
            calendarError = e.message
            isLoadingCalendar = false
        }
    }
    
    /**
     * Filter calendars based on user's subscription plan
     * Free/Starter plans get only primary calendars, Pro plans get all calendars
     */
    private fun filterCalendarsBySubscription(calendars: List<com.smartcal.app.models.Calendar>): List<com.smartcal.app.models.Calendar> {
        val effectiveSubscriptionPlan = userProfile?.subscriptionPlan
        println("🔍 Using effective subscription plan: '$effectiveSubscriptionPlan'")
        
        val filteredCalendars = if (effectiveSubscriptionPlan?.contains("Free") == true || 
                                    effectiveSubscriptionPlan?.contains("Starter") == true) {
            // For Free and Starter plans, only show primary calendar
            val primaryCalendars = calendars.filter { it.primary == true }
            println("🔍 Primary calendars found: ${primaryCalendars.size}")
            primaryCalendars.forEach { cal ->
                println("🔍 Primary calendar: id=${cal.id}, summary=${cal.summary}, primary=${cal.primary}")
            }
            
            // 🚨 FALLBACK: If no primary calendar found, use the first calendar as fallback
            if (primaryCalendars.isEmpty() && calendars.isNotEmpty()) {
                println("⚠️ No primary calendar found! Using first calendar as fallback: ${calendars.first().summary}")
                listOf(calendars.first())
            } else {
                primaryCalendars
            }
        } else {
            // For Pro plan, show all calendars
            calendars
        }
        
        println("🔍 Filtered calendars: ${filteredCalendars.size}")
        filteredCalendars.forEach { cal ->
            println("🔍 Will enable calendar: id=${cal.id}, summary=${cal.summary}")
        }
        
        return filteredCalendars
    }
    
    /**
     * Load events for all provided calendars
     * Updates UI immediately as each calendar's events are loaded
     */
    private suspend fun loadEventsForCalendars(calendars: List<com.smartcal.app.models.Calendar>) {
        val eventsMap = allEventsResponses.toMutableMap()
        val allUniqueEvents = mutableMapOf<String, com.smartcal.app.models.Event>()
        
        println("🔍 Loading events for ${calendars.size} filtered calendars...")
        calendars.forEach { calendar ->
            println("📅 Loading events for calendar: id=${calendar.id}, summary=${calendar.summary}")
            getEvents(calendar.id).fold(
                onSuccess = { eventsResponse ->
                    val eventsCount = eventsResponse.events?.size ?: 0
                    println("✅ Loaded $eventsCount events for calendar ${calendar.id}")
                    eventsMap[calendar.id] = eventsResponse
                    
                    // Collect unique events for reminder sync (avoid duplicates)
                    eventsResponse.events?.forEach { event ->
                        allUniqueEvents[event.id] = event
                    }
                    
                    // Update UI immediately with each calendar's events
                    allEventsResponses = eventsMap.toMap()
                    println("📊 CalendarViewModel: Updated allEventsResponses - now contains ${allEventsResponses.size} calendars")
                    println("📊 CalendarViewModel: allEventsResponses keys: ${allEventsResponses.keys}")
                    allEventsResponses.forEach { (calId, resp) ->
                        println("📊 CalendarViewModel: Calendar $calId now has ${resp.events?.size ?: 0} events in UI state")
                    }
                    
                    // Debug: Show first few events
                    eventsResponse.events?.take(3)?.forEach { event ->
                        println("   📌 Event: ${event.summary} at ${event.start?.dateTime ?: event.start?.date}")
                    }
                },
                onFailure = { e ->
                    handleEventLoadingError(e, calendar.id)
                }
            )
        }
        
        // Sync reminders only once for all unique events
        if (allUniqueEvents.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val uniqueEventsResponse = com.smartcal.app.models.EventsResponse(
                        success = true,
                        events = allUniqueEvents.values.toList()
                    )
                    reminderManager.syncEvents(uniqueEventsResponse)
                    println("🔔 Recordatorios sincronizados para ${allUniqueEvents.size} eventos únicos")
                } catch (e: Exception) {
                    println("❌ Error sincronizando recordatorios: ${e.message}")
                }
            }
        }
        
        // Final summary
        printEventLoadingSummary(eventsMap)
    }
    
    /**
     * Handle errors that occur during calendar loading
     */
    private fun handleCalendarLoadingError(e: Throwable) {
        if (e is com.smartcal.app.services.UnauthorizedException) {
            println("🔒 Token expired loading calendars")
            onTokenExpiredCallback?.invoke()
        } else {
            val errorMessage = mapNetworkError(e, "Failed to load calendars")
            calendarError = errorMessage
            println("🌐 Network error loading calendars: $errorMessage")
        }
    }
    
    /**
     * Handle errors that occur during event loading for a specific calendar
     */
    private suspend fun handleEventLoadingError(e: Throwable, calendarId: String) {
        println("❌ Failed to load events for calendar $calendarId: ${e.message}")
        if (e is com.smartcal.app.services.UnauthorizedException) {
            println("🔒 Token expired loading events for $calendarId")
            onTokenExpiredCallback?.invoke()
            return
        } else {
            val errorMessage = mapNetworkError(e, "Network error occurred")
            calendarError = errorMessage
            println("🌐 Network error for calendar $calendarId: $errorMessage")
        }
    }
    
    /**
     * Map network exceptions to user-friendly error messages
     */
    private fun mapNetworkError(e: Throwable, defaultMessage: String): String {
        return when {
            e.message?.contains("Unable to resolve host") == true -> 
                "No internet connection. Please check your network."
            e.message?.contains("timeout") == true -> 
                "Connection timeout. Please try again."
            else -> e.message ?: defaultMessage
        }
    }
    
    /**
     * Print summary of loaded events for debugging
     */
    private fun printEventLoadingSummary(eventsMap: Map<String, com.smartcal.app.models.EventsResponse>) {
        val totalEvents = eventsMap.values.sumOf { it.events?.size ?: 0 }
        println("🎯 Final result: $totalEvents total events loaded from ${eventsMap.size} calendars")
        eventsMap.forEach { (calendarId, response) ->
            println("   📊 Calendar $calendarId: ${response.events?.size ?: 0} events")
        }
    }
    
    /**
     * Refresh calendar data (called by pull to refresh)
     */
    suspend fun refreshCalendarData() {
        if (isRefreshing || isLoadingCalendar) return
        
        isRefreshing = true
        
        try {
            // Reset states before refreshing
            calendarError = null
            
            // First fetch user profile
            getUserProfile()
            
            // Small delay to ensure profile data is updated
            delay(500)
            
            // Get and process calendars
            val calendarsResult = getCalendars()
            calendarsResult.fold(
                onSuccess = { response ->
                    calendarsResponse = response
                    val calendars = response.calendars.orEmpty()
                    
                    // Filter calendars based on subscription plan
                    val filteredCalendars = filterCalendarsBySubscription(calendars)
                    
                    // Enable filtered calendars
                    enabledCalendars = filteredCalendars.map { it.id }.toSet()
                    println("🔍 Enabled calendars set (refresh): $enabledCalendars")
                    
                    // Load events for filtered calendars and wait for completion
                    loadEventsForCalendars(filteredCalendars)
                },
                onFailure = { e ->
                    handleCalendarLoadingError(e)
                }
            )
        } catch (e: Exception) {
            calendarError = e.message
        } finally {
            isRefreshing = false
        }
    }
    
    /**
     * Toggle calendar enabled state
     */
    fun toggleCalendar(calendarId: String, enabled: Boolean) {
        enabledCalendars = if (enabled) {
            enabledCalendars + calendarId
        } else {
            enabledCalendars - calendarId
        }
    }
    
    /**
     * Fetch user profile information
     */
    suspend fun getUserProfile(): Result<com.smartcal.app.models.UserProfileResponse> {
        return try {
            isLoadingProfile = true
            val result = repository.getUserProfile()
            if (result.isSuccess) {
                val profile = result.getOrNull()
                userProfile = profile
                println("✅ User profile loaded: ${profile?.firstName}")
                
                // Update credits and subscription plan from profile
                if (profile?.creditsRemaining != null || profile?.subscriptionPlan != null) {
                    println("🔄 CalendarViewModel: Updating credits/subscription from profile")
                    println("   creditsRemaining: ${profile.creditsRemaining}")
                    println("   subscriptionPlan: ${profile.subscriptionPlan}")
                    onCreditsUpdateCallback?.invoke(profile.creditsRemaining, profile.subscriptionPlan)
                }
                
                // Update user profile info (fullName, profilePicture)
                if (profile?.fullName != null || profile?.profilePicture != null) {
                    println("👤 CalendarViewModel: Updating user profile from profile")
                    println("   fullName: ${profile.fullName}")
                    println("   profilePicture: ${profile.profilePicture}")
                    onUserProfileUpdateCallback?.invoke(profile.fullName, profile.profilePicture)
                }
            }
            result
        } catch (e: Exception) {
            println("❌ Failed to load user profile: ${e.message}")
            Result.failure(e)
        } finally {
            isLoadingProfile = false
        }
    }
    
    /**
     * Fetch detailed credits information including renewal date
     */
    suspend fun getUserCreditsInfo(): Result<com.smartcal.app.models.CreditsInfoResponse> {
        return try {
            val result = repository.getUserCreditsInfo()
            if (result.isSuccess) {
                val creditsInfo = result.getOrNull()
                println("✅ Credits info loaded:")
                println("   creditsRemaining: ${creditsInfo?.creditsRemaining}")
                println("   subscriptionPlan: ${creditsInfo?.subscriptionPlan}")
                println("   nextRenewalDate: ${creditsInfo?.nextRenewalDate}")
                println("   daysUntilRenewal: ${creditsInfo?.daysUntilRenewal}")
            }
            result
        } catch (e: Exception) {
            println("❌ Failed to load credits info: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Fetch contacts using CalendarService and stored session token
     */
    suspend fun fetchContacts(): Result<com.smartcal.app.models.ContactsResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        return calendarService.getContacts(token)
    }

    /**
     * Update an existing calendar event
     */
    suspend fun updateEvent(
        eventId: String,
        request: com.smartcal.app.models.UpdateEventRequest
    ): Result<com.smartcal.app.models.UpdateEventResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        val result = calendarService.updateEvent(token, eventId, request)
        
        // Resincronizar recordatorios después de actualizar un evento
        if (result.isSuccess) {
            try {
                refreshCalendarData()
                println("🔔 Recordatorios resincionizados después de actualizar evento $eventId")
            } catch (e: Exception) {
                println("❌ Error resincronizando recordatorios después de actualizar evento: ${e.message}")
            }
        }
        
        return result
    }

    /**
     * Create a new calendar event
     */
    suspend fun createEvent(
        request: com.smartcal.app.models.CreateEventRequest
    ): Result<com.smartcal.app.models.CreateEventResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        val result = calendarService.createEvent(token, request)
        
        // Resincronizar recordatorios después de crear un evento
        if (result.isSuccess) {
            try {
                refreshCalendarData()
                println("🔔 Recordatorios sincronizados después de crear nuevo evento")
            } catch (e: Exception) {
                println("❌ Error sincronizando recordatorios después de crear evento: ${e.message}")
            }
        }
        
        return result
    }

    /**
     * Delete a calendar event
     */
    suspend fun deleteEvent(
        eventId: String,
        calendarId: String = "primary"
    ): Result<com.smartcal.app.models.DeleteEventResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        val result = calendarService.deleteEvent(token, eventId, calendarId)
        
        // Resincronizar recordatorios después de eliminar un evento
        if (result.isSuccess) {
            try {
                refreshCalendarData()
                println("🔔 Recordatorios sincronizados después de eliminar evento $eventId")
            } catch (e: Exception) {
                println("❌ Error sincronizando recordatorios después de eliminar evento: ${e.message}")
            }
        }
        
        return result
    }

    // Clean up when view model is no longer needed
    override fun onCleared() {
        viewModelScope.cancel()
    }
}