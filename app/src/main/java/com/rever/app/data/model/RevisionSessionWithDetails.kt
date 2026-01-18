package com.rever.app.data.model

/**
 * Combined data class for displaying a revision session with topic and subject info
 */
data class RevisionSessionWithDetails(
    val sessionId: Long,
    val topicId: Long,
    val topicName: String,
    val subjectId: Long,
    val subjectName: String,
    val subjectColor: String = "#4ECDC4",
    val date: Long,
    val durationMinutes: Int,
    val isCompleted: Boolean,
    val completionPercentage: Int = 0, // 0-100 percentage
    val difficulty: Difficulty,
    // Enhanced fields from Topic model
    val topicType: TopicType = TopicType.THEORY,
    val priority: Int = 2, // 1=Low, 2=Medium, 3=High
    val tags: String = "",
    val confidenceLevel: Int = 3, // 1-5 stars
    val revisionCount: Int = 0
) {
    fun getTagsList(): List<String> {
        return if (tags.isBlank()) emptyList()
        else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    
    fun isImportant(): Boolean = getTagsList().any { 
        it.equals("Important", ignoreCase = true) || 
        it.equals("Exam-likely", ignoreCase = true) 
    }
    
    fun isWeakArea(): Boolean = getTagsList().any { 
        it.equals("Weak-area", ignoreCase = true) 
    }
    
    fun priorityLabel(): String = when (priority) {
        3 -> "High"
        2 -> "Medium"
        else -> "Low"
    }
}
