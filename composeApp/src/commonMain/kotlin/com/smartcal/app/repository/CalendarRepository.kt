package com.smartcal.app.repository

// Removed agent-related imports - now using backend API (kept for backend path)
import com.smartcal.app.services.UnauthorizedException
import com.smartcal.app.services.InsufficientCreditsException
import com.smartcal.app.utils.getErrorMessageKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import com.smartcal.app.config.AppFeatures
import com.smartcal.app.services.FrontendMessageService
import com.smartcal.app.models.MessageRequest

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

data class ConversationState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val streamingMessage: String? = null, // Mensaje temporal mientras hace streaming
    val showCreditsExhausted: Boolean = false // Indica si mostrar mensaje de créditos agotados
)

interface CalendarRepository {
    val conversationState: StateFlow<ConversationState>
    suspend fun sendMessage(message: String): Result<String>
    suspend fun sendMessageStreaming(message: String): Result<String>
    fun clearConversation()
    fun setSessionToken(token: String)
    suspend fun getUserProfile(): Result<com.smartcal.app.models.UserProfileResponse>
    suspend fun getUserCreditsInfo(): Result<com.smartcal.app.models.CreditsInfoResponse>
    suspend fun getCalendars(): Result<com.smartcal.app.models.CalendarsResponse>
    suspend fun getEvents(calendarId: String = "primary", timeMin: String? = null, timeMax: String? = null): Result<com.smartcal.app.models.EventsResponse>
    suspend fun getUpcomingEvents(): Result<com.smartcal.app.models.EventsResponse>
    fun setOnTokenExpiredCallback(callback: () -> Unit)
    fun setOnCreditsUpdateCallback(callback: (Int?, String?) -> Unit)
    fun dismissCreditsExhausted()
}

class CalendarRepositoryImpl(
    private val frontendMessageService: FrontendMessageService = FrontendMessageService(),
) : CalendarRepository {
    
    private val _conversationState = MutableStateFlow(ConversationState())
    override val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()
    
    private var sessionToken: String? = null
    private var userProfile: com.smartcal.app.models.UserProfileResponse? = null
    private val calendarService = com.smartcal.app.services.CalendarService()
    private var conversationId: String? = null
    private var onTokenExpiredCallback: (() -> Unit)? = null
    private var onCreditsUpdateCallback: ((Int?, String?) -> Unit)? = null
    
    override fun setOnTokenExpiredCallback(callback: () -> Unit) {
        onTokenExpiredCallback = callback
    }
    
    override fun setOnCreditsUpdateCallback(callback: (Int?, String?) -> Unit) {
        onCreditsUpdateCallback = callback
    }
    
    override fun setSessionToken(token: String) {
        sessionToken = token
    }
    
    // Backend API handles agent initialization - no local setup needed
    
    override suspend fun sendMessage(message: String): Result<String> {
        // Use streaming version for better UX
        return sendMessageStreaming(message)
    }
    
    override suspend fun sendMessageStreaming(message: String): Result<String> {
        return try {
            _conversationState.value = _conversationState.value.copy(isLoading = true, error = null)

            // Add user message
            addMessage(ChatMessage(content = message, isUser = true))

            if (AppFeatures.runAgentLocally) {
                // In-app path: call local FrontendMessageService with MCP tools
                val req = MessageRequest(message = message, conversationId = conversationId)
                val userEmail = userProfile?.email
                val messageResponse = frontendMessageService.send(req, userEmail, sessionToken)

                // Handle credits exhausted (same UX as backend 402)
                if (!messageResponse.success && messageResponse.showCreditsExhausted == true) {
                    println("💳 Insufficient credits in local agent - showing credits exhausted message")
                    _conversationState.value = _conversationState.value.copy(
                        isLoading = false,
                        error = null,
                        streamingMessage = null,
                        showCreditsExhausted = true
                    )
                    onCreditsUpdateCallback?.invoke(messageResponse.creditsRemaining, messageResponse.subscriptionPlan)
                    return Result.failure(InsufficientCreditsException(
                        messageResponse.message,
                        messageResponse.creditsRemaining,
                        messageResponse.subscriptionPlan
                    ))
                }

                // Handle other failures (LLM error, session error, etc.)
                if (!messageResponse.success) {
                    _conversationState.value = _conversationState.value.copy(
                        isLoading = false,
                        error = messageResponse.message,
                        streamingMessage = null,
                        showCreditsExhausted = false
                    )
                    addMessage(ChatMessage(content = messageResponse.message, isUser = false))
                    return Result.failure(Exception(messageResponse.message))
                }

                // Start simulated streaming with empty message
                _conversationState.value = _conversationState.value.copy(streamingMessage = "")

                val response = messageResponse.message
                val words = response.split(" ")
                var currentText = ""
                for (word in words) {
                    currentText += if (currentText.isEmpty()) word else " $word"
                    _conversationState.value = _conversationState.value.copy(streamingMessage = currentText)
                    delay(35) // slightly faster for local stub
                }
                _conversationState.value = _conversationState.value.copy(streamingMessage = null, isLoading = false)

                // Update conversation ID
                conversationId = messageResponse.conversationId ?: conversationId

                // Update credits and subscription if provided
                if (messageResponse.creditsRemaining != null || messageResponse.subscriptionPlan != null) {
                    onCreditsUpdateCallback?.invoke(messageResponse.creditsRemaining, messageResponse.subscriptionPlan)
                }

                // Add final agent response
                addMessage(ChatMessage(content = response, isUser = false))
                return Result.success(response)
            } else {
                // Backend API path (current behavior)
                val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
                val result = calendarService.sendMessage(token, message, conversationId)

                result.fold(
                    onSuccess = { messageResponse ->
                        // Start simulated streaming with empty message
                        _conversationState.value = _conversationState.value.copy(streamingMessage = "")

                        // Simulate typewriter effect with backend response
                        val response = messageResponse.message
                        val words = response.split(" ")
                        var currentText = ""

                        for (word in words) {
                            currentText += if (currentText.isEmpty()) word else " $word"
                            _conversationState.value = _conversationState.value.copy(
                                streamingMessage = currentText
                            )
                            delay(50) // 50ms delay between words
                        }

                        // Clear streaming message and add final message
                        _conversationState.value = _conversationState.value.copy(
                            streamingMessage = null,
                            isLoading = false
                        )

                        // Update conversation ID if provided
                        conversationId = messageResponse.conversationId ?: conversationId

                        // Update credits and subscription if provided
                        println("📊 CalendarRepository: MessageResponse received")
                        println("   creditsRemaining: ${'$'}{messageResponse.creditsRemaining}")
                        println("   subscriptionPlan: ${'$'}{messageResponse.subscriptionPlan}")
                        if (messageResponse.creditsRemaining != null || messageResponse.subscriptionPlan != null) {
                            println("🔗 CalendarRepository: Calling credits update callback")
                            onCreditsUpdateCallback?.invoke(messageResponse.creditsRemaining, messageResponse.subscriptionPlan)
                        } else {
                            println("⚠️ CalendarRepository: No credits/subscription data in response")
                        }

                        // Add final agent response
                        addMessage(ChatMessage(content = response, isUser = false))

                        Result.success(response)
                    },
                    onFailure = { e ->
                        if (e is UnauthorizedException) {
                            println("🔒 Token expired in sendMessage - triggering logout")
                            onTokenExpiredCallback?.invoke()
                        } else if (e is InsufficientCreditsException) {
                            println("💳 Insufficient credits in sendMessage - showing credits exhausted message")
                            _conversationState.value = _conversationState.value.copy(
                                isLoading = false,
                                error = null,
                                streamingMessage = null,
                                showCreditsExhausted = true
                            )
                            onCreditsUpdateCallback?.invoke(e.creditsRemaining, e.subscriptionPlan)
                        } else {
                            val errorKey = getErrorMessageKey(e.message)
                            val errorMessage = "ERROR_PLACEHOLDER:${'$'}errorKey"
                            _conversationState.value = _conversationState.value.copy(
                                isLoading = false,
                                error = errorMessage,
                                streamingMessage = null,
                                showCreditsExhausted = false
                            )
                            addMessage(ChatMessage(content = errorMessage, isUser = false))
                        }
                        Result.failure(e)
                    }
                )
            }
        } catch (e: Exception) {
            val errorKey = getErrorMessageKey(e.message)
            val errorMessage = "ERROR_PLACEHOLDER:${'$'}errorKey"
            _conversationState.value = _conversationState.value.copy(
                isLoading = false,
                error = errorMessage,
                streamingMessage = null
            )
            addMessage(ChatMessage(content = errorMessage, isUser = false))
            Result.failure(e)
        }
    }
    
    override fun clearConversation() {
        println("🧹 CalendarRepository: Clearing conversation")
        // Clear cached MCP agent so the connection is released (e.g. on logout)
        frontendMessageService.clearAgentCache(userEmail = userProfile?.email, sessionToken = sessionToken)
        // Reset conversation but keep the welcome message placeholder
        _conversationState.value = ConversationState(
            messages = listOf(ChatMessage(content = "WELCOME_MESSAGE_PLACEHOLDER", isUser = false))
        )
        conversationId = null
    }
    
    override fun dismissCreditsExhausted() {
        _conversationState.value = _conversationState.value.copy(showCreditsExhausted = false)
    }
    
    override suspend fun getUserProfile(): Result<com.smartcal.app.models.UserProfileResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        
        return try {
            val response = calendarService.getUserProfile(token)
            userProfile = response
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUserCreditsInfo(): Result<com.smartcal.app.models.CreditsInfoResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        
        return try {
            val response = calendarService.getUserCreditsInfo(token)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCalendars(): Result<com.smartcal.app.models.CalendarsResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        return calendarService.getCalendars(token)
    }
    
    override suspend fun getEvents(
        calendarId: String,
        timeMin: String?,
        timeMax: String?
    ): Result<com.smartcal.app.models.EventsResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        return calendarService.getEvents(token, calendarId, timeMin, timeMax)
    }
    
    override suspend fun getUpcomingEvents(): Result<com.smartcal.app.models.EventsResponse> {
        val token = sessionToken ?: return Result.failure(Exception("Session token not set"))
        return calendarService.getUpcomingEvents(token)
    }
    
    fun addMessage(message: ChatMessage) {
        _conversationState.value = _conversationState.value.copy(
            messages = _conversationState.value.messages + message
        )
    }
    
    fun addWelcomeMessage(welcomeText: String) {
        if (_conversationState.value.messages.isEmpty()) {
            addMessage(ChatMessage(content = welcomeText, isUser = false))
        }
    }
    
    fun initializeWelcomeIfNeeded() {
        // Add welcome message placeholder if no messages exist - backend agent is ready
        if (_conversationState.value.messages.isEmpty()) {
            println("💬 Adding welcome message placeholder - backend agent ready")
            addMessage(ChatMessage(content = "WELCOME_MESSAGE_PLACEHOLDER", isUser = false))
        }
    }
}