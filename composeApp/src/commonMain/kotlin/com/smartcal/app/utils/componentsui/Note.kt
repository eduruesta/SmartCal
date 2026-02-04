package com.smartcal.app.utils.componentsui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val notes: ImageVector
    get() {
        if (_Speaker_notes != null) return _Speaker_notes!!

        _Speaker_notes = ImageVector.Builder(
            name = "Speaker_notes",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(280f, 560f)
                quadToRelative(17f, 0f, 28.5f, -11.5f)
                reflectiveQuadTo(320f, 520f)
                reflectiveQuadToRelative(-11.5f, -28.5f)
                reflectiveQuadTo(280f, 480f)
                reflectiveQuadToRelative(-28.5f, 11.5f)
                reflectiveQuadTo(240f, 520f)
                reflectiveQuadToRelative(11.5f, 28.5f)
                reflectiveQuadTo(280f, 560f)
                moveToRelative(0f, -120f)
                quadToRelative(17f, 0f, 28.5f, -11.5f)
                reflectiveQuadTo(320f, 400f)
                reflectiveQuadToRelative(-11.5f, -28.5f)
                reflectiveQuadTo(280f, 360f)
                reflectiveQuadToRelative(-28.5f, 11.5f)
                reflectiveQuadTo(240f, 400f)
                reflectiveQuadToRelative(11.5f, 28.5f)
                reflectiveQuadTo(280f, 440f)
                moveToRelative(0f, -120f)
                quadToRelative(17f, 0f, 28.5f, -11.5f)
                reflectiveQuadTo(320f, 280f)
                reflectiveQuadToRelative(-11.5f, -28.5f)
                reflectiveQuadTo(280f, 240f)
                reflectiveQuadToRelative(-28.5f, 11.5f)
                reflectiveQuadTo(240f, 280f)
                reflectiveQuadToRelative(11.5f, 28.5f)
                reflectiveQuadTo(280f, 320f)
                moveToRelative(120f, 240f)
                horizontalLineToRelative(200f)
                verticalLineToRelative(-80f)
                horizontalLineTo(400f)
                close()
                moveToRelative(0f, -120f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(-80f)
                horizontalLineTo(400f)
                close()
                moveToRelative(0f, -120f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(-80f)
                horizontalLineTo(400f)
                close()
                moveTo(80f, 880f)
                verticalLineToRelative(-720f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(160f, 80f)
                horizontalLineToRelative(640f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(880f, 160f)
                verticalLineToRelative(480f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(800f, 720f)
                horizontalLineTo(240f)
                close()
                moveToRelative(126f, -240f)
                horizontalLineToRelative(594f)
                verticalLineToRelative(-480f)
                horizontalLineTo(160f)
                verticalLineToRelative(525f)
                close()
                moveToRelative(-46f, 0f)
                verticalLineToRelative(-480f)
                close()
            }
        }.build()

        return _Speaker_notes!!
    }

private var _Speaker_notes: ImageVector? = null

