package com.smartcal.app.ui.components

import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.app_name
import smartcalai.composeapp.generated.resources.calendars
import smartcalai.composeapp.generated.resources.filter_day
import smartcalai.composeapp.generated.resources.filter_month
import smartcalai.composeapp.generated.resources.filter_week
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartcal.app.models.Calendar
import com.smartcal.app.ui.EventFilterType
import com.smartcal.app.utils.componentsui.Calendar_day
import com.smartcal.app.utils.componentsui.Calendar_month
import com.smartcal.app.utils.componentsui.Calendar_week
import org.jetbrains.compose.resources.stringResource

@Composable
fun CalendarDrawer(
    filterType: EventFilterType,
    calendars: List<Calendar>,
    enabledCalendars: Set<String>,
    subscriptionPlan: String?,
    onFilterTypeChange: (EventFilterType) -> Unit,
    onCalendarToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.85f)
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        HorizontalDivider(color = Color(0xFF38444D))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // View section
            Text(
                text = "View",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
            )

            EventFilterType.entries.forEach { type ->
                val isSelected = filterType == type
                val filterText = when (type) {
                    EventFilterType.DAY -> stringResource(Res.string.filter_day)
                    EventFilterType.WEEK -> stringResource(Res.string.filter_week)
                    EventFilterType.MONTH -> stringResource(Res.string.filter_month)
                }
                val filterIcon = when (type) {
                    EventFilterType.DAY -> Calendar_day
                    EventFilterType.WEEK -> Calendar_week
                    EventFilterType.MONTH -> Calendar_month
                }

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = filterIcon,
                            contentDescription = filterText,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text(filterText) },
                    selected = isSelected,
                    onClick = { onFilterTypeChange(type) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            // Calendars section
            Text(
                text = stringResource(Res.string.calendars),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
            )

            // Filter calendars based on subscription plan
            val availableCalendars = if (subscriptionPlan?.contains("Pro") == true) {
                calendars
            } else {
                calendars.filter { it.primary == true }
            }

            availableCalendars.forEach { calendar ->
                val isEnabled = enabledCalendars.contains(calendar.id)

                NavigationDrawerItem(
                    icon = {
                        Checkbox(
                            checked = isEnabled,
                            onCheckedChange = null
                        )
                    },
                    label = {
                        Text(
                            text = calendar.summary,
                            maxLines = 2
                        )
                    },
                    selected = false,
                    onClick = { onCalendarToggle(calendar.id, !isEnabled) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    }
}