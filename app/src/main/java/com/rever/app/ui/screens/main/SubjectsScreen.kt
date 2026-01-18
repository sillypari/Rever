package com.rever.app.ui.screens.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rever.app.data.model.Subject
import com.rever.app.data.model.Topic
import com.rever.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    subjects: List<Subject>,
    topicCountBySubject: Map<Long, Int> = emptyMap(),
    selectedSubject: Subject?,
    topicsForSelectedSubject: List<Topic>,
    showDeleteDialog: Boolean,
    onSelectSubject: (Subject) -> Unit,
    onClearSelectedSubject: () -> Unit,
    onShowDeleteConfirmation: (Subject) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onAddSubject: (String, String) -> Unit,
    onAddTopic: (Long, String) -> Unit,
    onDeleteTopic: (Topic) -> Unit,
    onImportFile: (Uri) -> Unit = {}
) {
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }
    var newSubjectDescription by remember { mutableStateOf("") }
    var newTopicName by remember { mutableStateOf("") }
    var isFabExpanded by remember { mutableStateOf(false) }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportFile(it) }
        isFabExpanded = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (selectedSubject != null) selectedSubject.name else "Your Subjects"
                    ) 
                },
                navigationIcon = {
                    if (selectedSubject != null) {
                        IconButton(onClick = onClearSelectedSubject) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (selectedSubject != null) {
                        IconButton(onClick = { onShowDeleteConfirmation(selectedSubject) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Subject",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        },
        floatingActionButton = {
            // Expandable FAB for subject list view
            if (selectedSubject == null) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Expanded options
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Import option
                            ExtendedFloatingActionButton(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf(
                                        "application/json",
                                        "text/plain",
                                        "text/*",
                                        "*/*"
                                    ))
                                },
                                icon = {
                                    Icon(
                                        Icons.Outlined.CloudUpload,
                                        contentDescription = "Import"
                                    )
                                },
                                text = { Text("Import from File") },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            // Add manually option
                            ExtendedFloatingActionButton(
                                onClick = {
                                    showAddSubjectDialog = true
                                    isFabExpanded = false
                                },
                                icon = {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Add"
                                    )
                                },
                                text = { Text("Add Manually") },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // Main FAB
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Outlined.Close else Icons.Outlined.Add,
                            contentDescription = if (isFabExpanded) "Close" else "Add"
                        )
                    }
                }
            } else {
                // Simple FAB for topic view
                FloatingActionButton(
                    onClick = { showAddTopicDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add Topic")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedSubject != null) {
                // Show topics for selected subject
                if (topicsForSelectedSubject.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No topics yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add topics to this subject",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(topicsForSelectedSubject) { topic ->
                            ReverOutlinedCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = topic.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${topic.estimatedTimeMinutes} min • ${topic.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    IconButton(onClick = { onDeleteTopic(topic) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Show subjects list
                if (subjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No subjects yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add your first subject to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(subjects) { subject ->
                            SubjectCard(
                                subjectName = subject.name,
                                description = subject.description,
                                topicCount = topicCountBySubject[subject.id] ?: 0,
                                onClick = { onSelectSubject(subject) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add Subject Dialog
    if (showAddSubjectDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddSubjectDialog = false
                newSubjectName = ""
                newSubjectDescription = ""
            },
            title = { Text("Add Subject") },
            text = {
                Column {
                    ReverOutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = "Subject Name"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverOutlinedTextField(
                        value = newSubjectDescription,
                        onValueChange = { newSubjectDescription = it },
                        label = "Description (optional)"
                    )
                }
            },
            confirmButton = {
                ReverFilledButton(
                    text = "Add",
                    onClick = {
                        if (newSubjectName.isNotBlank()) {
                            onAddSubject(newSubjectName, newSubjectDescription)
                            showAddSubjectDialog = false
                            newSubjectName = ""
                            newSubjectDescription = ""
                        }
                    },
                    enabled = newSubjectName.isNotBlank()
                )
            },
            dismissButton = {
                ReverTextButton(
                    text = "Cancel",
                    onClick = {
                        showAddSubjectDialog = false
                        newSubjectName = ""
                        newSubjectDescription = ""
                    }
                )
            }
        )
    }
    
    // Add Topic Dialog
    if (showAddTopicDialog && selectedSubject != null) {
        AlertDialog(
            onDismissRequest = { 
                showAddTopicDialog = false
                newTopicName = ""
            },
            title = { Text("Add Topic") },
            text = {
                ReverOutlinedTextField(
                    value = newTopicName,
                    onValueChange = { newTopicName = it },
                    label = "Topic Name"
                )
            },
            confirmButton = {
                ReverFilledButton(
                    text = "Add",
                    onClick = {
                        if (newTopicName.isNotBlank()) {
                            onAddTopic(selectedSubject.id, newTopicName)
                            showAddTopicDialog = false
                            newTopicName = ""
                        }
                    },
                    enabled = newTopicName.isNotBlank()
                )
            },
            dismissButton = {
                ReverTextButton(
                    text = "Cancel",
                    onClick = {
                        showAddTopicDialog = false
                        newTopicName = ""
                    }
                )
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text("Delete Subject?") },
            text = { 
                Text("Are you sure you want to delete this subject and all its topics? This action cannot be undone.") 
            },
            confirmButton = {
                ReverFilledButton(
                    text = "Delete",
                    onClick = onConfirmDelete
                )
            },
            dismissButton = {
                ReverTextButton(
                    text = "Cancel",
                    onClick = onDismissDeleteDialog
                )
            }
        )
    }
}
