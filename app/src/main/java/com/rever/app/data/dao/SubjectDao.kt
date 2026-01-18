package com.rever.app.data.dao

import androidx.room.*
import com.rever.app.data.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    
    @Query("SELECT * FROM subjects ORDER BY createdAt DESC")
    fun getAllSubjects(): Flow<List<Subject>>
    
    @Query("SELECT * FROM subjects ORDER BY priority DESC, createdAt DESC")
    suspend fun getAllSubjectsSync(): List<Subject>
    
    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): Subject?
    
    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectByIdFlow(id: Long): Flow<Subject?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long
    
    @Update
    suspend fun updateSubject(subject: Subject)
    
    @Delete
    suspend fun deleteSubject(subject: Subject)
    
    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)
    
    @Query("SELECT COUNT(*) FROM subjects")
    fun getSubjectCount(): Flow<Int>
}
