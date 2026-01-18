package com.rever.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rever.app.ReverApplication
import com.rever.app.data.model.RevisionSessionWithDetails
import com.rever.app.data.model.Subject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ProgressState(
    val subjects: List<Subject> = emptyList(),
    val subjectProgress: Map<Long, SubjectProgressData> = emptyMap(),
    val totalTopics: Int = 0,
    val completedTopics: Int = 0,
    val pendingToday: Int = 0,
    val overdueCount: Int = 0,
    val overdueSessions: List<RevisionSessionWithDetails> = emptyList(),
    val allSessions: List<RevisionSessionWithDetails> = emptyList(), // All sessions for timeline
    val weeklyProgress: List<DayProgress> = emptyList(), // Last 7 days
    val totalMinutesStudied: Int = 0,
    val currentStreak: Int = 0,
    val recentCompletedSessions: List<RevisionSessionWithDetails> = emptyList(),
    val isLoading: Boolean = false
)

data class DayProgress(
    val date: Long,
    val dayLabel: String,
    val completedMinutes: Int,
    val totalMinutes: Int,
    val topicsCompleted: Int
)

data class SubjectProgressData(
    val subjectId: Long,
    val subjectName: String,
    val totalTopics: Int,
    val completedTopics: Int
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as ReverApplication
    private val subjectRepository = app.subjectRepository
    private val topicRepository = app.topicRepository
    private val revisionSessionRepository = app.revisionSessionRepository
    
    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state.asStateFlow()
    
    init {
        loadProgressData()
        loadWeeklyProgress()
        loadRecentCompletedSessions()
        loadAllSessions()
    }
    
    private fun loadProgressData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Load subjects
            subjectRepository.allSubjects.collect { subjects ->
                _state.update { it.copy(subjects = subjects) }
                loadSubjectProgress(subjects)
            }
        }
        
        // Load total topic count
        viewModelScope.launch {
            topicRepository.totalTopicCount.collect { count ->
                _state.update { it.copy(totalTopics = count) }
            }
        }
        
        // Load completed topic count
        viewModelScope.launch {
            revisionSessionRepository.completedTopicCount.collect { count ->
                _state.update { it.copy(completedTopics = count) }
            }
        }
        
        // Load pending today count
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())
            revisionSessionRepository.getPendingTodayCount(today).collect { count ->
                _state.update { it.copy(pendingToday = count) }
            }
        }
        
        // Load overdue count
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())
            revisionSessionRepository.getOverdueCount(today).collect { count ->
                _state.update { it.copy(overdueCount = count) }
            }
        }
        
        // Load overdue sessions
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())
            revisionSessionRepository.getOverdueSessions(today).collect { sessions ->
                _state.update { it.copy(overdueSessions = sessions, isLoading = false) }
            }
        }
    }
    
    private fun loadSubjectProgress(subjects: List<Subject>) {
        viewModelScope.launch {
            val progressMap = mutableMapOf<Long, SubjectProgressData>()
            
            for (subject in subjects) {
                val topics = topicRepository.getTopicsBySubjectSync(subject.id)
                val totalTopics = topics.size
                
                // Count completed topics (topics that have at least one completed session)
                var completedTopics = 0
                for (topic in topics) {
                    // This is a simplified count - in a real app you might want to check
                    // if all sessions for a topic are completed
                    completedTopics++ // Placeholder - would need session data
                }
                
                progressMap[subject.id] = SubjectProgressData(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    totalTopics = totalTopics,
                    completedTopics = 0 // Will be updated based on sessions
                )
            }
            
            _state.update { it.copy(subjectProgress = progressMap) }
        }
    }
    
    fun toggleSessionCompletion(sessionId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            revisionSessionRepository.toggleSessionCompletion(sessionId, isCompleted)
        }
    }
    
    private fun loadWeeklyProgress() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val weeklyData = mutableListOf<DayProgress>()
            
            // Get last 7 days including today
            for (i in 6 downTo 0) {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val dayStart = getStartOfDay(calendar.timeInMillis)
                
                revisionSessionRepository.getSessionsForDate(dayStart).collect { sessions ->
                    val dayProgress = DayProgress(
                        date = dayStart,
                        dayLabel = if (i == 0) "Today" else dayFormat.format(calendar.time),
                        completedMinutes = sessions.filter { it.isCompleted }.sumOf { it.durationMinutes },
                        totalMinutes = sessions.sumOf { it.durationMinutes },
                        topicsCompleted = sessions.count { it.isCompleted }
                    )
                    
                    // Update weekly progress (this is simplified - real implementation would aggregate properly)
                    val currentWeekly = _state.value.weeklyProgress.toMutableList()
                    val existingIndex = currentWeekly.indexOfFirst { it.date == dayStart }
                    if (existingIndex >= 0) {
                        currentWeekly[existingIndex] = dayProgress
                    } else {
                        currentWeekly.add(dayProgress)
                    }
                    currentWeekly.sortBy { it.date }
                    
                    // Calculate total minutes studied
                    val totalMinutes = currentWeekly.sumOf { it.completedMinutes }
                    
                    // Calculate streak (consecutive days with completed topics)
                    var streak = 0
                    for (day in currentWeekly.reversed()) {
                        if (day.topicsCompleted > 0) {
                            streak++
                        } else {
                            break
                        }
                    }
                    
                    _state.update { 
                        it.copy(
                            weeklyProgress = currentWeekly.takeLast(7),
                            totalMinutesStudied = totalMinutes,
                            currentStreak = streak
                        ) 
                    }
                }
            }
        }
    }
    
    private fun loadRecentCompletedSessions() {
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())
            val weekAgo = today - (7 * 24 * 60 * 60 * 1000L)
            
            revisionSessionRepository.getSessionsForDateRange(weekAgo, today).collect { sessions ->
                val completed = sessions
                    .filter { it.isCompleted }
                    .sortedByDescending { it.date }
                    .take(10)
                
                _state.update { it.copy(recentCompletedSessions = completed) }
            }
        }
    }
    
    private fun loadAllSessions() {
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())
            // Load sessions from today onwards for the next 30 days
            val monthLater = today + (30 * 24 * 60 * 60 * 1000L)
            
            revisionSessionRepository.getSessionsForDateRange(today, monthLater).collect { sessions ->
                val sorted = sessions.sortedBy { it.date }
                _state.update { it.copy(allSessions = sorted) }
            }
        }
    }
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
