package com.rever.app.data.dao

import androidx.room.*
import com.rever.app.data.model.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY priority DESC, createdAt ASC")
    fun getTopicsBySubject(subjectId: Long): Flow<List<Topic>>
    
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY priority DESC, createdAt ASC")
    suspend fun getTopicsBySubjectSync(subjectId: Long): List<Topic>
    
    @Query("SELECT * FROM topics ORDER BY priority DESC, createdAt ASC")
    fun getAllTopics(): Flow<List<Topic>>
    
    @Query("SELECT * FROM topics ORDER BY priority DESC, createdAt ASC")
    suspend fun getAllTopicsSync(): List<Topic>
    
    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getTopicById(id: Long): Topic?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<Topic>): List<Long>
    
    @Update
    suspend fun updateTopic(topic: Topic)
    
    @Delete
    suspend fun deleteTopic(topic: Topic)
    
    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)
    
    @Query("DELETE FROM topics WHERE subjectId = :subjectId")
    suspend fun deleteTopicsBySubject(subjectId: Long)
    
    @Query("SELECT COUNT(*) FROM topics WHERE subjectId = :subjectId")
    fun getTopicCountBySubject(subjectId: Long): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM topics")
    fun getTotalTopicCount(): Flow<Int>
    
    // Update revision tracking fields
    @Query("""
        UPDATE topics 
        SET lastRevisedDate = :date, 
            revisionCount = revisionCount + 1 
        WHERE id = :topicId
    """)
    suspend fun markTopicRevised(topicId: Long, date: Long)
    
    @Query("UPDATE topics SET confidenceLevel = :level WHERE id = :topicId")
    suspend fun updateConfidenceLevel(topicId: Long, level: Int)
    
    // Get topics by tag
    @Query("SELECT * FROM topics WHERE tags LIKE '%' || :tag || '%' ORDER BY priority DESC")
    suspend fun getTopicsByTag(tag: String): List<Topic>
    
    // Get topics needing revision (based on spaced repetition)
    @Query("""
        SELECT * FROM topics 
        WHERE lastRevisedDate IS NOT NULL 
        ORDER BY lastRevisedDate ASC 
        LIMIT :limit
    """)
    suspend fun getTopicsNeedingRevision(limit: Int = 20): List<Topic>
    
    // Get topics by difficulty
    @Query("SELECT * FROM topics WHERE difficulty = :difficulty ORDER BY priority DESC")
    suspend fun getTopicsByDifficulty(difficulty: String): List<Topic>
}
