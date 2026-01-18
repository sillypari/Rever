package com.rever.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rever.app.ui.components.ReverFilledButton
import com.rever.app.ui.components.ReverOutlinedTextField
import com.rever.app.ui.components.ReverSelectionChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDailyTimeScreen(
    selectedTimeMinutes: Int,
    onTimeSelected: (Int) -> Unit,
    onGeneratePlan: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }
    
    val timeOptions = listOf(
        30 to "30 min",
        60 to "1 hour",
        90 to "1.5 hours",
        120 to "2 hours"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Revision Time") },
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
                text = "How much time can you devote for revision each day?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "This helps us create an optimal revision schedule for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Time options grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    timeOptions.take(2).forEach { (minutes, label) ->
                        ReverSelectionChip(
                            text = label,
                            selected = selectedTimeMinutes == minutes && !showCustomInput,
                            onClick = {
                                showCustomInput = false
                                onTimeSelected(minutes)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    timeOptions.drop(2).forEach { (minutes, label) ->
                        ReverSelectionChip(
                            text = label,
                            selected = selectedTimeMinutes == minutes && !showCustomInput,
                            onClick = {
                                showCustomInput = false
                                onTimeSelected(minutes)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Custom option
                ReverSelectionChip(
                    text = "Custom",
                    selected = showCustomInput,
                    onClick = { showCustomInput = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Custom input field
            if (showCustomInput) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() } && value.length <= 3) {
                                customMinutes = value
                                value.toIntOrNull()?.let { onTimeSelected(it) }
                            }
                        },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    Text(
                        text = "minutes per day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Selected time summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your daily revision time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTime(selectedTimeMinutes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            ReverFilledButton(
                text = if (isLoading) "Generating Plan..." else "Generate Plan",
                onClick = onGeneratePlan,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedTimeMinutes > 0 && !isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatTime(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} hour${if (minutes / 60 > 1) "s" else ""}"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            "${hours}h ${mins}m"
        }
    }
}
