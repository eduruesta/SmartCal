package com.smartcal.app.utils.componentsui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Eye: ImageVector
    get() {
        if (_Eye != null) return _Eye!!

        _Eye = ImageVector.Builder(
            name = "Eye",
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
                moveTo(2.03555f, 12.3224f)
                curveTo(1.96647f, 12.1151f, 1.9664f, 11.8907f, 2.03536f, 11.6834f)
                curveTo(3.42372f, 7.50972f, 7.36079f, 4.5f, 12.0008f, 4.5f)
                curveTo(16.6387f, 4.5f, 20.5742f, 7.50692f, 21.9643f, 11.6776f)
                curveTo(22.0334f, 11.8849f, 22.0335f, 12.1093f, 21.9645f, 12.3166f)
                curveTo(20.5761f, 16.4903f, 16.6391f, 19.5f, 11.9991f, 19.5f)
                curveTo(7.36119f, 19.5f, 3.42564f, 16.4931f, 2.03555f, 12.3224f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 12f)
                curveTo(15f, 13.6569f, 13.6569f, 15f, 12f, 15f)
                curveTo(10.3431f, 15f, 9f, 13.6569f, 9f, 12f)
                curveTo(9f, 10.3431f, 10.3431f, 9f, 12f, 9f)
                curveTo(13.6569f, 9f, 15f, 10.3431f, 15f, 12f)
                close()
            }
        }.build()

        return _Eye!!
    }

private var _Eye: ImageVector? = null

