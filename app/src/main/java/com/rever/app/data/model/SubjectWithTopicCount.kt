package com.rever.app.data.model

/**
 * Data class for subject with topic count
 */
data class SubjectWithTopicCount(
    val subject: Subject,
    val topicCount: Int,
    val completedTopics: Int = 0
)
