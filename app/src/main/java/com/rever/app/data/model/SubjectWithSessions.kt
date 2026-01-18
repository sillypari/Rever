package com.rever.app.data.model

/**
 * Groups revision sessions by subject for display
 */
data class SubjectWithSessions(
    val subjectId: Long,
    val subjectName: String,
    val colorHex: String,
    val sessions: List<RevisionSessionWithDetails>,
    val totalMinutes: Int,
    val completedMinutes: Int
) {
    val completionPercentage: Float
        get() = if (totalMinutes > 0) completedMinutes.toFloat() / totalMinutes else 0f
}
