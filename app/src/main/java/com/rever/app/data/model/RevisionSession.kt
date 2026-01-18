package com.rever.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a scheduled revision session for a topic on a specific date
 */
@Entity(
    tableName = "revision_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["topicId"]),
        Index(value = ["date"]),
        Index(value = ["date", "isCompleted"])
    ]
)
data class RevisionSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: Long,
    val date: Long, // Date as epoch millis (start of day)
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val completionPercentage: Int = 0, // 0-100 percentage of topic covered
    val completedAt: Long? = null
)
