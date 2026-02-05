package com.smartcal.app.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.*

/**
 * Maps technical error messages to user-friendly localized messages
 */
@Composable
fun getErrorMessage(errorMessage: String?): String {
    if (errorMessage == null) return stringResource(Res.string.error_unknown)

    return when {
        // Authentication errors (specific strings first)
        errorMessage.contains("Authentication succeeded but no token received", ignoreCase = true) ->
            stringResource(Res.string.error_no_token_received)

        errorMessage.contains("OAuth process incomplete", ignoreCase = true) ||
        errorMessage.contains("authentication cancelled", ignoreCase = true) ->
            stringResource(Res.string.error_oauth_incomplete)

        errorMessage.contains("OAuth authentication cancelled", ignoreCase = true) ->
            stringResource(Res.string.error_oauth_cancelled)

        errorMessage.contains("OAuth verification failed", ignoreCase = true) ||
        errorMessage.contains("OAuth failed", ignoreCase = true) ->
            stringResource(Res.string.error_oauth_verification_failed)

        errorMessage.contains("login failed", ignoreCase = true) ||
        errorMessage.contains("authentication failed", ignoreCase = true) ->
            stringResource(Res.string.error_authentication_failed)

        errorMessage.contains("session token not set", ignoreCase = true) ||
        errorMessage.contains("session expired", ignoreCase = true) ||
        errorMessage.contains("unauthorized", ignoreCase = true) ->
            stringResource(Res.string.error_session_expired)

        // Credits
        errorMessage.contains("insufficient credits", ignoreCase = true) ||
        errorMessage.contains("not enough credits", ignoreCase = true) ->
            stringResource(Res.string.error_insufficient_credits)

        // AI Agent specific (must be before generic network to avoid "taking longer" being swallowed)
        errorMessage.contains("taking longer", ignoreCase = true) ->
            stringResource(Res.string.error_agent_timeout)

        // Specific network errors (must be before generic network check)
        errorMessage.contains("connection was lost", ignoreCase = true) ||
        errorMessage.contains("network connection was lost", ignoreCase = true) ->
            stringResource(Res.string.error_connection_lost)

        errorMessage.contains("no internet", ignoreCase = true) ||
        errorMessage.contains("no network", ignoreCase = true) ->
            stringResource(Res.string.error_no_internet)

        // Send message errors (before generic network, since "Send message error: <detail>"
        // could contain network keywords from the underlying exception)
        errorMessage.contains("send message error", ignoreCase = true) ||
        errorMessage.contains("failed to send", ignoreCase = true) ->
            stringResource(Res.string.error_message_send_failed)

        // Server errors
        errorMessage.contains("server error", ignoreCase = true) ||
        errorMessage.contains("500", ignoreCase = true) ||
        errorMessage.contains("502", ignoreCase = true) ||
        errorMessage.contains("503", ignoreCase = true) ||
        errorMessage.contains("504", ignoreCase = true) ->
            stringResource(Res.string.error_server)

        // Calendar access errors
        errorMessage.contains("calendar", ignoreCase = true) &&
        (errorMessage.contains("access", ignoreCase = true) ||
         errorMessage.contains("permission", ignoreCase = true)) ->
            stringResource(Res.string.error_calendar_access)

        // Events loading errors
        errorMessage.contains("events", ignoreCase = true) &&
        (errorMessage.contains("load", ignoreCase = true) ||
         errorMessage.contains("fetch", ignoreCase = true)) ->
            stringResource(Res.string.error_events_load)

        // Generic network errors (catch-all for network issues)
        errorMessage.contains("network", ignoreCase = true) ||
        errorMessage.contains("connection", ignoreCase = true) ||
        errorMessage.contains("timeout", ignoreCase = true) ||
        errorMessage.contains("unreachable", ignoreCase = true) ->
            stringResource(Res.string.error_network)

        // Default to generic error message
        else -> stringResource(Res.string.error_general)
    }
}

/**
 * Gets a user-friendly error message key for repository layer
 * Returns the string resource key that should be used for localization
 */
fun getErrorMessageKey(errorMessage: String?): String {
    if (errorMessage == null) return "error_general"

    return when {
        // Authentication errors (specific strings first)
        errorMessage.contains("Authentication succeeded but no token received", ignoreCase = true) ->
            "error_no_token_received"

        errorMessage.contains("OAuth process incomplete", ignoreCase = true) ||
        errorMessage.contains("authentication cancelled", ignoreCase = true) ->
            "error_oauth_incomplete"

        errorMessage.contains("OAuth authentication cancelled", ignoreCase = true) ->
            "error_oauth_cancelled"

        errorMessage.contains("OAuth verification failed", ignoreCase = true) ||
        errorMessage.contains("OAuth failed", ignoreCase = true) ->
            "error_oauth_verification_failed"

        errorMessage.contains("login failed", ignoreCase = true) ||
        errorMessage.contains("authentication failed", ignoreCase = true) ->
            "error_authentication_failed"

        errorMessage.contains("session token not set", ignoreCase = true) ||
        errorMessage.contains("session expired", ignoreCase = true) ||
        errorMessage.contains("unauthorized", ignoreCase = true) ->
            "error_session_expired"

        // Credits
        errorMessage.contains("insufficient credits", ignoreCase = true) ||
        errorMessage.contains("not enough credits", ignoreCase = true) ->
            "error_insufficient_credits"

        // AI Agent specific (must be before generic network to avoid "taking longer" being swallowed)
        errorMessage.contains("taking longer", ignoreCase = true) ->
            "error_agent_timeout"

        // Specific network errors (must be before generic network check)
        errorMessage.contains("connection was lost", ignoreCase = true) ||
        errorMessage.contains("network connection was lost", ignoreCase = true) ->
            "error_connection_lost"

        errorMessage.contains("no internet", ignoreCase = true) ||
        errorMessage.contains("no network", ignoreCase = true) ->
            "error_no_internet"

        // Send message errors (before generic network, since "Send message error: <detail>"
        // could contain network keywords from the underlying exception)
        errorMessage.contains("send message error", ignoreCase = true) ||
        errorMessage.contains("failed to send", ignoreCase = true) ->
            "error_message_send_failed"

        // Server errors
        errorMessage.contains("server error", ignoreCase = true) ||
        errorMessage.contains("500", ignoreCase = true) ||
        errorMessage.contains("502", ignoreCase = true) ||
        errorMessage.contains("503", ignoreCase = true) ||
        errorMessage.contains("504", ignoreCase = true) ->
            "error_server"

        // Calendar access errors
        errorMessage.contains("calendar", ignoreCase = true) &&
        (errorMessage.contains("access", ignoreCase = true) ||
         errorMessage.contains("permission", ignoreCase = true)) ->
            "error_calendar_access"

        // Events loading errors
        errorMessage.contains("events", ignoreCase = true) &&
        (errorMessage.contains("load", ignoreCase = true) ||
         errorMessage.contains("fetch", ignoreCase = true)) ->
            "error_events_load"

        // Generic network errors (catch-all for network issues)
        errorMessage.contains("network", ignoreCase = true) ||
        errorMessage.contains("connection", ignoreCase = true) ||
        errorMessage.contains("timeout", ignoreCase = true) ||
        errorMessage.contains("unreachable", ignoreCase = true) ->
            "error_network"

        // Default to generic error message
        else -> "error_general"
    }
}

