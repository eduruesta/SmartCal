package com.smartcal.app.ui

import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.onboarding_app_icon_description
import smartcalai.composeapp.generated.resources.onboarding_button_next
import smartcalai.composeapp.generated.resources.onboarding_button_see_all_plans
import smartcalai.composeapp.generated.resources.onboarding_button_start_free
import smartcalai.composeapp.generated.resources.onboarding_credit_info
import smartcalai.composeapp.generated.resources.onboarding_credits_label
import smartcalai.composeapp.generated.resources.onboarding_credits_number
import smartcalai.composeapp.generated.resources.onboarding_feature_check_calendars
import smartcalai.composeapp.generated.resources.onboarding_feature_credits
import smartcalai.composeapp.generated.resources.onboarding_feature_multiple_calendars
import smartcalai.composeapp.generated.resources.onboarding_feature_natural_language
import smartcalai.composeapp.generated.resources.onboarding_feature_organize
import smartcalai.composeapp.generated.resources.onboarding_feature_primary_calendar
import smartcalai.composeapp.generated.resources.onboarding_feature_smart_suggestions
import smartcalai.composeapp.generated.resources.onboarding_feature_voice_commands
import smartcalai.composeapp.generated.resources.onboarding_step_1_of_2
import smartcalai.composeapp.generated.resources.onboarding_step_2_of_2
import smartcalai.composeapp.generated.resources.onboarding_title_free_plan
import smartcalai.composeapp.generated.resources.onboarding_title_smart_calendar
import smartcalai.composeapp.generated.resources.play_store_512
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcal.app.presentation.subscription.RevenueCatPaywallScreen
import com.smartcal.app.utils.componentsui.Contactless
import com.smartcal.app.utils.componentsui.Eye
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(1) }
    var showPaywall by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        when {
            showPaywall -> {
                RevenueCatPaywallScreen(
                    onBackClick = { showPaywall = false },
                    onSubscriptionSuccess = { 
                        showPaywall = false
                        onOnboardingComplete()
                    }
                )
            }
            currentStep == 1 -> FirstOnboardingStep(
                onNext = { currentStep = 2 },
                currentStep = currentStep,
                onStepClick = { step -> currentStep = step }
            )
            currentStep == 2 -> SecondOnboardingStep(
                onComplete = onOnboardingComplete,
                onShowPaywall = { showPaywall = true },
                currentStep = currentStep,
                onStepClick = { step -> currentStep = step }
            )
        }
    }
}

@Composable
private fun FirstOnboardingStep(
    onNext: () -> Unit,
    currentStep: Int = 1,
    onStepClick: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        Text(
            text = stringResource(Res.string.onboarding_step_1_of_2),
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 40.dp)
        )

        // Progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clickable { onStepClick(1) }
                    .background(Color(0xFF1976D2), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clickable { onStepClick(2) }
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        // App icon placeholder - using calendar icon for now
        Box(
            modifier = Modifier
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.play_store_512),
                contentDescription = stringResource(Res.string.onboarding_app_icon_description),
                modifier = Modifier.size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = stringResource(Res.string.onboarding_title_smart_calendar),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feature list
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureItem(
                text = stringResource(Res.string.onboarding_feature_organize)
            )
            FeatureItem(
                text = stringResource(Res.string.onboarding_feature_check_calendars)
            )
            FeatureItem(
                text = stringResource(Res.string.onboarding_feature_voice_commands)
            )
            FeatureItem(
                text = stringResource(Res.string.onboarding_feature_smart_suggestions)
            )
            FeatureItem(
                text = stringResource(Res.string.onboarding_feature_multiple_calendars)
            )
            FeatureItem(
                text = stringResource(Res.string.onboarding_feature_natural_language)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2)
            ),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                text = stringResource(Res.string.onboarding_button_next),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SecondOnboardingStep(
    onComplete: () -> Unit,
    onShowPaywall: () -> Unit,
    currentStep: Int = 2,
    onStepClick: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        Text(
            text = stringResource(Res.string.onboarding_step_2_of_2),
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 40.dp)
        )

        // Progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clickable { onStepClick(1) }
                    .background(Color(0xFF1976D2), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clickable { onStepClick(2) }
                    .background(Color(0xFF1976D2), RoundedCornerShape(2.dp))
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Credits design with overlapping label on upper right
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Outlined circle with light blue background
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFFF0F8FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.Transparent, CircleShape)
                        .background(
                            Color(0xFF1976D2).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.onboarding_credits_number),
                        color = Color(0xFF1976D2),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Credits label overlapping on the upper right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0xFF1976D2), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_credits_label),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = stringResource(Res.string.onboarding_title_free_plan),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Features list
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PlanFeatureItem(
                text = stringResource(Res.string.onboarding_feature_credits),
                icon = Contactless
            )
            PlanFeatureItem(
                text = stringResource(Res.string.onboarding_feature_primary_calendar),
                icon = Eye
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1 question = 1 credit text
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = stringResource(Res.string.onboarding_credit_info),
            color = Color(0xFF1976D2),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        // Start for free button
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2)
            ),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                text = stringResource(Res.string.onboarding_button_start_free),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // See All Plans link
        TextButton(onClick = { onShowPaywall() }) {
            Text(
                text = stringResource(Res.string.onboarding_button_see_all_plans),
                color = Color(0xFF1976D2),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color(0xFF4CAF50), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlanFeatureItem(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Icon(
            imageVector = icon,
            contentDescription = icon.name,
            modifier = Modifier
                .size(20.dp),
            tint = Color(0xFF1976D2)
        )

        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}