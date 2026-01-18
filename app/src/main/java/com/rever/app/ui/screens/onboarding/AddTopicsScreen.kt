package com.rever.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rever.app.ui.components.*
import com.rever.app.ui.viewmodel.TopicInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTopicsScreen(
    topics: List<TopicInput>,
    bulkTopicsInput: String,
    onBulkTopicsInputChange: (String) -> Unit,
    onImportBulkTopics: () -> Unit,
    onAddTopic: (String) -> Unit,
    onRemoveTopic: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    var newTopicName by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Topics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = "Import Topics"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add the topics you need to revise",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add topic input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReverOutlinedTextField(
                    value = newTopicName,
                    onValueChange = { newTopicName = it },
                    label = "Topic Name",
                    modifier = Modifier.weight(1f)
                )
                
                FilledIconButton(
                    onClick = {
                        if (newTopicName.isNotBlank()) {
                            onAddTopic(newTopicName)
                            newTopicName = ""
                        }
                    },
                    enabled = newTopicName.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add Topic"
                    )
                }
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Topics list
            if (topics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No topics added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add topics above or import a list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "${topics.size} topic${if (topics.size != 1) "s" else ""} added",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(topics) { index, topic ->
                        ReverOutlinedCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = topic.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${topic.estimatedTimeMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                IconButton(onClick = { onRemoveTopic(index) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ReverFilledButton(
                text = if (isLoading) "Saving..." else "Next: Set Daily Time",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                enabled = topics.isNotEmpty() && !isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Topics") },
            text = {
                Column {
                    Text(
                        text = "Paste topics below (one per line)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverMultilineTextField(
                        value = bulkTopicsInput,
                        onValueChange = onBulkTopicsInputChange,
                        label = "Topics",
                        placeholder = "Topic 1\nTopic 2\nTopic 3"
                    )
                }
            },
            confirmButton = {
                ReverFilledButton(
                    text = "Import",
                    onClick = {
                        onImportBulkTopics()
                        showImportDialog = false
                    },
                    enabled = bulkTopicsInput.isNotBlank()
                )
            },
            dismissButton = {
                ReverTextButton(
                    text = "Cancel",
                    onClick = { showImportDialog = false }
                )
            }
        )
    }
}
