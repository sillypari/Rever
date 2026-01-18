package com.rever.app.data.model

/**
 * Type of topic content - helps with scheduling and display
 */
enum class TopicType {
    THEORY,      // Reading/understanding concepts
    NUMERICAL,   // Solving numerical problems
    CODE,        // Programming/algorithm practice
    MCQ,         // Multiple choice questions practice
    PRACTICAL;   // Hands-on lab work
    
    companion object {
        fun fromString(str: String): TopicType {
            return when (str.lowercase().trim()) {
                "theory", "reading", "concept" -> THEORY
                "numerical", "numerical", "solve", "calculation" -> NUMERICAL
                "code", "coding", "programming", "algorithm" -> CODE
                "mcq", "quiz", "questions", "objective" -> MCQ
                "practical", "lab", "hands-on", "experiment" -> PRACTICAL
                else -> THEORY // Default
            }
        }
        
        /**
         * Auto-detect topic type from topic name keywords
         */
        fun detectFromName(topicName: String): TopicType {
            val name = topicName.lowercase()
            return when {
                name.contains("code") || name.contains("program") || 
                name.contains("algorithm") || name.contains("implement") ||
                name.contains("tree") || name.contains("graph") ||
                name.contains("sort") || name.contains("search") ||
                name.contains("array") || name.contains("linked list") ||
                name.contains("stack") || name.contains("queue") -> CODE
                
                name.contains("solve") || name.contains("calculate") ||
                name.contains("numerical") || name.contains("formula") ||
                name.contains("equation") || name.contains("problem") -> NUMERICAL
                
                name.contains("mcq") || name.contains("quiz") ||
                name.contains("objective") || name.contains("questions") -> MCQ
                
                name.contains("lab") || name.contains("practical") ||
                name.contains("experiment") || name.contains("hands-on") -> PRACTICAL
                
                else -> THEORY
            }
        }
    }
    
    fun displayName(): String {
        return when (this) {
            THEORY -> "Theory"
            NUMERICAL -> "Numerical"
            CODE -> "Code"
            MCQ -> "MCQ"
            PRACTICAL -> "Practical"
        }
    }
}
