package com.rever.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rever.app.ReverApplication
import com.rever.app.data.model.Subject
import com.rever.app.data.model.Topic
import com.rever.app.data.model.Difficulty
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OnboardingState(
    val currentStep: Int = 0,
    val subjectName: String = "",
    val subjectDescription: String = "",
    val createdSubjectId: Long? = null,
    val topics: List<TopicInput> = emptyList(),
    val bulkTopicsInput: String = "",
    val dailyTimeMinutes: Int = 60,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class TopicInput(
    val name: String = "",
    val estimatedTimeMinutes: Int = 30,
    val difficulty: Difficulty = Difficulty.MEDIUM
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as ReverApplication
    private val subjectRepository = app.subjectRepository
    private val topicRepository = app.topicRepository
    private val userPreferencesRepository = app.userPreferencesRepository
    
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    
    val onboardingCompleted = userPreferencesRepository.onboardingCompleted
    
    fun updateSubjectName(name: String) {
        _state.update { it.copy(subjectName = name, errorMessage = null) }
    }
    
    fun updateSubjectDescription(description: String) {
        _state.update { it.copy(subjectDescription = description) }
    }
    
    fun createSubject(onSuccess: (Long) -> Unit) {
        val currentState = _state.value
        
        if (currentState.subjectName.isBlank()) {
            _state.update { it.copy(errorMessage = "Subject name is required") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val subject = Subject(
                    name = currentState.subjectName.trim(),
                    description = currentState.subjectDescription.trim()
                )
                val subjectId = subjectRepository.insertSubject(subject)
                _state.update { 
                    it.copy(
                        createdSubjectId = subjectId,
                        isLoading = false,
                        currentStep = 1
                    ) 
                }
                onSuccess(subjectId)
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Failed to create subject"
                    ) 
                }
            }
        }
    }
    
    fun addTopic(name: String, estimatedTime: Int = 30) {
        if (name.isBlank()) return
        
        val newTopic = TopicInput(
            name = name.trim(),
            estimatedTimeMinutes = estimatedTime
        )
        _state.update { 
            it.copy(topics = it.topics + newTopic, errorMessage = null) 
        }
    }
    
    fun removeTopic(index: Int) {
        _state.update { 
            it.copy(topics = it.topics.filterIndexed { i, _ -> i != index }) 
        }
    }
    
    fun updateBulkTopicsInput(input: String) {
        _state.update { it.copy(bulkTopicsInput = input) }
    }
    
    fun importBulkTopics() {
        val lines = _state.value.bulkTopicsInput
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        val newTopics = lines.map { TopicInput(name = it) }
        _state.update { 
            it.copy(
                topics = it.topics + newTopics,
                bulkTopicsInput = ""
            ) 
        }
    }
    
    fun saveTopics(subjectId: Long, onSuccess: () -> Unit) {
        val currentState = _state.value
        
        if (currentState.topics.isEmpty()) {
            _state.update { it.copy(errorMessage = "Add at least one topic") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val topics = currentState.topics.map { input ->
                    Topic(
                        subjectId = subjectId,
                        name = input.name,
                        estimatedTimeMinutes = input.estimatedTimeMinutes,
                        difficulty = input.difficulty
                    )
                }
                topicRepository.insertTopics(topics)
                _state.update { 
                    it.copy(isLoading = false, currentStep = 2) 
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Failed to save topics"
                    ) 
                }
            }
        }
    }
    
    fun updateDailyTime(minutes: Int) {
        _state.update { it.copy(dailyTimeMinutes = minutes) }
    }
    
    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                userPreferencesRepository.setDailyRevisionTime(_state.value.dailyTimeMinutes)
                userPreferencesRepository.setOnboardingCompleted(true)
                _state.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Failed to save preferences"
                    ) 
                }
            }
        }
    }
    
    fun resetState() {
        _state.value = OnboardingState()
    }
}
