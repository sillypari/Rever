package com.rever.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a study subject (e.g., Mathematics, Physics)
 * Priority: 1 = Low, 2 = Medium (default), 3 = High
 * Higher priority subjects get more revision time
 */
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val subjectCode: String = "", // e.g., "CSE301", "DBMS"
    val description: String = "",
    val examDate: Long? = null, // Exam date timestamp for prioritization
    val priority: Int = 2, // 1=Low, 2=Medium, 3=High
    val colorHex: String = "#4ECDC4", // Default teal color
    val createdAt: Long = System.currentTimeMillis()
) {
    val hasExamDate: Boolean get() = examDate != null
    
    fun daysUntilExam(): Int? {
        return examDate?.let {
            val diff = it - System.currentTimeMillis()
            (diff / (24 * 60 * 60 * 1000)).toInt()
        }
    }
}
