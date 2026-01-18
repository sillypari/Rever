package com.rever.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rever.app.ReverApplication
import com.rever.app.data.model.Subject
import com.rever.app.data.model.Topic
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubjectsState(
    val subjects: List<Subject> = emptyList(),
    val topicCountBySubject: Map<Long, Int> = emptyMap(),
    val selectedSubject: Subject? = null,
    val topicsForSelectedSubject: List<Topic> = emptyList(),
    val isLoading: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val subjectToDelete: Subject? = null
)

class SubjectsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as ReverApplication
    private val subjectRepository = app.subjectRepository
    private val topicRepository = app.topicRepository
    
    private val _state = MutableStateFlow(SubjectsState())
    val state: StateFlow<SubjectsState> = _state.asStateFlow()
    
    init {
        loadSubjects()
        loadTopicCounts()
    }
    
    private fun loadSubjects() {
        viewModelScope.launch {
            subjectRepository.allSubjects.collect { subjects ->
                _state.update { it.copy(subjects = subjects) }
            }
        }
    }
    
    private fun loadTopicCounts() {
        viewModelScope.launch {
            topicRepository.allTopics.collect { topics ->
                val countMap = topics.groupBy { it.subjectId }.mapValues { it.value.size }
                _state.update { it.copy(topicCountBySubject = countMap) }
            }
        }
    }
    
    fun selectSubject(subject: Subject) {
        _state.update { it.copy(selectedSubject = subject) }
        loadTopicsForSubject(subject.id)
    }
    
    private fun loadTopicsForSubject(subjectId: Long) {
        viewModelScope.launch {
            topicRepository.getTopicsBySubject(subjectId).collect { topics ->
                _state.update { it.copy(topicsForSelectedSubject = topics) }
            }
        }
    }
    
    fun clearSelectedSubject() {
        _state.update { 
            it.copy(
                selectedSubject = null, 
                topicsForSelectedSubject = emptyList()
            ) 
        }
    }
    
    fun showDeleteConfirmation(subject: Subject) {
        _state.update { 
            it.copy(
                showDeleteDialog = true, 
                subjectToDelete = subject
            ) 
        }
    }
    
    fun dismissDeleteDialog() {
        _state.update { 
            it.copy(
                showDeleteDialog = false, 
                subjectToDelete = null
            ) 
        }
    }
    
    fun confirmDeleteSubject() {
        val subject = _state.value.subjectToDelete ?: return
        
        viewModelScope.launch {
            try {
                subjectRepository.deleteSubject(subject)
                _state.update { 
                    it.copy(
                        showDeleteDialog = false, 
                        subjectToDelete = null,
                        selectedSubject = if (it.selectedSubject?.id == subject.id) null else it.selectedSubject,
                        topicsForSelectedSubject = if (it.selectedSubject?.id == subject.id) emptyList() else it.topicsForSelectedSubject
                    ) 
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun addTopic(subjectId: Long, name: String, estimatedTime: Int = 30) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            try {
                val topic = Topic(
                    subjectId = subjectId,
                    name = name.trim(),
                    estimatedTimeMinutes = estimatedTime
                )
                topicRepository.insertTopic(topic)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteTopic(topic: Topic) {
        viewModelScope.launch {
            try {
                topicRepository.deleteTopic(topic)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun createSubject(name: String, description: String, onSuccess: (Long) -> Unit) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            try {
                val subject = Subject(
                    name = name.trim(),
                    description = description.trim()
                )
                val id = subjectRepository.insertSubject(subject)
                onSuccess(id)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
