package com.rever.app.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    // Onboarding
    object Welcome : Screen("welcome")
    object CreateSubject : Screen("create_subject")
    object AddTopics : Screen("add_topics/{subjectId}") {
        fun createRoute(subjectId: Long) = "add_topics/$subjectId"
    }
    object SetDailyTime : Screen("set_daily_time")
    
    // Main screens
    object Main : Screen("main")
    object Plan : Screen("plan")
    object Subjects : Screen("subjects")
    object Progress : Screen("progress")
    object Settings : Screen("settings")
    
    // Detail screens
    object SubjectDetail : Screen("subject_detail/{subjectId}") {
        fun createRoute(subjectId: Long) = "subject_detail/$subjectId"
    }
    object EditSubject : Screen("edit_subject/{subjectId}") {
        fun createRoute(subjectId: Long) = "edit_subject/$subjectId"
    }
}
