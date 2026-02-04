package com.smartcal.app.utils.componentsui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Microphone: ImageVector
    get() {
        if (_Microphone != null) return _Microphone!!

        _Microphone = ImageVector.Builder(
            name = "Microphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 18.75f)
                curveTo(15.3137f, 18.75f, 18f, 16.0637f, 18f, 12.75f)
                verticalLineTo(11.25f)
                moveTo(12f, 18.75f)
                curveTo(8.68629f, 18.75f, 6f, 16.0637f, 6f, 12.75f)
                verticalLineTo(11.25f)
                moveTo(12f, 18.75f)
                verticalLineTo(22.5f)
                moveTo(8.25f, 22.5f)
                horizontalLineTo(15.75f)
                moveTo(12f, 15.75f)
                curveTo(10.3431f, 15.75f, 9f, 14.4069f, 9f, 12.75f)
                verticalLineTo(4.5f)
                curveTo(9f, 2.84315f, 10.3431f, 1.5f, 12f, 1.5f)
                curveTo(13.6569f, 1.5f, 15f, 2.84315f, 15f, 4.5f)
                verticalLineTo(12.75f)
                curveTo(15f, 14.4069f, 13.6569f, 15.75f, 12f, 15.75f)
                close()
            }
        }.build()

        return _Microphone!!
    }

private var _Microphone: ImageVector? = null

