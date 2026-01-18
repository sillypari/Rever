package com.rever.app.data.dao

import androidx.room.*
import com.rever.app.data.model.RevisionSession
import com.rever.app.data.model.RevisionSessionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RevisionSessionDao {
    
    @Query("""
        SELECT 
            rs.id as sessionId,
            rs.topicId,
            t.name as topicName,
            s.id as subjectId,
            s.name as subjectName,
            s.colorHex as subjectColor,
            rs.date,
            rs.durationMinutes,
            rs.isCompleted,
            rs.completionPercentage,
            t.difficulty,
            t.type as topicType,
            t.priority,
            t.tags,
            t.confidenceLevel,
            t.revisionCount
        FROM revision_sessions rs
        INNER JOIN topics t ON rs.topicId = t.id
        INNER JOIN subjects s ON t.subjectId = s.id
        WHERE rs.date = :date
        ORDER BY t.priority DESC, s.name, t.name
    """)
    fun getSessionsForDate(date: Long): Flow<List<RevisionSessionWithDetails>>
    
    @Query("""
        SELECT 
            rs.id as sessionId,
            rs.topicId,
            t.name as topicName,
            s.id as subjectId,
            s.name as subjectName,
            s.colorHex as subjectColor,
            rs.date,
            rs.durationMinutes,
            rs.isCompleted,
            rs.completionPercentage,
            t.difficulty,
            t.type as topicType,
            t.priority,
            t.tags,
            t.confidenceLevel,
            t.revisionCount
        FROM revision_sessions rs
        INNER JOIN topics t ON rs.topicId = t.id
        INNER JOIN subjects s ON t.subjectId = s.id
        WHERE rs.date >= :startDate AND rs.date <= :endDate
        ORDER BY rs.date, t.priority DESC, s.name, t.name
    """)
    fun getSessionsForDateRange(startDate: Long, endDate: Long): Flow<List<RevisionSessionWithDetails>>
    
    @Query("""
        SELECT 
            rs.id as sessionId,
            rs.topicId,
            t.name as topicName,
            s.id as subjectId,
            s.name as subjectName,
            s.colorHex as subjectColor,
            rs.date,
            rs.durationMinutes,
            rs.isCompleted,
            rs.completionPercentage,
            t.difficulty,
            t.type as topicType,
            t.priority,
            t.tags,
            t.confidenceLevel,
            t.revisionCount
        FROM revision_sessions rs
        INNER JOIN topics t ON rs.topicId = t.id
        INNER JOIN subjects s ON t.subjectId = s.id
        WHERE rs.date < :today AND rs.isCompleted = 0
        ORDER BY rs.date DESC, t.priority DESC
    """)
    fun getOverdueSessions(today: Long): Flow<List<RevisionSessionWithDetails>>
    
    @Query("SELECT * FROM revision_sessions WHERE date = :date")
    suspend fun getSessionsForDateSync(date: Long): List<RevisionSession>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RevisionSession): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<RevisionSession>): List<Long>
    
    @Update
    suspend fun updateSession(session: RevisionSession)
    
    @Query("""
        UPDATE revision_sessions 
        SET completionPercentage = :percentage,
            isCompleted = CASE WHEN :percentage >= 100 THEN 1 ELSE 0 END,
            completedAt = CASE WHEN :percentage >= 100 THEN :completedAt ELSE NULL END
        WHERE id = :sessionId
    """)
    suspend fun updateSessionCompletionPercentage(sessionId: Long, percentage: Int, completedAt: Long?)
    
    @Query("UPDATE revision_sessions SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :sessionId")
    suspend fun updateSessionCompletion(sessionId: Long, isCompleted: Boolean, completedAt: Long?)
    
    @Delete
    suspend fun deleteSession(session: RevisionSession)
    
    @Query("DELETE FROM revision_sessions")
    suspend fun deleteAllSessions()
    
    @Query("DELETE FROM revision_sessions WHERE topicId = :topicId")
    suspend fun deleteSessionsByTopic(topicId: Long)
    
    @Query("SELECT COUNT(*) FROM revision_sessions WHERE isCompleted = 1")
    fun getCompletedSessionCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM revision_sessions")
    fun getTotalSessionCount(): Flow<Int>
    
    @Query("""
        SELECT COUNT(DISTINCT topicId) FROM revision_sessions 
        WHERE isCompleted = 1
    """)
    fun getCompletedTopicCount(): Flow<Int>
    
    @Query("""
        SELECT COUNT(*) FROM revision_sessions 
        WHERE date = :today AND isCompleted = 0
    """)
    fun getPendingTodayCount(today: Long): Flow<Int>
    
    @Query("""
        SELECT COUNT(*) FROM revision_sessions 
        WHERE date < :today AND isCompleted = 0
    """)
    fun getOverdueCount(today: Long): Flow<Int>
    
    @Query("""
        SELECT SUM(durationMinutes) FROM revision_sessions 
        WHERE date = :date
    """)
    suspend fun getTotalScheduledTimeForDate(date: Long): Int?
}
