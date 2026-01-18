package com.rever.app.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rever.app.ui.screens.main.MainScreen
import com.rever.app.ui.screens.onboarding.*
import com.rever.app.ui.viewmodel.OnboardingViewModel
import com.rever.app.ui.viewmodel.PlanViewModel

@Composable
fun ReverNavGraph(
    onboardingCompleted: Boolean,
    hasExistingSubjects: Boolean = false,
    onboardingViewModel: OnboardingViewModel = viewModel(),
    planViewModel: PlanViewModel = viewModel()
) {
    val navController = rememberNavController()
    val state by onboardingViewModel.state.collectAsState()
    
    // If onboarding is completed OR user has existing subjects, go to Main directly
    val startDestination = if (onboardingCompleted || hasExistingSubjects) {
        Screen.Main.route
    } else {
        Screen.Welcome.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding Flow
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                hasExistingData = hasExistingSubjects,
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onGetStartedManually = {
                    navController.navigate(Screen.CreateSubject.route)
                },
                onImportFile = { uri ->
                    // Import file and navigate to daily time setup
                    planViewModel.importFromFile(uri)
                    navController.navigate(Screen.SetDailyTime.route)
                }
            )
        }
        
        composable(Screen.CreateSubject.route) {
            CreateSubjectScreen(
                subjectName = state.subjectName,
                subjectDescription = state.subjectDescription,
                onSubjectNameChange = onboardingViewModel::updateSubjectName,
                onSubjectDescriptionChange = onboardingViewModel::updateSubjectDescription,
                onNext = {
                    onboardingViewModel.createSubject { subjectId ->
                        navController.navigate(Screen.AddTopics.createRoute(subjectId))
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                isLoading = state.isLoading,
                errorMessage = state.errorMessage
            )
        }
        
        composable(
            route = Screen.AddTopics.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: 0L
            
            AddTopicsScreen(
                topics = state.topics,
                bulkTopicsInput = state.bulkTopicsInput,
                onBulkTopicsInputChange = onboardingViewModel::updateBulkTopicsInput,
                onImportBulkTopics = onboardingViewModel::importBulkTopics,
                onAddTopic = { name -> onboardingViewModel.addTopic(name) },
                onRemoveTopic = onboardingViewModel::removeTopic,
                onNext = {
                    onboardingViewModel.saveTopics(subjectId) {
                        navController.navigate(Screen.SetDailyTime.route)
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                isLoading = state.isLoading,
                errorMessage = state.errorMessage
            )
        }
        
        composable(Screen.SetDailyTime.route) {
            SetDailyTimeScreen(
                selectedTimeMinutes = state.dailyTimeMinutes,
                onTimeSelected = onboardingViewModel::updateDailyTime,
                onGeneratePlan = {
                    onboardingViewModel.completeOnboarding {
                        // Generate the plan
                        planViewModel.generatePlan()
                        // Navigate to main screen
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                isLoading = state.isLoading
            )
        }
        
        // Main App
        composable(Screen.Main.route) {
            MainScreen(planViewModel = planViewModel)
        }
    }
}
