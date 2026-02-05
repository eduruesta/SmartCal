package com.smartcal.app.ui.components

import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.cd_back
import smartcalai.composeapp.generated.resources.done_button
import smartcalai.composeapp.generated.resources.error_contacts
import smartcalai.composeapp.generated.resources.no_contacts
import smartcalai.composeapp.generated.resources.profile_picture
import smartcalai.composeapp.generated.resources.search_placeholder
import smartcalai.composeapp.generated.resources.select_contacts_title
import smartcalai.composeapp.generated.resources.selected_contact
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.smartcal.app.models.Contact
import com.smartcal.app.models.ContactsResponse
import com.smartcal.app.utils.componentsui.ArrowBack
import com.smartcal.app.utils.componentsui.Check
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsPickerScreen(
    loadContacts: suspend () -> Result<ContactsResponse>,
    onCancel: () -> Unit,
    onDone: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val selectedContactIds = remember { mutableStateListOf<String>() }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        val result = loadContacts()
        result.fold(
            onSuccess = { resp ->
                contacts = resp.contacts
                    .filter { it.emailAddresses?.isNotEmpty() == true }
                    .sortedBy {
                        (it.displayName ?: it.names?.firstOrNull()?.displayName ?: "").lowercase()
                    }
                isLoading = false
            },
            onFailure = { e ->
                error = e.message ?: "Error"
                isLoading = false
            }
        )
    }

    // Derived filtered list and flat list with headers
    val filtered = remember(contacts, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) contacts else contacts.filter { c ->
            val name = c.displayName ?: c.names?.firstOrNull()?.displayName ?: ""
            name.lowercase().contains(q)
        }
    }

    data class ListItem(val header: Char? = null, val contact: Contact? = null)

    val itemsList = remember(filtered) {
        val sorted = filtered.sortedBy {
            (it.displayName ?: it.names?.firstOrNull()?.displayName ?: "").lowercase()
        }
        val list = mutableListOf<ListItem>()
        var lastHeader: Char? = null
        for (c in sorted) {
            val name = c.displayName ?: c.names?.firstOrNull()?.displayName ?: ""
            val letter = name.trim().firstOrNull()?.uppercaseChar() ?: '#'
            if (lastHeader != letter) {
                list.add(ListItem(header = letter))
                lastHeader = letter
            }
            list.add(ListItem(contact = c))
        }
        list
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.select_contacts_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = ArrowBack, contentDescription = stringResource(Res.string.cd_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (selectedContactIds.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(onClick = {
                        // Collect emails of selected contacts (first email each)
                        val emails =
                            contacts.filter { selectedContactIds.contains(it.resourceName) }
                                .mapNotNull { it.emailAddresses?.firstOrNull()?.value }
                        onDone(emails)
                    }) {
                        Text(stringResource(Res.string.done_button))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                singleLine = true
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(stringResource(Res.string.error_contacts))
                }

                itemsList.isEmpty() -> {
                    Text(stringResource(Res.string.no_contacts))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(itemsList) { listItem ->
                            if (listItem.header != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = listItem.header.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                val contact = listItem.contact
                                if (contact != null) {
                                    val name = contact.displayName
                                        ?: contact.names?.firstOrNull()?.displayName
                                        ?: ""
                                    val emails = contact.emailAddresses ?: emptyList()
                                    if (emails.isNotEmpty()) {
                                        val selected =
                                            selectedContactIds.contains(contact.resourceName)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (selected) selectedContactIds.remove(contact.resourceName) else selectedContactIds.add(
                                                        contact.resourceName
                                                    )
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val profilePictureUrl =
                                                contact.photos?.firstOrNull()?.url
                                            if (selected) {
                                                Icon(
                                                    imageVector = Check,
                                                    contentDescription = stringResource(Res.string.selected_contact),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .padding(end = 16.dp)
                                                        .size(40.dp)
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = profilePictureUrl,
                                                    contentDescription = stringResource(Res.string.profile_picture),
                                                    modifier = Modifier
                                                        .padding(end = 16.dp)
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surface)
                                                )
                                            }

                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    name,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Column(Modifier.padding(top = 2.dp)) {
                                                    for (email in emails) {
                                                        Text(
                                                            email.value,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}