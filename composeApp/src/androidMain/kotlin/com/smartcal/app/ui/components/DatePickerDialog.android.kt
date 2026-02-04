package com.smartcal.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.DatePickerDialog as M3DatePickerDialog

@ExperimentalMaterial3Api
@Composable
actual fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: (@Composable (() -> Unit))?,
    shape: Shape,
    tonalElevation: Dp,
    colors: DatePickerColors,
    properties: DialogProperties,
    content: @Composable ColumnScope.() -> Unit
) {
    M3DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        shape = shape,
        tonalElevation = tonalElevation,
        colors = colors,
        properties = properties,
        content = content
    )
}