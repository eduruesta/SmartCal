package com.smartcal.app.utils.componentsui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Schedule: ImageVector
    get() {
        if (_Schedule != null) return _Schedule!!

        _Schedule = ImageVector.Builder(
            name = "Schedule",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveToRelative(612f, -292f)
                lineToRelative(56f, -56f)
                lineToRelative(-148f, -148f)
                verticalLineToRelative(-184f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(216f)
                close()
                moveTo(480f, 880f)
                quadToRelative(-83f, 0f, -156f, -31.5f)
                reflectiveQuadTo(197f, 763f)
                reflectiveQuadToRelative(-85.5f, -127f)
                reflectiveQuadTo(80f, 480f)
                reflectiveQuadToRelative(31.5f, -156f)
                reflectiveQuadTo(197f, 197f)
                reflectiveQuadToRelative(127f, -85.5f)
                reflectiveQuadTo(480f, 80f)
                reflectiveQuadToRelative(156f, 31.5f)
                reflectiveQuadTo(763f, 197f)
                reflectiveQuadToRelative(85.5f, 127f)
                reflectiveQuadTo(880f, 480f)
                reflectiveQuadToRelative(-31.5f, 156f)
                reflectiveQuadTo(763f, 763f)
                reflectiveQuadToRelative(-127f, 85.5f)
                reflectiveQuadTo(480f, 880f)
                moveToRelative(0f, -80f)
                quadToRelative(133f, 0f, 226.5f, -93.5f)
                reflectiveQuadTo(800f, 480f)
                reflectiveQuadToRelative(-93.5f, -226.5f)
                reflectiveQuadTo(480f, 160f)
                reflectiveQuadToRelative(-226.5f, 93.5f)
                reflectiveQuadTo(160f, 480f)
                reflectiveQuadToRelative(93.5f, 226.5f)
                reflectiveQuadTo(480f, 800f)
            }
        }.build()

        return _Schedule!!
    }

private var _Schedule: ImageVector? = null

