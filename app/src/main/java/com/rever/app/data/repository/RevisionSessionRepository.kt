package com.rever.app.data.repository

import com.rever.app.data.dao.RevisionSessionDao
import com.rever.app.data.model.RevisionSession
import com.rever.app.data.model.RevisionSessionWithDetails
import kotlinx.coroutines.flow.Flow

class RevisionSessionRepository(private val revisionSessionDao: RevisionSessionDao) {
    
    val completedSessionCount: Flow<Int> = revisionSessionDao.getCompletedSessionCount()
    val totalSessionCount: Flow<Int> = revisionSessionDao.getTotalSessionCount()
    val completedTopicCount: Flow<Int> = revisionSessionDao.getCompletedTopicCount()
    
    fun getSessionsForDate(date: Long): Flow<List<RevisionSessionWithDetails>> {
        return revisionSessionDao.getSessionsForDate(date)
    }
    
    fun getSessionsForDateRange(startDate: Long, endDate: Long): Flow<List<RevisionSessionWithDetails>> {
        return revisionSessionDao.getSessionsForDateRange(startDate, endDate)
    }
    
    fun getOverdueSessions(today: Long): Flow<List<RevisionSessionWithDetails>> {
        return revisionSessionDao.getOverdueSessions(today)
    }
    
    fun getPendingTodayCount(today: Long): Flow<Int> {
        return revisionSessionDao.getPendingTodayCount(today)
    }
    
    fun getOverdueCount(today: Long): Flow<Int> {
        return revisionSessionDao.getOverdueCount(today)
    }
    
    suspend fun getSessionsForDateSync(date: Long): List<RevisionSession> {
        return revisionSessionDao.getSessionsForDateSync(date)
    }
    
    suspend fun insertSession(session: RevisionSession): Long {
        return revisionSessionDao.insertSession(session)
    }
    
    suspend fun insertSessions(sessions: List<RevisionSession>): List<Long> {
        return revisionSessionDao.insertSessions(sessions)
    }
    
    suspend fun updateSession(session: RevisionSession) {
        revisionSessionDao.updateSession(session)
    }
    
    suspend fun toggleSessionCompletion(sessionId: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        revisionSessionDao.updateSessionCompletion(sessionId, isCompleted, completedAt)
    }
    
    suspend fun updateCompletionPercentage(sessionId: Long, percentage: Int) {
        val completedAt = if (percentage >= 100) System.currentTimeMillis() else null
        revisionSessionDao.updateSessionCompletionPercentage(sessionId, percentage.coerceIn(0, 100), completedAt)
    }
    
    suspend fun deleteSession(session: RevisionSession) {
        revisionSessionDao.deleteSession(session)
    }
    
    suspend fun deleteAllSessions() {
        revisionSessionDao.deleteAllSessions()
    }
    
    suspend fun deleteSessionsByTopic(topicId: Long) {
        revisionSessionDao.deleteSessionsByTopic(topicId)
    }
    
    suspend fun getTotalScheduledTimeForDate(date: Long): Int {
        return revisionSessionDao.getTotalScheduledTimeForDate(date) ?: 0
    }
}
