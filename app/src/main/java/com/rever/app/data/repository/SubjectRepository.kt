package com.rever.app.data.repository

import com.rever.app.data.dao.SubjectDao
import com.rever.app.data.model.Subject
import kotlinx.coroutines.flow.Flow

class SubjectRepository(private val subjectDao: SubjectDao) {
    
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()
    val subjectCount: Flow<Int> = subjectDao.getSubjectCount()
    
    suspend fun getAllSubjectsSync(): List<Subject> {
        return subjectDao.getAllSubjectsSync()
    }
    
    suspend fun getSubjectById(id: Long): Subject? {
        return subjectDao.getSubjectById(id)
    }
    
    fun getSubjectByIdFlow(id: Long): Flow<Subject?> {
        return subjectDao.getSubjectByIdFlow(id)
    }
    
    suspend fun insertSubject(subject: Subject): Long {
        return subjectDao.insertSubject(subject)
    }
    
    suspend fun updateSubject(subject: Subject) {
        subjectDao.updateSubject(subject)
    }
    
    suspend fun deleteSubject(subject: Subject) {
        subjectDao.deleteSubject(subject)
    }
    
    suspend fun deleteSubjectById(id: Long) {
        subjectDao.deleteSubjectById(id)
    }
}
