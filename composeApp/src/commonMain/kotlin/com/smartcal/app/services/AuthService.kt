package com.smartcal.app.services

import com.smartcal.app.models.*
import com.smartcal.app.storage.SessionStorage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// Custom exception for 401 Unauthorized responses
class UnauthorizedException(message: String) : Exception(message)

class AuthService(
    private val sessionStorage: SessionStorage
) {
    private var sessionToken: String? = null
    
    private val baseUrl = "https://calendar-backend-y1rx.onrender.com"
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 30000
        }
    }
    
    suspend fun loginUser(): LoginResult {
        return try {
            val deviceId = sessionStorage.getDeviceId()
            println("🔗 Attempting to connect to: $baseUrl/auth/login")
            println("📱 Using device ID: $deviceId")
            
            val response = client.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(
                    deviceId = deviceId,
                    allowSessionReuse = false  // Use secure by default
                ))
            }
            
            if (response.status == HttpStatusCode.OK) {
                val loginResponse = response.body<LoginResponse>()
                when {
                    loginResponse.success && !loginResponse.authUrl.isNullOrEmpty() -> {
                        // User needs OAuth authentication
                        LoginResult.NeedsBrowserAuth(
                            authUrl = loginResponse.authUrl,
                            sessionId = loginResponse.sessionId ?: ""
                        )
                    }
                    loginResponse.success && !loginResponse.sessionToken.isNullOrEmpty() -> {
                        // User already authenticated with saved tokens
                        setSessionToken(loginResponse.sessionToken)
                        LoginResult.Success(userEmail = loginResponse.userEmail, isNewUser = loginResponse.isNewUser)
                    }
                    loginResponse.success -> {
                        // Success but no token yet (shouldn't happen)
                        LoginResult.Success()
                    }
                    else -> {
                        LoginResult.Error(loginResponse.message)
                    }
                }
            } else {
                LoginResult.Error("Login failed with status ${response.status}")
            }
        } catch (e: SocketTimeoutException) {
            LoginResult.Error("Server is not responding. Please check if the server is running.")
        } catch (e: Exception) {
            LoginResult.Error("Network error: ${e.message}. Please check your internet connection.")
        }
    }
    
    fun setSessionToken(token: String) {
        sessionToken = token
    }
    
    fun getSessionToken(): String? = sessionToken
    
    
    suspend fun logout(sessionToken: String): Result<LogoutResponse> {
        return try {
            val response = client.post("$baseUrl/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(LogoutRequest(sessionToken))
            }
            
            if (response.status == HttpStatusCode.OK) {
                val logoutResponse = response.body<LogoutResponse>()
                Result.success(logoutResponse)
            } else {
                Result.failure(Exception("Logout failed with status ${response.status}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Server is not responding during logout."))
        } catch (e: Exception) {
            Result.failure(Exception("Logout error: ${e.message}"))
        }
    }
    
    suspend fun deleteAccount(sessionToken: String): Result<DeleteAccountResponse> {
        return try {
            val response = client.post("$baseUrl/user/delete-account") {
                contentType(ContentType.Application.Json)
                header("Session-Token", sessionToken)
                setBody(DeleteAccountRequest(confirmPhrase = "DELETE MY ACCOUNT"))
            }
            
            if (response.status == HttpStatusCode.OK) {
                val deleteAccountResponse = response.body<DeleteAccountResponse>()
                Result.success(deleteAccountResponse)
            } else {
                Result.failure(Exception("Delete account failed with status ${response.status}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Server is not responding during account deletion."))
        } catch (e: Exception) {
            Result.failure(Exception("Delete account error: ${e.message}"))
        }
    }
    
    suspend fun sendChatMessage(message: String, sessionToken: String): Result<ChatResponse> {
        return try {
            val response = client.post("$baseUrl/chat") {
                contentType(ContentType.Application.Json)
                header("Session-Token", sessionToken)
                setBody(ChatRequest(message, sessionToken))
            }
            
            if (response.status == HttpStatusCode.OK) {
                val chatResponse = response.body<ChatResponse>()
                Result.success(chatResponse)
            } else {
                Result.failure(Exception("Chat request failed with status ${response.status}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Server is not responding to chat message."))
        } catch (e: Exception) {
            Result.failure(Exception("Chat error: ${e.message}"))
        }
    }
    
    suspend fun validateSession(token: String): Result<Boolean> {
        return try {
            // Use calendars endpoint to validate session - it's lightweight and handles 401 properly
            val response = client.get("$baseUrl/calendars") {
                header("Session-Token", token)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    println("✅ Session validation: Token is valid")
                    Result.success(true)
                }
                HttpStatusCode.Unauthorized -> {
                    println("🔒 Session validation: Token is expired/invalid (401)")
                    Result.success(false)
                }
                else -> {
                    println("❌ Session validation: Unexpected status ${response.status}")
                    Result.success(false)
                }
            }
        } catch (e: SocketTimeoutException) {
            println("⏰ Session validation: Server timeout")
            Result.success(false) // Treat timeout as invalid session to be safe
        } catch (e: Exception) {
            println("❌ Session validation error: ${e.message}")
            Result.success(false) // Invalid session
        }
    }
    
    data class CallbackResult(
        val sessionToken: String,
        val isNewUser: Boolean = false
    )
    
    fun extractDataFromCallback(callbackUrl: String): CallbackResult? {
        return try {
            // Parse OAuth callback URL: koogcalendar://oauth-callback?success=true&sessionToken=token_xxx&sessionId=session_xxx&isNewUser=true
            val sessionTokenRegex = Regex("sessionToken=([^&]+)")
            val isNewUserRegex = Regex("isNewUser=(true|false)")
            
            val sessionTokenMatch = sessionTokenRegex.find(callbackUrl)
            val isNewUserMatch = isNewUserRegex.find(callbackUrl)
            
            val sessionToken = sessionTokenMatch?.groupValues?.get(1)
            val isNewUser = isNewUserMatch?.groupValues?.get(1)?.toBoolean() ?: false
            
            if (sessionToken != null) {
                println("✅ Extracted session token from callback: ${sessionToken.take(20)}...")
                println("✅ Extracted isNewUser from callback: $isNewUser")
                CallbackResult(sessionToken, isNewUser)
            } else {
                println("❌ Could not extract session token from callback URL")
                null
            }
        } catch (e: Exception) {
            println("❌ Error extracting data from callback: ${e.message}")
            null
        }
    }
    
    // Keep old method for backwards compatibility
    fun extractSessionTokenFromCallback(callbackUrl: String): String? {
        return extractDataFromCallback(callbackUrl)?.sessionToken
    }
    
    suspend fun getCalendars(sessionToken: String): Result<CalendarsResponse> {
        return try {
            val response = client.get("$baseUrl/calendars") {
                header("Session-Token", sessionToken)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val calendarsResponse = response.body<CalendarsResponse>()
                Result.success(calendarsResponse)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                // Handle 401 Unauthorized - token expired or invalid
                val errorBody = try { response.body<String>() } catch (e: Exception) { "Unknown 401 error" }
                println("🔒 401 Unauthorized in getCalendars: $errorBody")
                Result.failure(UnauthorizedException("Invalid or expired session"))
            } else {
                Result.failure(Exception("Get calendars failed with status ${response.status}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Server is not responding while fetching calendars."))
        } catch (e: Exception) {
            Result.failure(Exception("Get calendars error: ${e.message}"))
        }
    }
    
    suspend fun getEvents(
        sessionToken: String,
        calendarId: String = "primary",
        timeMin: String? = null,
        timeMax: String? = null
    ): Result<EventsResponse> {
        return try {
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
                Result.success(eventsResponse)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                // Handle 401 Unauthorized - token expired or invalid
                val errorBody = try { response.body<String>() } catch (e: Exception) { "Unknown 401 error" }
                println("🔒 401 Unauthorized in getEvents: $errorBody")
                Result.failure(UnauthorizedException("Invalid or expired session"))
            } else {
                Result.failure(Exception("Get events failed with status ${response.status}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Server is not responding while fetching events."))
        } catch (e: Exception) {
            Result.failure(Exception("Get events error: ${e.message}"))
        }
    }
}