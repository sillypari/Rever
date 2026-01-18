package com.rever.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rever.app.ui.components.ReverFilledButton
import com.rever.app.ui.components.ReverOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSubjectScreen(
    subjectName: String,
    subjectDescription: String,
    onSubjectNameChange: (String) -> Unit,
    onSubjectDescriptionChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Subject") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
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
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "What subject would you like to revise?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ReverOutlinedTextField(
                value = subjectName,
                onValueChange = onSubjectNameChange,
                label = "Subject Name",
                placeholder = "e.g., Mathematics, Physics",
                isError = errorMessage != null,
                errorMessage = errorMessage ?: ""
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ReverOutlinedTextField(
                value = subjectDescription,
                onValueChange = onSubjectDescriptionChange,
                label = "Description (optional)",
                placeholder = "e.g., Final exam preparation"
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            ReverFilledButton(
                text = if (isLoading) "Creating..." else "Next: Add Topics",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                enabled = subjectName.isNotBlank() && !isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
