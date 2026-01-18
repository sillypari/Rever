package com.rever.app.data.repository

import com.rever.app.data.dao.TopicDao
import com.rever.app.data.model.Topic
import kotlinx.coroutines.flow.Flow

class TopicRepository(private val topicDao: TopicDao) {
    
    val allTopics: Flow<List<Topic>> = topicDao.getAllTopics()
    val totalTopicCount: Flow<Int> = topicDao.getTotalTopicCount()
    
    fun getTopicsBySubject(subjectId: Long): Flow<List<Topic>> {
        return topicDao.getTopicsBySubject(subjectId)
    }
    
    suspend fun getTopicsBySubjectSync(subjectId: Long): List<Topic> {
        return topicDao.getTopicsBySubjectSync(subjectId)
    }
    
    suspend fun getAllTopicsSync(): List<Topic> {
        return topicDao.getAllTopicsSync()
    }
    
    suspend fun getTopicById(id: Long): Topic? {
        return topicDao.getTopicById(id)
    }
    
    suspend fun insertTopic(topic: Topic): Long {
        return topicDao.insertTopic(topic)
    }
    
    suspend fun insertTopics(topics: List<Topic>): List<Long> {
        return topicDao.insertTopics(topics)
    }
    
    suspend fun updateTopic(topic: Topic) {
        topicDao.updateTopic(topic)
    }
    
    suspend fun deleteTopic(topic: Topic) {
        topicDao.deleteTopic(topic)
    }
    
    suspend fun deleteTopicById(id: Long) {
        topicDao.deleteTopicById(id)
    }
    
    suspend fun deleteTopicsBySubject(subjectId: Long) {
        topicDao.deleteTopicsBySubject(subjectId)
    }
    
    fun getTopicCountBySubject(subjectId: Long): Flow<Int> {
        return topicDao.getTopicCountBySubject(subjectId)
    }
    
    // Revision tracking methods
    suspend fun markTopicRevised(topicId: Long) {
        topicDao.markTopicRevised(topicId, System.currentTimeMillis())
    }
    
    suspend fun updateConfidenceLevel(topicId: Long, level: Int) {
        topicDao.updateConfidenceLevel(topicId, level.coerceIn(1, 5))
    }
    
    suspend fun getTopicsByTag(tag: String): List<Topic> {
        return topicDao.getTopicsByTag(tag)
    }
    
    suspend fun getTopicsNeedingRevision(limit: Int = 20): List<Topic> {
        return topicDao.getTopicsNeedingRevision(limit)
    }
    
    suspend fun getTopicsByDifficulty(difficulty: String): List<Topic> {
        return topicDao.getTopicsByDifficulty(difficulty)
    }
}
