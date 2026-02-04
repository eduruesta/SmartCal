package com.smartcal.app.utils.componentsui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val HamburgerMenu: ImageVector
    get() {
        if (_HamburgerMenu != null) return _HamburgerMenu!!

        _HamburgerMenu = ImageVector.Builder(
            name = "HamburgerMenu",
            defaultWidth = 15.dp,
            defaultHeight = 15.dp,
            viewportWidth = 15f,
            viewportHeight = 15f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(1.5f, 3f)
                curveTo(1.22386f, 3f, 1f, 3.22386f, 1f, 3.5f)
                curveTo(1f, 3.77614f, 1.22386f, 4f, 1.5f, 4f)
                horizontalLineTo(13.5f)
                curveTo(13.7761f, 4f, 14f, 3.77614f, 14f, 3.5f)
                curveTo(14f, 3.22386f, 13.7761f, 3f, 13.5f, 3f)
                horizontalLineTo(1.5f)
                close()
                moveTo(1f, 7.5f)
                curveTo(1f, 7.22386f, 1.22386f, 7f, 1.5f, 7f)
                horizontalLineTo(13.5f)
                curveTo(13.7761f, 7f, 14f, 7.22386f, 14f, 7.5f)
                curveTo(14f, 7.77614f, 13.7761f, 8f, 13.5f, 8f)
                horizontalLineTo(1.5f)
                curveTo(1.22386f, 8f, 1f, 7.77614f, 1f, 7.5f)
                close()
                moveTo(1f, 11.5f)
                curveTo(1f, 11.2239f, 1.22386f, 11f, 1.5f, 11f)
                horizontalLineTo(13.5f)
                curveTo(13.7761f, 11f, 14f, 11.2239f, 14f, 11.5f)
                curveTo(14f, 11.7761f, 13.7761f, 12f, 13.5f, 12f)
                horizontalLineTo(1.5f)
                curveTo(1.22386f, 12f, 1f, 11.7761f, 1f, 11.5f)
                close()
            }
        }.build()

        return _HamburgerMenu!!
    }

private var _HamburgerMenu: ImageVector? = null

