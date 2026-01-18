package com.rever.app.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rever.app.navigation.BottomNavItem
import com.rever.app.navigation.Screen
import com.rever.app.ui.viewmodel.PlanViewModel
import com.rever.app.ui.viewmodel.ProgressViewModel
import com.rever.app.ui.viewmodel.SubjectsViewModel

@Composable
fun MainScreen(
    planViewModel: PlanViewModel = viewModel(),
    subjectsViewModel: SubjectsViewModel = viewModel(),
    progressViewModel: ProgressViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val items = listOf(
        BottomNavItem.Plan,
        BottomNavItem.Subjects,
        BottomNavItem.Progress
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { 
                        it.route == item.route 
                    } == true
                    
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Plan.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Plan.route) {
                val state by planViewModel.state.collectAsState()
                
                PlanScreen(
                    weekDates = state.weekDates,
                    selectedDate = state.selectedDate,
                    sessions = state.sessionsForSelectedDate,
                    groupedSessions = state.groupedSessions,
                    monthSessionsByDate = state.monthSessionsByDate,
                    infiniteScrollDays = state.infiniteScrollDays,
                    isLoadingMore = state.isLoadingMore,
                    totalMinutesForDay = state.totalMinutesForDay,
                    completedMinutesForDay = state.completedMinutesForDay,
                    dailyTime = state.dailyTime,
                    isPlanGenerated = state.isPlanGenerated,
                    onDateSelected = planViewModel::selectDate,
                    onToggleComplete = planViewModel::toggleSessionCompletion,
                    onCompleteWithConfidence = planViewModel::completeSessionWithConfidence,
                    onUpdateCompletionPercentage = planViewModel::updateCompletionPercentage,
                    onPreviousWeek = planViewModel::navigateToPreviousWeek,
                    onNextWeek = planViewModel::navigateToNextWeek,
                    onGeneratePlan = planViewModel::generatePlan,
                    onImportFile = planViewModel::importFromFile,
                    onShowDailyTimeDialog = planViewModel::showDailyTimeDialog,
                    onLoadMoreDays = planViewModel::loadMoreDays,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onClearError = planViewModel::clearError,
                    importSuccess = state.importSuccess,
                    importedCount = state.importedCount,
                    onClearImportSuccess = planViewModel::clearImportSuccess
                )
                
                // Daily time dialog
                if (state.showDailyTimeDialog) {
                    DailyTimeDialog(
                        currentTime = state.dailyTime,
                        onDismiss = planViewModel::hideDailyTimeDialog,
                        onConfirm = planViewModel::updateDailyTime
                    )
                }
            }
            
            composable(Screen.Subjects.route) {
                val state by subjectsViewModel.state.collectAsState()
                
                SubjectsScreen(
                    subjects = state.subjects,
                    topicCountBySubject = state.topicCountBySubject,
                    selectedSubject = state.selectedSubject,
                    topicsForSelectedSubject = state.topicsForSelectedSubject,
                    showDeleteDialog = state.showDeleteDialog,
                    onSelectSubject = subjectsViewModel::selectSubject,
                    onClearSelectedSubject = subjectsViewModel::clearSelectedSubject,
                    onShowDeleteConfirmation = subjectsViewModel::showDeleteConfirmation,
                    onConfirmDelete = subjectsViewModel::confirmDeleteSubject,
                    onDismissDeleteDialog = subjectsViewModel::dismissDeleteDialog,
                    onAddSubject = { name, description ->
                        subjectsViewModel.createSubject(name, description) { }
                    },
                    onAddTopic = subjectsViewModel::addTopic,
                    onDeleteTopic = subjectsViewModel::deleteTopic,
                    onImportFile = planViewModel::importFromFile
                )
            }
            
            composable(Screen.Progress.route) {
                val state by progressViewModel.state.collectAsState()
                
                ProgressScreen(
                    totalTopics = state.totalTopics,
                    completedTopics = state.completedTopics,
                    pendingToday = state.pendingToday,
                    overdueCount = state.overdueCount,
                    overdueSessions = state.overdueSessions,
                    allSessions = state.allSessions,
                    weeklyProgress = state.weeklyProgress,
                    totalMinutesStudied = state.totalMinutesStudied,
                    currentStreak = state.currentStreak,
                    recentCompletedSessions = state.recentCompletedSessions,
                    onToggleSessionComplete = progressViewModel::toggleSessionCompletion
                )
            }
            
            composable(Screen.Settings.route) {
                val planState by planViewModel.state.collectAsState()
                
                SettingsScreen(
                    dailyRevisionTime = planState.dailyTime,
                    onUpdateDailyTime = planViewModel::updateDailyTime,
                    onNavigateBack = { navController.popBackStack() },
                    onClearAllData = { /* TODO: Implement clear all data */ }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTimeDialog(
    currentTime: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedTime by remember { mutableStateOf(currentTime) }
    
    val timeOptions = listOf(15, 30, 45, 60, 90, 120, 180)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Revision Time") },
        text = {
            Column {
                Text(
                    text = "How much time do you want to spend on revision each day?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                timeOptions.forEach { minutes ->
                    val hours = minutes / 60
                    val mins = minutes % 60
                    val label = when {
                        hours > 0 && mins > 0 -> "$hours hr $mins min"
                        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
                        else -> "$mins minutes"
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTime = minutes }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTime == minutes,
                            onClick = { selectedTime = minutes }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedTime) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
