package com.smartcal.app.config

/**
 * Feature flags for enabling experimental or in-development functionality.
 *
 * NOTE: Set [runAgentLocally] to true to bypass the backend `/messages` endpoint
 * and use an in-app agent path (currently a minimal stub) so you can try it
 * directly in the app.
 */
object AppFeatures {
    // Toggle to enable the in-app agent instead of calling the server `/messages`.
    // Default true to allow trying the in-app flow easily. Switch to false to
    // revert to server behavior.
    var runAgentLocally: Boolean = true
}
