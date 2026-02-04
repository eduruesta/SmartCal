package com.smartcal.app.models

import kotlinx.serialization.Serializable

// Request models for event operations

@Serializable
data class CreateEventRequest(
    val calendarId: String = "primary",
    val summary: String,
    val description: String? = null,
    val start: String,
    val end: String,
    val attendees: List<CreateEventAttendee>? = null,
    val location: String? = null,
    val addMeet: Boolean = true,
    val reminders: EventReminders? = null
)

@Serializable
data class CreateEventAttendee(
    val email: String
)


@Serializable
data class UpdateEventRequest(
    val calendarId: String = "primary",
    val summary: String? = null,
    val description: String? = null,
    val start: String? = null,
    val end: String? = null,
    val attendees: List<CreateEventAttendee>? = null,
    val location: String? = null,
    val reminders: EventReminders? = null
)

// Response models for event operations

@Serializable
data class CreateEventResponse(
    val success: Boolean,
    val event: GoogleEvent? = null,
    val message: String,
    val timestamp: String
)

@Serializable
data class UpdateEventResponse(
    val success: Boolean,
    val event: GoogleEvent? = null,
    val message: String,
    val timestamp: String
)

@Serializable
data class DeleteEventResponse(
    val success: Boolean,
    val message: String,
    val timestamp: String
)

// Google Event model (complete event data returned by API)

@Serializable
data class GoogleEvent(
    val id: String,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val start: GoogleEventDateTime? = null,
    val end: GoogleEventDateTime? = null,
    val attendees: List<GoogleEventPerson>? = null,
    val htmlLink: String? = null,
    val hangoutLink: String? = null,
    val conferenceData: GoogleConferenceData? = null,
    val creator: GoogleEventPerson? = null,
    val organizer: GoogleEventPerson? = null,
    val created: String? = null,
    val updated: String? = null,
    val status: String? = null,
    val recurringEventId: String? = null,
    val reminders: EventReminders? = null
)

@Serializable
data class GoogleEventDateTime(
    val date: String? = null,
    val dateTime: String? = null,
    val timeZone: String? = null
)

@Serializable
data class GoogleEventPerson(
    val email: String,
    val displayName: String? = null,
    val self: Boolean? = null,
    val responseStatus: String? = null
)

@Serializable
data class GoogleConferenceData(
    val conferenceId: String? = null,
    val conferenceSolution: GoogleConferenceSolution? = null,
    val entryPoints: List<GoogleConferenceEntryPoint>? = null
)

@Serializable
data class GoogleConferenceSolution(
    val name: String? = null
)

@Serializable
data class GoogleConferenceEntryPoint(
    val entryPointType: String? = null,
    val uri: String? = null
)

// Contacts API models

@Serializable
data class ContactsResponse(
    val success: Boolean,
    val contacts: List<Contact>,
    val totalContacts: Int,
    val userEmail: String? = null,
    val timestamp: String
)

@Serializable
data class Contact(
    val resourceName: String,
    val displayName: String? = null,
    val emailAddresses: List<ContactEmail>? = null,
    val names: List<ContactName>? = null,
    val photos: List<ContactPhoto>? = null
)

@Serializable
data class ContactEmail(
    val value: String,
    val type: String? = null,
    val displayName: String? = null
)

@Serializable
data class ContactName(
    val displayName: String? = null,
    val familyName: String? = null,
    val givenName: String? = null
)

@Serializable
data class ContactPhoto(
    val url: String? = null,
    val default: Boolean? = null
)
