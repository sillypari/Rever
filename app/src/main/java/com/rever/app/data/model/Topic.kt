package com.rever.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a topic within a subject with enhanced fields for smart scheduling
 */
@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Topic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val estimatedTimeMinutes: Int = 20, // Default 20 minutes per topic
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val type: TopicType = TopicType.THEORY,
    val priority: Int = 2, // 1=Low, 2=Medium, 3=High
    
    // Enhanced scheduling fields
    val subtopicsCount: Int = 0, // Number of subtopics for complexity estimation
    val prerequisites: String = "", // Comma-separated topic names
    val tags: String = "", // Comma-separated tags: Important,Exam-likely,Weak-area,Formula-heavy
    val notes: String = "", // Quick recap points or links
    
    // Auto-tracked fields for spaced repetition
    val lastRevisedDate: Long? = null,
    val revisionCount: Int = 0,
    val confidenceLevel: Int = 3, // 1-5 stars, default 3
    
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculate next revision interval based on spaced repetition
     * Intervals: 1 day → 3 days → 7 days → 16 days → 35 days
     */
    fun getNextRevisionIntervalDays(): Int {
        return when (revisionCount) {
            0 -> 1
            1 -> 3
            2 -> 7
            3 -> 16
            4 -> 35
            else -> 35 + (revisionCount - 4) * 14 // Continue with 14-day increments
        }
    }
    
    /**
     * Adjust interval based on confidence level
     * Low confidence (1-2) = shorter intervals
     * High confidence (4-5) = longer intervals
     */
    fun getAdjustedRevisionIntervalDays(): Int {
        val baseInterval = getNextRevisionIntervalDays()
        return when (confidenceLevel) {
            1 -> (baseInterval * 0.5).toInt().coerceAtLeast(1)
            2 -> (baseInterval * 0.75).toInt().coerceAtLeast(1)
            4 -> (baseInterval * 1.25).toInt()
            5 -> (baseInterval * 1.5).toInt()
            else -> baseInterval
        }
    }
    
    fun getTagsList(): List<String> {
        return if (tags.isBlank()) emptyList()
        else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    
    fun getPrerequisitesList(): List<String> {
        return if (prerequisites.isBlank()) emptyList()
        else prerequisites.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    
    fun hasTag(tag: String): Boolean {
        return getTagsList().any { it.equals(tag, ignoreCase = true) }
    }
    
    fun isImportant(): Boolean = hasTag("Important") || hasTag("Exam-likely")
    fun isWeakArea(): Boolean = hasTag("Weak-area")
    fun isFormulaHeavy(): Boolean = hasTag("Formula-heavy")
}

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD;
    
    companion object {
        fun fromString(str: String): Difficulty {
            return when (str.lowercase().trim()) {
                "easy", "low", "simple" -> EASY
                "hard", "high", "difficult", "complex" -> HARD
                else -> MEDIUM
            }
        }
    }
    
    fun displayName(): String {
        return when (this) {
            EASY -> "Easy"
            MEDIUM -> "Medium"
            HARD -> "Hard"
        }
    }
}
