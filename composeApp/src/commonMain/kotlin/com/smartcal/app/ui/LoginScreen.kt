package com.smartcal.app.ui

import smartcalai.composeapp.generated.resources.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcal.app.models.AuthState
import com.smartcal.app.utils.openBrowser
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import smartcalai.composeapp.generated.resources.Res
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.smartcal.app.utils.getErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authState: AuthState,
    onLoginStart: () -> Unit,
    onError: () -> Unit,
    onAuthUrl: (String, String) -> Unit, // authUrl, sessionId
    onCancelOAuth: (() -> Unit)? = null
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // App name
        Text(
            text = stringResource(Res.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )

        // Logo
        Box(
            modifier = Modifier
                .size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.play_store_512),
                contentDescription = stringResource(Res.string.onboarding_app_icon_description),
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pre-login description
        Text(
            text = stringResource(Res.string.prelogin_description),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onLoginStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !authState.isLoading
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(Res.string.sign_in_button))
            }
        }
        
        // Show cancel button and message when in OAuth flow
        if (authState.isLoading && onCancelOAuth != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Please complete the authentication in your browser.\nIf you encounter issues, you can cancel and try again.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onCancelOAuth,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Authentication")
            }
        }
        
        // Handle browser auth trigger
        if (authState.needsBrowserAuth && authState.authUrl != null && authState.sessionId != null) {
            onAuthUrl(authState.authUrl, authState.sessionId)
        }

        if (authState.error != null) {
            Text(
                text = getErrorMessage(authState.error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.sign_in_disclaimer),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { openBrowser("https://gendaai.com/privacy.html") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.prelogin_view_privacy_policy))
        }

        TextButton(
            onClick = { openBrowser("https://gendaai.com") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.prelogin_visit_site))
        }
    }
}