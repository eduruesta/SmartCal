# Google Calendar Events API Documentation

## Overview

This document describes the Google Calendar Events API endpoints that allow you to create, update, and delete events in Google Calendar. All endpoints require authentication via a session token obtained through the OAuth login flow.

## Base URL

```
https://your-api-domain.com
```

## Authentication

All endpoints require a `Session-Token` header obtained after successful OAuth authentication.

```
Session-Token: your-session-token-here
```

If the session token is invalid or expired, you'll receive a `401 Unauthorized` response.

---

## Endpoints

### 1. Get Contacts

Fetch the user's contacts from Google Contacts. This endpoint is useful for retrieving email addresses to use as attendees when creating or updating calendar events.

**Endpoint:** `GET /contacts`

**Headers:**
```
Session-Token: your-session-token-here
```

**Query Parameters:** None

**Response (Success - 200 OK):**

```json
{
  "success": true,
  "contacts": [
    {
      "resourceName": "people/c1234567890",
      "displayName": "John Doe",
      "emailAddresses": [
        {
          "value": "john.doe@example.com",
          "type": "work",
          "displayName": null
        }
      ],
      "names": [
        {
          "displayName": "John Doe",
          "familyName": "Doe",
          "givenName": "John"
        }
      ],
      "photos": [
        {
          "url": "https://lh3.googleusercontent.com/...",
          "default": false
        }
      ]
    },
    {
      "resourceName": "people/c0987654321",
      "displayName": "Jane Smith",
      "emailAddresses": [
        {
          "value": "jane.smith@example.com",
          "type": "home",
          "displayName": null
        },
        {
          "value": "jsmith@company.com",
          "type": "work",
          "displayName": null
        }
      ],
      "names": [
        {
          "displayName": "Jane Smith",
          "familyName": "Smith",
          "givenName": "Jane"
        }
      ],
      "photos": null
    }
  ],
  "totalContacts": 2,
  "userEmail": "user@example.com",
  "timestamp": "2025-10-21T11:56:00.123Z"
}
```

**Response Data Model:**

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Whether the request was successful |
| `contacts` | Array<Contact> | List of contacts with email addresses |
| `totalContacts` | Int | Total number of contacts returned |
| `userEmail` | String? | Email of the authenticated user |
| `timestamp` | String | Timestamp when the response was generated |

**Contact Model:**

| Field | Type | Description |
|-------|------|-------------|
| `resourceName` | String | Unique identifier for the contact in Google People API |
| `displayName` | String? | Primary display name for the contact |
| `emailAddresses` | Array<ContactEmail>? | List of email addresses for this contact |
| `names` | Array<ContactName>? | List of names for this contact |
| `photos` | Array<ContactPhoto>? | List of photos for this contact |

**ContactEmail Model:**

| Field | Type | Description |
|-------|------|-------------|
| `value` | String | The email address |
| `type` | String? | Type of email (e.g., "work", "home", "other") |
| `displayName` | String? | Display name associated with this email |

**ContactName Model:**

| Field | Type | Description |
|-------|------|-------------|
| `displayName` | String? | Full display name |
| `familyName` | String? | Last name / family name |
| `givenName` | String? | First name / given name |

**ContactPhoto Model:**

| Field | Type | Description |
|-------|------|-------------|
| `url` | String? | URL to the contact's photo |
| `default` | Boolean? | Whether this is a default/placeholder photo |

**Response (Error - 500 Internal Server Error):**

```json
{
  "error": "Failed to fetch contacts from Google People API",
  "details": "This could be due to insufficient permissions. The contacts.readonly scope may need to be granted."
}
```

**Important Notes:**

1. **Permissions**: This endpoint requires the `contacts.readonly` OAuth scope. Users must grant this permission during the authentication flow.

2. **Filtered Results**: The API only returns contacts that have at least one email address, as contacts without emails cannot be used as event attendees.

3. **Pagination**: The current implementation fetches up to 1,000 contacts. For users with more contacts, pagination would need to be implemented.

4. **Use Case**: The primary use case is to populate an attendee picker UI when creating or updating calendar events. Extract the email addresses from the `emailAddresses` field to use in event creation.

**Example Usage:**

```typescript
// Fetch contacts and extract emails for attendee selection
async function getContactEmails(sessionToken: string): Promise<string[]> {
  const response = await fetch('https://your-api-domain.com/contacts', {
    headers: {
      'Session-Token': sessionToken
    }
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  
  // Extract all email addresses from all contacts
  const emails: string[] = [];
  data.contacts.forEach(contact => {
    contact.emailAddresses?.forEach(email => {
      emails.push(email.value);
    });
  });
  
  return emails;
}
```

---

### 2. Create Event

Create a new event in a Google Calendar.

**Endpoint:** `POST /events`

**Headers:**
```
Session-Token: your-session-token-here
Content-Type: application/json
```

**Request Body:**

```json
{
  "calendarId": "primary",
  "summary": "Team Meeting",
  "description": "Weekly sync with the development team",
  "start": "2025-10-21T14:00:00Z",
  "end": "2025-10-21T15:00:00Z",
  "attendees": [
    {
      "email": "john.doe@example.com"
    },
    {
      "email": "jane.smith@example.com"
    }
  ],
  "location": "Conference Room A",
  "addMeet": true
}
```

**Request Data Model:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `calendarId` | String | No | `"primary"` | ID of the calendar to create the event in. Use `"primary"` for the user's primary calendar |
| `summary` | String | Yes | - | Title/name of the event |
| `description` | String | No | `null` | Detailed description of the event |
| `start` | String | Yes | - | Start date/time in ISO 8601 format (e.g., `"2025-10-21T14:00:00Z"`) |
| `end` | String | Yes | - | End date/time in ISO 8601 format (e.g., `"2025-10-21T15:00:00Z"`) |
| `attendees` | Array<EventAttendee> | No | `null` | List of event attendees |
| `location` | String | No | `null` | Physical or virtual location of the event |
| `addMeet` | Boolean | No | `true` | Whether to automatically add a Google Meet link to the event |

**EventAttendee Model:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | String | Yes | Email address of the attendee |

**Response (Success - 201 Created):**

```json
{
  "success": true,
  "event": {
    "id": "abc123def456",
    "summary": "Team Meeting",
    "description": "Weekly sync with the development team",
    "location": "Conference Room A",
    "start": {
      "dateTime": "2025-10-21T14:00:00Z",
      "timeZone": "UTC"
    },
    "end": {
      "dateTime": "2025-10-21T15:00:00Z",
      "timeZone": "UTC"
    },
    "attendees": [
      {
        "email": "john.doe@example.com",
        "displayName": "John Doe",
        "responseStatus": "needsAction"
      },
      {
        "email": "jane.smith@example.com",
        "displayName": "Jane Smith",
        "responseStatus": "needsAction"
      }
    ],
    "htmlLink": "https://calendar.google.com/calendar/event?eid=...",
    "hangoutLink": "https://meet.google.com/abc-defg-hij",
    "conferenceData": {
      "conferenceId": "abc-defg-hij",
      "conferenceSolution": {
        "name": "Google Meet"
      },
      "entryPoints": [
        {
          "entryPointType": "video",
          "uri": "https://meet.google.com/abc-defg-hij"
        }
      ]
    },
    "creator": {
      "email": "user@example.com",
      "displayName": "User Name",
      "self": true
    },
    "organizer": {
      "email": "user@example.com",
      "displayName": "User Name",
      "self": true
    },
    "created": "2025-10-21T10:00:00Z",
    "updated": "2025-10-21T10:00:00Z"
  },
  "message": "Event created successfully",
  "timestamp": "2025-10-21T10:00:00.123Z"
}
```

**Response (Error - 500 Internal Server Error):**

```json
{
  "success": false,
  "event": null,
  "message": "Failed to create event in Google Calendar",
  "timestamp": "2025-10-21T10:00:00.123Z"
}
```

---

### 3. Update Event

Update an existing event in a Google Calendar.

**Endpoint:** `POST /events/{eventId}`

**Path Parameters:**
- `eventId`: The ID of the event to update

**Headers:**
```
Session-Token: your-session-token-here
Content-Type: application/json
```

**Request Body:**

```json
{
  "calendarId": "primary",
  "summary": "Team Meeting - Updated",
  "description": "Weekly sync with the development team - Updated agenda",
  "start": "2025-10-21T15:00:00Z",
  "end": "2025-10-21T16:00:00Z",
  "attendees": [
    {
      "email": "john.doe@example.com"
    },
    {
      "email": "jane.smith@example.com"
    },
    {
      "email": "bob.wilson@example.com"
    }
  ],
  "location": "Conference Room B"
}
```

**Request Data Model:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `calendarId` | String | No | `"primary"` | ID of the calendar containing the event |
| `summary` | String | No | `null` | Updated title/name of the event |
| `description` | String | No | `null` | Updated description of the event |
| `start` | String | No | `null` | Updated start date/time in ISO 8601 format |
| `end` | String | No | `null` | Updated end date/time in ISO 8601 format |
| `attendees` | Array<EventAttendee> | No | `null` | Updated list of event attendees (replaces existing) |
| `location` | String | No | `null` | Updated location of the event |

**Note:** All fields are optional. Only provide the fields you want to update. Fields not included in the request will remain unchanged.

**Response (Success - 200 OK):**

```json
{
  "success": true,
  "event": {
    "id": "abc123def456",
    "summary": "Team Meeting - Updated",
    "description": "Weekly sync with the development team - Updated agenda",
    "location": "Conference Room B",
    "start": {
      "dateTime": "2025-10-21T15:00:00Z",
      "timeZone": "UTC"
    },
    "end": {
      "dateTime": "2025-10-21T16:00:00Z",
      "timeZone": "UTC"
    },
    "attendees": [
      {
        "email": "john.doe@example.com",
        "displayName": "John Doe",
        "responseStatus": "needsAction"
      },
      {
        "email": "jane.smith@example.com",
        "displayName": "Jane Smith",
        "responseStatus": "accepted"
      },
      {
        "email": "bob.wilson@example.com",
        "displayName": "Bob Wilson",
        "responseStatus": "needsAction"
      }
    ],
    "htmlLink": "https://calendar.google.com/calendar/event?eid=...",
    "hangoutLink": "https://meet.google.com/abc-defg-hij",
    "creator": {
      "email": "user@example.com",
      "displayName": "User Name",
      "self": true
    },
    "organizer": {
      "email": "user@example.com",
      "displayName": "User Name",
      "self": true
    },
    "created": "2025-10-21T10:00:00Z",
    "updated": "2025-10-21T10:30:00Z"
  },
  "message": "Event updated successfully",
  "timestamp": "2025-10-21T10:30:00.456Z"
}
```

**Response (Error - 500 Internal Server Error):**

```json
{
  "success": false,
  "event": null,
  "message": "Failed to update event in Google Calendar",
  "timestamp": "2025-10-21T10:30:00.456Z"
}
```

---

### 4. Delete Event

Delete an event from a Google Calendar.

**Endpoint:** `POST /events/{eventId}/delete`

**Path Parameters:**
- `eventId`: The ID of the event to delete

**Query Parameters:**
- `calendarId` (optional): The ID of the calendar containing the event. Defaults to `"primary"`.

**Headers:**
```
Session-Token: your-session-token-here
```

**Example Request:**

```
POST /events/abc123def456/delete?calendarId=primary
```

**Response (Success - 200 OK):**

```json
{
  "success": true,
  "message": "Event deleted successfully",
  "timestamp": "2025-10-21T10:45:00.789Z"
}
```

**Response (Error - 500 Internal Server Error):**

```json
{
  "success": false,
  "message": "Failed to delete event from Google Calendar",
  "timestamp": "2025-10-21T10:45:00.789Z"
}
```

---

## Common Response Models

### GoogleEvent Model

The `GoogleEvent` object returned in create and update responses contains the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique identifier for the event |
| `summary` | String | Title/name of the event |
| `description` | String? | Detailed description of the event |
| `location` | String? | Physical or virtual location |
| `start` | GoogleEventDateTime | Start date/time information |
| `end` | GoogleEventDateTime | End date/time information |
| `attendees` | Array<GoogleEventPerson>? | List of event attendees with their response status |
| `htmlLink` | String | Link to the event in Google Calendar web interface |
| `hangoutLink` | String? | Google Meet video conference link (if enabled) |
| `conferenceData` | ConferenceData? | Detailed conference/meeting information |
| `creator` | GoogleEventPerson | Person who created the event |
| `organizer` | GoogleEventPerson | Event organizer |
| `created` | String | Timestamp when the event was created |
| `updated` | String | Timestamp when the event was last updated |
| `status` | String? | Event status (e.g., "confirmed", "tentative", "cancelled") |
| `recurringEventId` | String? | ID of the recurring event (if this is a recurring event instance) |

### GoogleEventDateTime Model

| Field | Type | Description |
|-------|------|-------------|
| `date` | String? | Date in YYYY-MM-DD format (for all-day events) |
| `dateTime` | String? | Date and time in ISO 8601 format |
| `timeZone` | String? | Time zone (e.g., "UTC", "America/New_York") |

### GoogleEventPerson Model

| Field | Type | Description |
|-------|------|-------------|
| `email` | String | Email address of the person |
| `displayName` | String? | Display name of the person |
| `self` | Boolean? | Whether this person is the authenticated user |
| `responseStatus` | String? | Response status for attendees ("needsAction", "accepted", "declined", "tentative") |

### ConferenceData Model

| Field | Type | Description |
|-------|------|-------------|
| `conferenceId` | String? | Unique identifier for the conference |
| `conferenceSolution` | Object? | Information about the conference solution (e.g., Google Meet) |
| `entryPoints` | Array? | List of entry points (video, phone, etc.) to join the conference |

---

## Error Responses

All endpoints can return the following error responses:

### 400 Bad Request
Missing required header or parameters.

```json
{
  "error": "Session-Token header required"
}
```

### 401 Unauthorized
Invalid or expired session token, or missing access token.

```json
{
  "error": "Invalid or expired session"
}
```

```json
{
  "error": "No valid access token found"
}
```

### 500 Internal Server Error
Server error or Google Calendar API error.

```json
{
  "success": false,
  "message": "Error message details",
  "timestamp": "2025-10-21T10:00:00.123Z"
}
```

---

## Usage Examples

### JavaScript/TypeScript (Fetch API)

#### Create Event

```typescript
async function createCalendarEvent(sessionToken: string) {
  const response = await fetch('https://your-api-domain.com/events', {
    method: 'POST',
    headers: {
      'Session-Token': sessionToken,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      calendarId: 'primary',
      summary: 'Team Meeting',
      description: 'Weekly sync with the development team',
      start: '2025-10-21T14:00:00Z',
      end: '2025-10-21T15:00:00Z',
      attendees: [
        { email: 'john.doe@example.com' },
        { email: 'jane.smith@example.com' }
      ],
      location: 'Conference Room A',
      addMeet: true
    })
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  console.log('Event created:', data.event);
  return data;
}
```

#### Update Event

```typescript
async function updateCalendarEvent(sessionToken: string, eventId: string) {
  const response = await fetch(`https://your-api-domain.com/events/${eventId}`, {
    method: 'POST',
    headers: {
      'Session-Token': sessionToken,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      calendarId: 'primary',
      summary: 'Team Meeting - Updated',
      start: '2025-10-21T15:00:00Z',
      end: '2025-10-21T16:00:00Z'
    })
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  console.log('Event updated:', data.event);
  return data;
}
```

#### Delete Event

```typescript
async function deleteCalendarEvent(sessionToken: string, eventId: string, calendarId: string = 'primary') {
  const response = await fetch(
    `https://your-api-domain.com/events/${eventId}/delete?calendarId=${calendarId}`,
    {
      method: 'POST',
      headers: {
        'Session-Token': sessionToken
      }
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  console.log('Event deleted:', data.message);
  return data;
}
```

### Swift (iOS)

#### Create Event

```swift
func createCalendarEvent(sessionToken: String) async throws {
    guard let url = URL(string: "https://your-api-domain.com/events") else {
        throw URLError(.badURL)
    }

    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue(sessionToken, forHTTPHeaderField: "Session-Token")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

    let eventData: [String: Any] = [
        "calendarId": "primary",
        "summary": "Team Meeting",
        "description": "Weekly sync with the development team",
        "start": "2025-10-21T14:00:00Z",
        "end": "2025-10-21T15:00:00Z",
        "attendees": [
            ["email": "john.doe@example.com"],
            ["email": "jane.smith@example.com"]
        ],
        "location": "Conference Room A",
        "addMeet": true
    ]

    request.httpBody = try JSONSerialization.data(withJSONObject: eventData)

    let (data, response) = try await URLSession.shared.data(for: request)

    guard let httpResponse = response as? HTTPURLResponse,
          httpResponse.statusCode == 201 else {
        throw URLError(.badServerResponse)
    }

    let result = try JSONDecoder().decode(CreateEventResponse.self, from: data)
    print("Event created:", result.event?.summary ?? "")
}
```

#### Update Event

```swift
func updateCalendarEvent(sessionToken: String, eventId: String) async throws {
    guard let url = URL(string: "https://your-api-domain.com/events/\(eventId)") else {
        throw URLError(.badURL)
    }

    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue(sessionToken, forHTTPHeaderField: "Session-Token")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

    let updateData: [String: Any] = [
        "calendarId": "primary",
        "summary": "Team Meeting - Updated",
        "start": "2025-10-21T15:00:00Z",
        "end": "2025-10-21T16:00:00Z"
    ]

    request.httpBody = try JSONSerialization.data(withJSONObject: updateData)

    let (data, _) = try await URLSession.shared.data(for: request)
    let result = try JSONDecoder().decode(UpdateEventResponse.self, from: data)
    print("Event updated:", result.event?.summary ?? "")
}
```

#### Delete Event

```swift
func deleteCalendarEvent(sessionToken: String, eventId: String, calendarId: String = "primary") async throws {
    guard let url = URL(string: "https://your-api-domain.com/events/\(eventId)/delete?calendarId=\(calendarId)") else {
        throw URLError(.badURL)
    }

    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue(sessionToken, forHTTPHeaderField: "Session-Token")

    let (data, _) = try await URLSession.shared.data(for: request)
    let result = try JSONDecoder().decode(DeleteEventResponse.self, from: data)
    print("Event deleted:", result.message)
}
```

### Kotlin (Android)

#### Create Event

```kotlin
suspend fun createCalendarEvent(sessionToken: String): CreateEventResponse {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val response = client.post("https://your-api-domain.com/events") {
        header("Session-Token", sessionToken)
        contentType(ContentType.Application.Json)
        setBody(CreateEventRequest(
            calendarId = "primary",
            summary = "Team Meeting",
            description = "Weekly sync with the development team",
            start = "2025-10-21T14:00:00Z",
            end = "2025-10-21T15:00:00Z",
            attendees = listOf(
                EventAttendee("john.doe@example.com"),
                EventAttendee("jane.smith@example.com")
            ),
            location = "Conference Room A",
            addMeet = true
        ))
    }

    return response.body()
}
```

#### Update Event

```kotlin
suspend fun updateCalendarEvent(sessionToken: String, eventId: String): UpdateEventResponse {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val response = client.post("https://your-api-domain.com/events/$eventId") {
        header("Session-Token", sessionToken)
        contentType(ContentType.Application.Json)
        setBody(UpdateEventRequest(
            calendarId = "primary",
            summary = "Team Meeting - Updated",
            start = "2025-10-21T15:00:00Z",
            end = "2025-10-21T16:00:00Z"
        ))
    }

    return response.body()
}
```

#### Delete Event

```kotlin
suspend fun deleteCalendarEvent(
    sessionToken: String,
    eventId: String,
    calendarId: String = "primary"
): DeleteEventResponse {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val response = client.post(
        "https://your-api-domain.com/events/$eventId/delete?calendarId=$calendarId"
    ) {
        header("Session-Token", sessionToken)
    }

    return response.body()
}
```

---

## Best Practices

1. **Date/Time Format**: Always use ISO 8601 format for dates and times (e.g., `2025-10-21T14:00:00Z`). The timezone is set to UTC by default.

2. **Calendar ID**: Use `"primary"` to access the user's primary calendar. For other calendars, use the calendar's email address (e.g., `"team@example.com"`).

3. **Error Handling**: Always check the `success` field in responses and handle errors appropriately. The API may return detailed error messages in the `message` field.

4. **Session Management**: Store the session token securely and refresh it when it expires. Implement proper error handling for 401 responses.

5. **Google Meet Integration**: Set `addMeet: false` if you don't want to automatically create a Google Meet link. The Meet link will be available in the `hangoutLink` field of the response.

6. **Partial Updates**: When updating events, only include fields you want to change. Omitted fields will retain their current values.

7. **Attendees**: When updating attendees, provide the complete list of attendees as the update replaces the existing attendee list entirely.

8. **Event IDs**: Save the event ID from the create response to use in subsequent update or delete operations.

---

## Rate Limits

The API uses Google Calendar API under the hood, which has rate limits. If you receive rate limit errors (429 status), implement exponential backoff and retry logic.

---

## Support

For issues or questions about these endpoints, please contact your API administrator or refer to the main project documentation.
