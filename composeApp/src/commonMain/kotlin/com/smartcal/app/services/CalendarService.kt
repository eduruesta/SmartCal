package com.smartcal.app.services

import com.smartcal.app.models.CalendarsResponse
import com.smartcal.app.models.EventsResponse
import com.smartcal.app.models.UserProfileResponse
import com.smartcal.app.models.MessageRequest
import com.smartcal.app.models.MessageResponse
import com.smartcal.app.models.CreateEventRequest
import com.smartcal.app.models.CreateEventResponse
import com.smartcal.app.models.UpdateEventRequest
import com.smartcal.app.models.UpdateEventResponse
import com.smartcal.app.models.DeleteEventResponse
import com.smartcal.app.models.ContactsResponse
import com.smartcal.app.models.CreditsInfoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Custom exception for 402 Payment Required
class InsufficientCreditsException(message: String) : Exception(message)

class CalendarService {
    private val baseUrl = "https://calendar-backend-y1rx.onrender.com"
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120000  // Increased to 120s for AI agent processing
            connectTimeoutMillis = 15000   // Keep 15s for connection
            socketTimeoutMillis = 120000   // Increased to 120s for AI agent processing
        }
    }
    
    /**
     * Fetch user's calendars from the backend
     * @param sessionToken User's session token for authentication
     * @return Result with CalendarsResponse containing list of calendars
     */
    suspend fun getCalendars(sessionToken: String): Result<CalendarsResponse> {
        return try {
            println("📅 Fetching calendars for user...")
            
            val response = client.get("$baseUrl/calendars") {
                header("Session-Token", sessionToken)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val calendarsResponse = response.body<CalendarsResponse>()
                println("✅ Successfully fetched ${calendarsResponse.calendars?.size ?: 0} calendars")
                Result.success(calendarsResponse)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                // Handle 401 Unauthorized - token expired or invalid
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown 401 error" }
                println("🔒 401 Unauthorized in getCalendars: $errorBody")
                Result.failure(UnauthorizedException("Invalid or expired session"))
            } else {
                println("❌ Get calendars failed with status ${response.status}")
                Result.failure(Exception("Get calendars failed with status ${response.status}"))
            }
        } catch (_: SocketTimeoutException) {
            println("⏰ Calendar server timeout")
            Result.failure(Exception("Server is not responding while fetching calendars."))
        } catch (e: Exception) {
            println("❌ Calendar fetch error: ${e.message}")
            Result.failure(Exception("Get calendars error: ${e.message}"))
        }
    }
    
    /**
     * Fetch events from a specific calendar
     * @param sessionToken User's session token for authentication
     * @param calendarId Calendar ID to fetch events from (defaults to "primary")
     * @param timeMin Start time for events query (ISO 8601 format)
     * @param timeMax End time for events query (ISO 8601 format)
     * @return Result with EventsResponse containing list of events
     */
    suspend fun getEvents(
        sessionToken: String,
        calendarId: String = "primary",
        timeMin: String? = null,
        timeMax: String? = null
    ): Result<EventsResponse> {
        return try {
            println("📅 Fetching events from calendar '$calendarId'...")
            if (timeMin != null) println("  📅 Time range: $timeMin to ${timeMax ?: "now"}")
            
            val response = client.get("$baseUrl/events") {
                header("Session-Token", sessionToken)
                if (calendarId != "primary") {
                    parameter("calendarId", calendarId)
                }
                timeMin?.let { parameter("timeMin", it) }
                timeMax?.let { parameter("timeMax", it) }
            }
            
            if (response.status == HttpStatusCode.OK) {
                val eventsResponse = response.body<EventsResponse>()
                println("✅ Successfully fetched ${eventsResponse.events?.size ?: 0} events")
                Result.success(eventsResponse)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                // Handle 401 Unauthorized - token expired or invalid
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown 401 error" }
                println("🔒 401 Unauthorized in getEvents: $errorBody")
                Result.failure(UnauthorizedException("Invalid or expired session"))
            } else {
                println("❌ Get events failed with status ${response.status}")
                Result.failure(Exception("Get events failed with status ${response.status}"))
            }
        } catch (_: SocketTimeoutException) {
            println("⏰ Events server timeout")
            Result.failure(Exception("Server is not responding while fetching events."))
        } catch (e: Exception) {
            println("❌ Events fetch error: ${e.message}")
            Result.failure(Exception("Get events error: ${e.message}"))
        }
    }
    
    /**
     * Get events for the next 30 days from primary calendar
     * Convenience method that sets up default time range
     */
    suspend fun getUpcomingEvents(sessionToken: String): Result<EventsResponse> {
        // Get events for the next 30 days - using simple approach without datetime calculations
        return getEvents(
            sessionToken = sessionToken,
            calendarId = "primary",
            timeMin = null, // Let backend handle default time range
            timeMax = null
        )
    }
    
    /**
     * Fetch user profile information
     * @param sessionToken User's session token for authentication
     * @return Result with UserProfileResponse containing user info
     */
    suspend fun getUserProfile(sessionToken: String): UserProfileResponse {
        println("👤 Fetching user profile...")
        
        val response = client.get("$baseUrl/user/profile") {
            header("Session-Token", sessionToken)
        }
        
        if (response.status == HttpStatusCode.OK) {
            val profileResponse = response.body<UserProfileResponse>()
            println("✅ Successfully fetched user profile: ${profileResponse.firstName} (${profileResponse.email})")
            println("   subscriptionPlan: ${profileResponse.subscriptionPlan}")
            println("   creditsRemaining: ${profileResponse.creditsRemaining}")
            println("   creditsTotal: ${profileResponse.creditsTotal}")
            return profileResponse
        } else if (response.status == HttpStatusCode.Unauthorized) {
            println("🔒 401 Unauthorized in getUserProfile")
            throw UnauthorizedException("Invalid or expired session")
        } else {
            println("❌ Get user profile failed with status ${response.status}")
            throw Exception("Get user profile failed with status ${response.status}")
        }
    }
    
    /**
     * Fetch user credits information including renewal dates
     * @param sessionToken User's session token for authentication
     * @return CreditsInfoResponse containing credits info and renewal dates
     */
    suspend fun getUserCreditsInfo(sessionToken: String): CreditsInfoResponse {
        println("💳 Fetching user credits info...")
        
        val response = client.get("$baseUrl/user/credits") {
            header("Session-Token", sessionToken)
        }
        
        if (response.status == HttpStatusCode.OK) {
            val creditsResponse = response.body<CreditsInfoResponse>()
            println("✅ Successfully fetched credits info: ${creditsResponse.creditsRemaining}/${creditsResponse.creditsTotal}")
            println("   subscriptionPlan: ${creditsResponse.subscriptionPlan}")
            println("   daysUntilRenewal: ${creditsResponse.daysUntilRenewal}")
            println("   nextRenewalDate: ${creditsResponse.nextRenewalDate}")
            return creditsResponse
        } else if (response.status == HttpStatusCode.Unauthorized) {
            println("🔒 401 Unauthorized in getUserCreditsInfo")
            throw UnauthorizedException("Invalid or expired session")
        } else {
            println("❌ Get user credits info failed with status ${response.status}")
            throw Exception("Get user credits info failed with status ${response.status}")
        }
    }
    
    /**
     * Send message to backend agent
     * @param sessionToken User's session token for authentication
     * @param message User's message to send to the agent
     * @param conversationId Optional conversation ID for context
     * @return Result with MessageResponse containing agent's response
     */
    suspend fun sendMessage(
        sessionToken: String,
        message: String,
        conversationId: String? = null
    ): Result<MessageResponse> {
        return try {
            println("💬 Sending message to backend agent: ${message}...")
            
            val messageRequest = MessageRequest(
                message = message,
                conversationId = conversationId
            )
            
            val response = client.post("$baseUrl/messages") {
                header("Session-Token", sessionToken)
                contentType(ContentType.Application.Json)
                setBody(messageRequest)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val messageResponse = response.body<MessageResponse>()
                    println("✅ Successfully received agent response: ${messageResponse.message.take(50)}...")
                    Result.success(messageResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown 401 error" }
                    println("🔒 401 Unauthorized in sendMessage: $errorBody")
                    Result.failure(UnauthorizedException("Invalid or expired session"))
                }
                HttpStatusCode.PaymentRequired -> {
                    val errorBody = try { response.body<String>() } catch (_: Exception) { "Insufficient credits" }
                    println("💳 402 Payment Required in sendMessage: $errorBody")
                    Result.failure(InsufficientCreditsException("Insufficient credits to send message"))
                }
                else -> {
                    val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown error" }
                    println("❌ Send message failed with status ${response.status}: $errorBody")
                    // Return a user-friendly, generic message (mapped in ErrorHandler)
                    Result.failure(Exception("Send message error"))
                }
            }
        } catch (_: SocketTimeoutException) {
            println("⏰ Message socket timeout")
            Result.failure(Exception("Agent is taking longer than expected to process your request"))
        } catch (e: Exception) {
            // Check if it's a request timeout (HttpRequestTimeoutException) by message
            val isTimeout = e.message?.contains("Timeout", ignoreCase = true) == true ||
                    e::class.simpleName?.contains("Timeout", ignoreCase = true) == true
            if (isTimeout) {
                println("⏰ Message request timeout: ${e::class.simpleName}: ${e.message}")
                Result.failure(Exception("Agent is taking longer than expected to process your request"))
            } else {
                println("❌ Send message error [${e::class.simpleName}]: ${e.message}")
                Result.failure(Exception("Send message error: ${e.message}"))
            }
        }
    }

    /**
     * Create a new event in a Google Calendar
     * @param sessionToken User's session token for authentication
     * @param request CreateEventRequest containing event details
     * @return Result with CreateEventResponse containing the created event
     */
    suspend fun createEvent(
        sessionToken: String,
        request: CreateEventRequest
    ): Result<CreateEventResponse> {
        return try {
            println("📅 Creating event: ${request.summary}")
            
            val response = client.post("$baseUrl/events") {
                header("Session-Token", sessionToken)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status == HttpStatusCode.Created) {
                val createEventResponse = response.body<CreateEventResponse>()
                println("✅ Event created successfully: ${createEventResponse.event?.id}")
                Result.success(createEventResponse)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown 401 error" }
                println("🔒 401 Unauthorized in createEvent: $errorBody")
                Result.failure(UnauthorizedException("Invalid or expired session"))
            } else {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown error" }
                println("❌ Create event failed with status ${response.status}: $errorBody")
                Result.failure(Exception("Create event failed with status ${response.status}"))
            }
        } catch (_: SocketTimeoutException) {
            println("⏰ Create event server timeout")
            Result.failure(Exception("Server is not responding while creating event."))
        } catch (e: Exception) {
            println("❌ Create event error: ${e.message}")
            Result.failure(Exception("Create event error: ${e.message}"))
        }
    }
    
    /**
     * Update an existing event in a Google Calendar
     * @param sessionToken User's session token for authentication
     * @param eventId ID of the event to update
     * @param request UpdateEventRequest containing updated event details
     * @return Result with UpdateEventResponse containing the updated event
     */
    suspend fun updateEvent(
        sessionToken: String,
        eventId: String,
        request: UpdateEventRequest
    ): Result<UpdateEventResponse> {
        return try {
            println("📅 Updating event: $eventId")
            
            val response = client.post("$baseUrl/events/$eventId") {
                header("Session-Token", sessionToken)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val updateEventResponse = response.body<UpdateEventResponse>()
                println("✅ Event updated successfully: ${updateEventResponse.event?.id}")
                Result.success(updateEventResponse)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown 401 error" }
                println("🔒 401 Unauthorized in updateEvent: $errorBody")
                Result.failure(UnauthorizedException("Invalid or expired session"))
            } else {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown error" }
                println("❌ Update event failed with status ${response.status}: $errorBody")
                Result.failure(Exception("Update event failed with status ${response.status}"))
            }
        } catch (_: SocketTimeoutException) {
            println("⏰ Update event server timeout")
            Result.failure(Exception("Server is not responding while updating event."))
        } catch (e: Exception) {
            println("❌ Update event error: ${e.message}")
            Result.failure(Exception("Update event error: ${e.message}"))
        }
    }
    
    /**
     * Delete an event from a Google Calendar
     * @param sessionToken User's session token for authentication
     * @param eventId ID of the event to delete
     * @param calendarId Calendar ID containing the event (defaults to "primary")
     * @return Result with DeleteEventResponse
     */
    suspend fun deleteEvent(
        sessionToken: String,
        eventId: String,
        calendarId: String = "primary"
    ): Result<DeleteEventResponse> {
        return try {
            println("📅 Deleting event: $eventId from calendar: $calendarId")
            
            val response = client.post("$baseUrl/events/$eventId/delete") {
                header("Session-Token", sessionToken)
                parameter("calendarId", calendarId)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val deleteEventResponse = response.body<DeleteEventResponse>()
                println("✅ Event deleted successfully: $eventId")
                Result.success(deleteEventResponse)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown 401 error" }
                println("🔒 401 Unauthorized in deleteEvent: $errorBody")
                Result.failure(UnauthorizedException("Invalid or expired session"))
            } else {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown error" }
                println("❌ Delete event failed with status ${response.status}: $errorBody")
                Result.failure(Exception("Delete event failed with status ${response.status}"))
            }
        } catch (_: SocketTimeoutException) {
            println("⏰ Delete event server timeout")
            Result.failure(Exception("Server is not responding while deleting event."))
        } catch (e: Exception) {
            println("❌ Delete event error: ${e.message}")
            Result.failure(Exception("Delete event error: ${e.message}"))
        }
    }
    
    /**
     * Fetch user's contacts from Google Contacts
     * This endpoint is useful for retrieving email addresses to use as attendees when creating or updating calendar events
     * @param sessionToken User's session token for authentication
     * @return Result with ContactsResponse containing list of contacts with email addresses
     */
    suspend fun getContacts(
        sessionToken: String
    ): Result<ContactsResponse> {
        return try {
            println("👥 Fetching user contacts...")
            
            val response = client.get("$baseUrl/contacts") {
                header("Session-Token", sessionToken)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val contactsResponse = response.body<ContactsResponse>()
                println("✅ Successfully fetched ${contactsResponse.totalContacts} contacts")
                Result.success(contactsResponse)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown 401 error" }
                println("🔒 401 Unauthorized in getContacts: $errorBody")
                Result.failure(UnauthorizedException("Invalid or expired session"))
            } else {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "Unknown error" }
                println("❌ Get contacts failed with status ${response.status}: $errorBody")
                Result.failure(Exception("Get contacts failed with status ${response.status}"))
            }
        } catch (_: SocketTimeoutException) {
            println("⏰ Get contacts server timeout")
            Result.failure(Exception("Server is not responding while fetching contacts."))
        } catch (e: Exception) {
            println("❌ Get contacts error: ${e.message}")
            Result.failure(Exception("Get contacts error: ${e.message}"))
        }
    }
}