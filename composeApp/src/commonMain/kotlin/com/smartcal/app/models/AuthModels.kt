package com.smartcal.app.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String = "",
    val deviceId: String? = null,
    val allowSessionReuse: Boolean = false
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val sessionToken: String? = null,
    val authUrl: String? = null,
    val sessionId: String? = null,
    val message: String,
    val userEmail: String? = null,
    val isNewUser: Boolean? = null
)


@Serializable
data class LogoutRequest(
    val sessionToken: String
)

@Serializable
data class LogoutResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class DeleteAccountRequest(
    val confirmPhrase: String? = null
)

@Serializable
data class DeleteAccountResponse(
    val success: Boolean,
    val message: String,
    val accountDeleted: Boolean? = null,
    val conversationsDeleted: Int? = null,
    val timestamp: String? = null
)

@Serializable
data class ChatRequest(
    val message: String,
    val sessionToken: String
)

@Serializable
data class ChatResponse(
    val response: String,
    val userEmail: String? = null,
    val timestamp: String? = null
)

// New models for /messages API
@Serializable
data class MessageRequest(
    val message: String,
    val conversationId: String? = null
)

@Serializable
data class UserBasicInfo(
    val email: String? = null,
    val firstName: String? = null,
    val fullName: String? = null
)

@Serializable
data class MessageResponse(
    val message: String,
    val conversationId: String? = null,
    val userInfo: UserBasicInfo? = null,
    val success: Boolean = true,
    val creditsRemaining: Int? = null,
    val subscriptionPlan: String? = null
)

@Serializable
data class Calendar(
    val kind: String? = null,
    val etag: String? = null,
    val id: String,
    val summary: String,
    val description: String? = null,
    val timeZone: String? = null,
    val accessRole: String? = null,
    val selected: Boolean? = null,
    val primary: Boolean? = null,
    val backgroundColor: String? = null,
    val foregroundColor: String? = null
)

@Serializable
data class CalendarsResponse(
    val success: Boolean,
    val calendars: List<Calendar>? = null,
    val userEmail: String? = null,
    val timestamp: String? = null,
    val message: String? = null
)

@Serializable
data class EventOrganizer(
    val email: String? = null,
    val displayName: String? = null,
    val self: Boolean? = null
)

@Serializable
data class EventAttendee(
    val email: String? = null,
    val displayName: String? = null,
    val self: Boolean? = null
)

@Serializable
data class ConferenceData(
    val entryPoints: List<ConferenceEntryPoint>? = null,
    val createRequest: ConferenceCreateRequest? = null
)

@Serializable
data class ConferenceEntryPoint(
    val entryPointType: String? = null, // "video", "phone", etc.
    val uri: String? = null,
    val label: String? = null,
    val pin: String? = null
)

@Serializable
data class ConferenceCreateRequest(
    val requestId: String? = null,
    val conferenceSolutionKey: ConferenceSolutionKey? = null
)

@Serializable
data class ConferenceSolutionKey(
    val type: String? = null // "hangoutsMeet", "addOn", etc.
)

@Serializable
data class EventReminders(
    val useDefault: Boolean? = null,
    val overrides: List<EventReminder>? = null
)

@Serializable
data class EventReminder(
    val method: String, // "email", "popup"
    val minutes: Int // minutes before event
)

@Serializable
data class Event(
    val kind: String? = null,
    val etag: String? = null,
    val id: String,
    val status: String? = null,
    val htmlLink: String? = null,
    val created: String? = null,
    val updated: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val start: EventDateTime? = null,
    val end: EventDateTime? = null,
    val location: String? = null,
    val organizer: EventOrganizer? = null,
    val attendees: List<EventAttendee>? = null,
    val hangoutLink: String? = null,
    val conferenceData: ConferenceData? = null,
    val reminders: EventReminders? = null
)

@Serializable
data class EventDateTime(
    val dateTime: String? = null,
    val date: String? = null,
    val timeZone: String? = null
)

@Serializable
data class EventsResponse(
    val success: Boolean,
    val events: List<Event>? = null,
    val userEmail: String? = null,
    val calendarId: String? = null,
    val timeRange: TimeRange? = null,
    val timestamp: String? = null,
    val message: String? = null
)

@Serializable
data class TimeRange(
    val timeMin: String? = null,
    val timeMax: String? = null
)

@Serializable
data class UserProfileResponse(
    val success: Boolean,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val googleSub: String? = null,
    val profilePicture: String? = null,
    val subscriptionPlan: String? = null,
    val creditsRemaining: Int? = null,
    val creditsTotal: Int? = null,
    val lastLoginAt: String? = null,
    val createdAt: String? = null,
    val message: String? = null
)

data class AuthState(
    val isAuthenticated: Boolean = false,
    val sessionToken: String? = null,
    val userEmail: String? = null,
    val firstName: String? = null,
    val fullName: String? = null,
    val profilePicture: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val needsBrowserAuth: Boolean = false,
    val authUrl: String? = null,
    val sessionId: String? = null,
    val creditsRemaining: Int? = null,
    val subscriptionPlan: String? = null,
    val isNewUser: Boolean? = null,
    val isRevenueCatReady: Boolean = false,
    val googleSub: String? = null,
)

@Serializable
data class CreditsInfoResponse(
    val success: Boolean,
    val creditsRemaining: Int? = null,
    val creditsTotal: Int? = null,
    val subscriptionPlan: String? = null,
    val lastCreditRenewal: String? = null,
    val nextRenewalDate: String? = null,
    val daysUntilRenewal: Int? = null,
    val hasExpiredCredits: Boolean? = null,
    val message: String? = null
)