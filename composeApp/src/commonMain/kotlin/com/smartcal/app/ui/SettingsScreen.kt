package com.smartcal.app.ui

import smartcal.composeapp.generated.resources.Res
import smartcal.composeapp.generated.resources.cancel_button
import smartcal.composeapp.generated.resources.credits_available_label
import smartcal.composeapp.generated.resources.current_plan_label
import smartcal.composeapp.generated.resources.delete_account_button
import smartcal.composeapp.generated.resources.delete_account_confirmation_message
import smartcal.composeapp.generated.resources.delete_account_confirmation_title
import smartcal.composeapp.generated.resources.free_plan
import smartcal.composeapp.generated.resources.logout_button
import smartcal.composeapp.generated.resources.logout_confirmation_message
import smartcal.composeapp.generated.resources.logout_confirmation_title
import smartcal.composeapp.generated.resources.no_credits_info
import smartcal.composeapp.generated.resources.privacy_policy
import smartcal.composeapp.generated.resources.profile_picture
import smartcal.composeapp.generated.resources.terms_and_conditions
import smartcal.composeapp.generated.resources.title_settings
import smartcal.composeapp.generated.resources.subscription_success_message
import smartcal.composeapp.generated.resources.subscription_success_message_generic
import smartcal.composeapp.generated.resources.view_plans_button
import smartcal.composeapp.generated.resources.loading_profile
import smartcal.composeapp.generated.resources.credits_renewal_label
import smartcal.composeapp.generated.resources.credits_renew_in_days
import smartcal.composeapp.generated.resources.credits_renew_tomorrow
import smartcal.composeapp.generated.resources.credits_renew_today
import smartcal.composeapp.generated.resources.credits_renewal_date
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.smartcal.app.utils.componentsui.Contactless
import com.smartcal.app.utils.componentsui.Upgrade
import com.smartcal.app.utils.getTermsAndConditionsUrl
import com.smartcal.app.utils.openBrowser
import com.mohamedrejeb.calf.ui.dialog.AdaptiveAlertDialog
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userEmail: String?,
    fullName: String?,
    profilePicture: String?,
    subscriptionPlan: String?,
    creditsRemaining: Int?,
    creditsRenewalDate: String? = null,
    daysUntilRenewal: Int? = null,
    isLoadingProfile: Boolean = false,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onUpgradePlan: () -> Unit,
    modifier: Modifier = Modifier,
    successPlanLabel: String? = null,
    onSuccessMessageShown: () -> Unit = {}
) {
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Pre-compute localized success message in composable scope
    val localizedSuccessMessage = if ((successPlanLabel ?: "").isNotBlank()) {
        stringResource(Res.string.subscription_success_message, successPlanLabel!!)
    } else {
        stringResource(Res.string.subscription_success_message_generic)
    }

    // Show subscription success message once when arriving from paywall
    LaunchedEffect(successPlanLabel) {
        if (successPlanLabel != null) {
            snackbarHostState.showSnackbar(message = localizedSuccessMessage)
            // Notify parent so it can clear the flag and avoid showing again
            onSuccessMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // 👇 Si este Settings vive dentro de un Scaffold padre (con bottom bar), evita insets dobles:
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.title_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->

        // 👇 Usamos el innerPadding para el contenido (no como Modifier.padding)
        val contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 16.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // 👇 Consumí los insets para que no se vuelvan a aplicar en hijos
                .consumeWindowInsets(innerPadding),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------- LOADING INDICATOR ----------
            if (isLoadingProfile) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.loading_profile),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // ---------- PROFILE ----------
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!profilePicture.isNullOrBlank()) {
                            AsyncImage(
                                model = profilePicture,
                                contentDescription = stringResource(Res.string.profile_picture),
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)

                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (fullName?.firstOrNull()
                                        ?: userEmail?.firstOrNull()
                                        ?: 'U').uppercaseChar().toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = fullName ?: userEmail ?: "Usuario",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ---------- PLAN ----------
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Upgrade,
                                contentDescription = "Plan",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(Res.string.current_plan_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = subscriptionPlan ?: stringResource(Res.string.free_plan),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onUpgradePlan,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(Res.string.view_plans_button)) }
                    }
                }
            }

            // ---------- CREDITS ----------
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Contactless,
                                contentDescription = "Credits",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(Res.string.credits_available_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = creditsRemaining?.toString()
                                        ?: stringResource(Res.string.no_credits_info),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Show renewal information if available
                        if (daysUntilRenewal != null || creditsRenewalDate != null) {
                            Spacer(Modifier.height(12.dp))
                            
                            Text(
                                text = stringResource(Res.string.credits_renewal_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            
                            // Show days until renewal with appropriate text
                            when (daysUntilRenewal) {
                                0 -> Text(
                                    text = stringResource(Res.string.credits_renew_today),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                1 -> Text(
                                    text = stringResource(Res.string.credits_renew_tomorrow),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                in 2..Int.MAX_VALUE -> Text(
                                    text = stringResource(Res.string.credits_renew_in_days, daysUntilRenewal ?: 0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                else -> {
                                    // Fallback to showing renewal date if no days info
                                    creditsRenewalDate?.let { date ->
                                        Text(
                                            text = stringResource(Res.string.credits_renewal_date, date),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------- LOGOUT ----------
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Button(
                        onClick = { showLogoutConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.logout_button),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            // ---------- DELETE ACCOUNT ----------
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Button(
                        onClick = { showDeleteAccountConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.delete_account_button),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ---------- LEGAL LINKS ----------
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.privacy_policy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            openBrowser("https://docs.google.com/document/d/e/2PACX-1vRVxXnObSSfqd5bFSQiodyOyKO0HkG8uZFIAhoj457OzZ43l6D0EW0oeyQbvrA3zCM_-IFZBIUfOKKM/pub")
                        }
                    )
                    
                    Text(
                        text = stringResource(Res.string.terms_and_conditions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            openBrowser(getTermsAndConditionsUrl())
                        }
                    )
                }
            }
        }
    }

    if (showLogoutConfirmation) {
        AdaptiveAlertDialog(
            onConfirm = { onLogout(); showLogoutConfirmation = false },
            onDismiss = { showLogoutConfirmation = false },
            title = stringResource(Res.string.logout_confirmation_title),
            text = stringResource(Res.string.logout_confirmation_message),
            confirmText = stringResource(Res.string.logout_button),
            dismissText = stringResource(Res.string.cancel_button)
        )
    }

    if (showDeleteAccountConfirmation) {
        AdaptiveAlertDialog(
            onConfirm = { onDeleteAccount(); showDeleteAccountConfirmation = false },
            onDismiss = { showDeleteAccountConfirmation = false },
            title = stringResource(Res.string.delete_account_confirmation_title),
            text = stringResource(Res.string.delete_account_confirmation_message),
            confirmText = stringResource(Res.string.delete_account_button),
            dismissText = stringResource(Res.string.cancel_button)
        )
    }
}
