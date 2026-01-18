package com.rever.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rever.app.ReverApplication
import com.rever.app.data.model.Difficulty
import com.rever.app.data.model.RevisionSession
import com.rever.app.data.model.RevisionSessionWithDetails
import com.rever.app.data.model.Subject
import com.rever.app.data.model.SubjectWithSessions
import com.rever.app.data.model.Topic
import com.rever.app.data.model.TopicType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

// Data class for sessions grouped by date for infinite scroll
data class DaySessionGroup(
    val date: Long,
    val sessions: List<RevisionSessionWithDetails>,
    val groupedBySubject: List<SubjectWithSessions>,
    val totalMinutes: Int,
    val completedMinutes: Int
)

data class PlanState(
    val selectedDate: Long = getStartOfDay(System.currentTimeMillis()),
    val weekDates: List<Long> = generateWeekDates(),
    val sessionsForSelectedDate: List<RevisionSessionWithDetails> = emptyList(),
    val groupedSessions: List<SubjectWithSessions> = emptyList(),
    val monthSessionsByDate: Map<Long, List<String>> = emptyMap(), // date -> list of subject colors
    // Infinite scroll data
    val infiniteScrollDays: List<DaySessionGroup> = emptyList(),
    val loadedDaysCount: Int = 7, // Number of days loaded
    val isLoadingMore: Boolean = false,
    val totalMinutesForDay: Int = 0,
    val completedMinutesForDay: Int = 0,
    val dailyTime: Int = 60,
    val isLoading: Boolean = false,
    val isPlanGenerated: Boolean = false,
    val errorMessage: String? = null,
    val showImportDialog: Boolean = false,
    val showDailyTimeDialog: Boolean = false,
    val importSuccess: Boolean = false,
    val importedCount: Int = 0
)

class PlanViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as ReverApplication
    private val revisionSessionRepository = app.revisionSessionRepository
    private val topicRepository = app.topicRepository
    private val subjectRepository = app.subjectRepository
    private val userPreferencesRepository = app.userPreferencesRepository
    
    private val _state = MutableStateFlow(PlanState())
    val state: StateFlow<PlanState> = _state.asStateFlow()
    
    val dailyRevisionTime = userPreferencesRepository.dailyRevisionTime
    val planGenerated = userPreferencesRepository.planGenerated
    
    init {
        loadSessionsForDate(_state.value.selectedDate)
        loadMonthSessions(_state.value.selectedDate)
        loadInfiniteScrollDays(_state.value.selectedDate, 7) // Load first 7 days
        
        viewModelScope.launch {
            planGenerated.collect { generated ->
                _state.update { it.copy(isPlanGenerated = generated) }
            }
        }
        
        viewModelScope.launch {
            dailyRevisionTime.collect { time ->
                _state.update { it.copy(dailyTime = time) }
            }
        }
    }
    
    fun selectDate(date: Long) {
        val oldMonth = Calendar.getInstance().apply { timeInMillis = _state.value.selectedDate }.get(Calendar.MONTH)
        val newMonth = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.MONTH)
        
        _state.update { it.copy(selectedDate = date) }
        loadSessionsForDate(date)
        loadInfiniteScrollDays(date, 7) // Refresh infinite scroll from selected date
        
        // Reload month sessions if month changed
        if (oldMonth != newMonth) {
            loadMonthSessions(date)
        }
    }
    
    private fun loadMonthSessions(date: Long) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val endOfMonth = calendar.timeInMillis
            
            revisionSessionRepository.getSessionsForDateRange(startOfMonth, endOfMonth).collect { sessions ->
                // Group by date, then get unique subject colors for each date
                val sessionsByDate = sessions.groupBy { it.date }.mapValues { (_, dateSessions) ->
                    dateSessions.map { it.subjectColor }.distinct().take(4) // Max 4 colors
                }
                
                _state.update { it.copy(monthSessionsByDate = sessionsByDate) }
            }
        }
    }
    
    private fun loadSessionsForDate(date: Long) {
        viewModelScope.launch {
            revisionSessionRepository.getSessionsForDate(date).collect { sessions ->
                // Group sessions by subject
                val grouped = sessions.groupBy { it.subjectId }.map { (subjectId, subjectSessions) ->
                    val first = subjectSessions.first()
                    SubjectWithSessions(
                        subjectId = subjectId,
                        subjectName = first.subjectName,
                        colorHex = first.subjectColor,
                        sessions = subjectSessions,
                        totalMinutes = subjectSessions.sumOf { it.durationMinutes },
                        completedMinutes = subjectSessions.filter { it.isCompleted }.sumOf { it.durationMinutes }
                    )
                }
                
                val totalMinutes = sessions.sumOf { it.durationMinutes }
                val completedMinutes = sessions.filter { it.isCompleted }.sumOf { it.durationMinutes }
                
                _state.update { 
                    it.copy(
                        sessionsForSelectedDate = sessions,
                        groupedSessions = grouped,
                        totalMinutesForDay = totalMinutes,
                        completedMinutesForDay = completedMinutes
                    ) 
                }
            }
        }
    }
    
    /**
     * Load sessions for multiple days for infinite scroll
     */
    private fun loadInfiniteScrollDays(startDate: Long, daysToLoad: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            
            val calendar = Calendar.getInstance().apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val startOfRange = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, daysToLoad)
            val endOfRange = calendar.timeInMillis
            
            revisionSessionRepository.getSessionsForDateRange(startOfRange, endOfRange).collect { allSessions ->
                // Group sessions by date
                val dayGroups = mutableListOf<DaySessionGroup>()
                
                // Create entries for each day (even empty ones)
                val currentCal = Calendar.getInstance().apply {
                    timeInMillis = startOfRange
                }
                
                for (i in 0 until daysToLoad) {
                    val dayStart = currentCal.timeInMillis
                    val daySessions = allSessions.filter { it.date == dayStart }
                    
                    val groupedBySubject = daySessions.groupBy { it.subjectId }.map { (subjectId, subjectSessions) ->
                        val first = subjectSessions.first()
                        SubjectWithSessions(
                            subjectId = subjectId,
                            subjectName = first.subjectName,
                            colorHex = first.subjectColor,
                            sessions = subjectSessions,
                            totalMinutes = subjectSessions.sumOf { it.durationMinutes },
                            completedMinutes = subjectSessions.filter { it.isCompleted }.sumOf { it.durationMinutes }
                        )
                    }
                    
                    dayGroups.add(
                        DaySessionGroup(
                            date = dayStart,
                            sessions = daySessions,
                            groupedBySubject = groupedBySubject,
                            totalMinutes = daySessions.sumOf { it.durationMinutes },
                            completedMinutes = daySessions.filter { it.isCompleted }.sumOf { it.durationMinutes }
                        )
                    )
                    
                    currentCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                _state.update { 
                    it.copy(
                        infiniteScrollDays = dayGroups,
                        loadedDaysCount = daysToLoad,
                        isLoadingMore = false
                    ) 
                }
            }
        }
    }
    
    /**
     * Load more days for infinite scroll
     */
    fun loadMoreDays() {
        val currentState = _state.value
        if (currentState.isLoadingMore) return
        
        val newDaysCount = currentState.loadedDaysCount + 7
        loadInfiniteScrollDays(currentState.selectedDate, newDaysCount)
    }
    
    /**
     * Refresh infinite scroll from a new start date
     */
    fun refreshInfiniteScroll(startDate: Long) {
        loadInfiniteScrollDays(startDate, 7)
    }
    
    fun showDailyTimeDialog() {
        _state.update { it.copy(showDailyTimeDialog = true) }
    }
    
    fun hideDailyTimeDialog() {
        _state.update { it.copy(showDailyTimeDialog = false) }
    }
    
    fun updateDailyTime(minutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setDailyRevisionTime(minutes)
            _state.update { it.copy(dailyTime = minutes, showDailyTimeDialog = false) }
        }
    }
    
    fun toggleSessionCompletion(sessionId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            revisionSessionRepository.toggleSessionCompletion(sessionId, isCompleted)
        }
    }
    
    /**
     * Complete a session with confidence rating.
     * This marks the session complete, updates the topic's confidence level,
     * and records the revision for spaced repetition tracking.
     */
    fun completeSessionWithConfidence(sessionId: Long, topicId: Long, confidence: Int) {
        viewModelScope.launch {
            // Mark session as completed (100%)
            revisionSessionRepository.updateCompletionPercentage(sessionId, 100)
            // Update topic's confidence level and mark as revised
            topicRepository.updateConfidenceLevel(topicId, confidence)
            topicRepository.markTopicRevised(topicId)
        }
    }
    
    /**
     * Update the completion percentage for a session.
     */
    fun updateCompletionPercentage(sessionId: Long, percentage: Int) {
        viewModelScope.launch {
            revisionSessionRepository.updateCompletionPercentage(sessionId, percentage)
        }
    }
    
    fun generatePlan() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Get all subjects and their topics
                val allSubjects = subjectRepository.getAllSubjectsSync()
                val allTopics = topicRepository.getAllTopicsSync()
                
                if (allTopics.isEmpty()) {
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = "No topics found. Add subjects and topics first, or import a list."
                        ) 
                    }
                    return@launch
                }
                
                // Group topics by subject
                val topicsBySubject = allTopics.groupBy { it.subjectId }
                
                // Clear existing sessions
                revisionSessionRepository.deleteAllSessions()
                
                // Get daily time
                val dailyMinutes = dailyRevisionTime.first()
                
                val sessions = distributTopicsAcrossDays(topicsBySubject, allSubjects, dailyMinutes)
                if (sessions.isNotEmpty()) {
                    revisionSessionRepository.insertSessions(sessions)
                }
                
                userPreferencesRepository.setPlanGenerated(true)
                _state.update { it.copy(isLoading = false, isPlanGenerated = true, errorMessage = null) }
                
                // Reload current date sessions
                loadSessionsForDate(_state.value.selectedDate)
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Failed to generate plan: ${e.message ?: "Unknown error"}"
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
    
    fun showImportDialog() {
        _state.update { it.copy(showImportDialog = true) }
    }
    
    fun hideImportDialog() {
        _state.update { it.copy(showImportDialog = false) }
    }
    
    fun clearImportSuccess() {
        _state.update { it.copy(importSuccess = false, importedCount = 0) }
    }
    
    /**
     * Import topics from a file (JSON or plain text format)
     * Supported formats:
     * 1. JSON: { "subjects": [{ "name": "Math", "topics": [{"name": "Algebra", "time": 30}] }] }
     * 2. Plain text: Subject: Topic1, Topic2, Topic3 (one subject per line)
     */
    fun importFromFile(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, showImportDialog = false) }
            
            try {
                val contentResolver = app.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot open file")
                
                val content = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
                inputStream.close()
                
                var importedTopics = 0
                
                val trimmedContent = content.trim()
                
                // Detect format and parse accordingly
                importedTopics = when {
                    // JSON format
                    trimmedContent.startsWith("{") || trimmedContent.startsWith("[") -> 
                        parseJsonFormat(content)
                    
                    // CSV format (has header row with commas)
                    trimmedContent.lines().firstOrNull()?.contains(",") == true &&
                    (trimmedContent.lowercase().contains("subject,") || 
                     trimmedContent.lowercase().contains("topic,") ||
                     trimmedContent.lowercase().contains(",duration")) ->
                        parseCsvFormat(content)
                    
                    // Plain text format
                    else -> parsePlainTextFormat(content)
                }
                
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        importSuccess = true, 
                        importedCount = importedTopics,
                        errorMessage = null
                    ) 
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Import failed: ${e.message ?: "Unknown error"}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Parse CSV format:
     * Subject,Topic,Duration,Difficulty,Type,Priority
     * DBMS,Normalization,20,Medium,Theory,High
     */
    private suspend fun parseCsvFormat(content: String): Int {
        var topicCount = 0
        val lines = content.lines().filter { it.isNotBlank() }
        
        if (lines.isEmpty()) return 0
        
        // Parse header to find column indices
        val header = lines.first().split(",").map { it.trim().lowercase() }
        val subjectIdx = header.indexOfFirst { it.contains("subject") }
        val topicIdx = header.indexOfFirst { it.contains("topic") || it.contains("name") }
        val durationIdx = header.indexOfFirst { it.contains("duration") || it.contains("time") }
        val difficultyIdx = header.indexOfFirst { it.contains("difficulty") }
        val typeIdx = header.indexOfFirst { it.contains("type") }
        val priorityIdx = header.indexOfFirst { it.contains("priority") }
        val tagsIdx = header.indexOfFirst { it.contains("tag") }
        
        if (topicIdx == -1) {
            throw Exception("CSV must have a 'Topic' or 'Name' column")
        }
        
        // Group by subject
        val subjectMap = mutableMapOf<String, MutableList<Topic>>()
        
        for (i in 1 until lines.size) {
            val values = parseCsvLine(lines[i])
            if (values.isEmpty()) continue
            
            val subjectName = if (subjectIdx >= 0 && subjectIdx < values.size) 
                values[subjectIdx].trim() else "General"
            val topicName = if (topicIdx >= 0 && topicIdx < values.size) 
                values[topicIdx].trim() else continue
            
            if (topicName.isBlank()) continue
            
            val duration = if (durationIdx >= 0 && durationIdx < values.size) 
                values[durationIdx].filter { it.isDigit() }.toIntOrNull() ?: 20 else 20
            val difficulty = if (difficultyIdx >= 0 && difficultyIdx < values.size) 
                Difficulty.fromString(values[difficultyIdx]) else Difficulty.MEDIUM
            val type = if (typeIdx >= 0 && typeIdx < values.size) 
                TopicType.fromString(values[typeIdx]) else TopicType.detectFromName(topicName)
            val priority = if (priorityIdx >= 0 && priorityIdx < values.size) 
                parsePriority(values[priorityIdx]) else 2
            val tags = if (tagsIdx >= 0 && tagsIdx < values.size) 
                values[tagsIdx].trim() else ""
            
            if (!subjectMap.containsKey(subjectName)) {
                subjectMap[subjectName] = mutableListOf()
            }
            
            subjectMap[subjectName]!!.add(Topic(
                subjectId = 0, // Will be set when inserting
                name = topicName,
                estimatedTimeMinutes = duration.coerceIn(5, 120),
                difficulty = difficulty,
                type = type,
                priority = priority,
                tags = tags
            ))
            topicCount++
        }
        
        // Insert subjects and topics
        for ((subjectName, topics) in subjectMap) {
            val subject = Subject(
                name = subjectName,
                colorHex = generateRandomColor()
            )
            val subjectId = subjectRepository.insertSubject(subject)
            
            val topicsWithSubject = topics.map { it.copy(subjectId = subjectId) }
            topicRepository.insertTopics(topicsWithSubject)
        }
        
        return topicCount
    }
    
    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        values.add(current.toString())
        
        return values
    }
    
    private suspend fun parseJsonFormat(content: String): Int {
        var topicCount = 0
        
        try {
            val json = JSONObject(content)
            
            // Check for "subjects" array or single subject
            if (json.has("subjects")) {
                val subjectsArray = json.getJSONArray("subjects")
                
                for (i in 0 until subjectsArray.length()) {
                    val subjectObj = subjectsArray.getJSONObject(i)
                    topicCount += parseSubjectFromJson(subjectObj)
                }
            } else if (json.has("subject") || json.has("name")) {
                // Single subject format
                topicCount = parseSubjectFromJson(json)
            } else if (json.has("topics")) {
                // Topics only, create default subject
                val subject = Subject(name = "Imported Topics", colorHex = generateRandomColor())
                val subjectId = subjectRepository.insertSubject(subject)
                topicCount = parseTopicsFromJson(json.getJSONArray("topics"), subjectId)
            }
        } catch (e: Exception) {
            // Try parsing as array
            try {
                val jsonArray = JSONArray(content)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.get(i)
                    if (item is JSONObject) {
                        topicCount += parseSubjectFromJson(item)
                    }
                }
            } catch (e2: Exception) {
                throw Exception("Invalid JSON format: ${e.message}")
            }
        }
        
        return topicCount
    }
    
    private suspend fun parseSubjectFromJson(subjectObj: JSONObject): Int {
        val subjectName = subjectObj.optString("name", 
            subjectObj.optString("subject", "Subject"))
        val subjectCode = subjectObj.optString("code", 
            subjectObj.optString("subject_code", ""))
        val description = subjectObj.optString("description", "")
        val examDateStr = subjectObj.optString("exam_date", 
            subjectObj.optString("examDate", ""))
        
        val examDate = parseExamDate(examDateStr)
        
        val subject = Subject(
            name = subjectName,
            subjectCode = subjectCode,
            description = description,
            examDate = examDate,
            colorHex = generateRandomColor()
        )
        val subjectId = subjectRepository.insertSubject(subject)
        
        return if (subjectObj.has("topics")) {
            parseTopicsFromJson(subjectObj.getJSONArray("topics"), subjectId)
        } else 0
    }
    
    private suspend fun parseTopicsFromJson(topicsArray: JSONArray, subjectId: Long): Int {
        val topics = mutableListOf<Topic>()
        
        for (j in 0 until topicsArray.length()) {
            val topicItem = topicsArray.get(j)
            
            val topic = when (topicItem) {
                is JSONObject -> {
                    val name = topicItem.optString("name", "Topic ${j + 1}")
                    val duration = topicItem.optInt("duration", 
                        topicItem.optInt("time", 20))
                    val difficulty = Difficulty.fromString(
                        topicItem.optString("difficulty", "medium"))
                    val type = TopicType.fromString(
                        topicItem.optString("type", "theory"))
                    val priority = parsePriority(
                        topicItem.optString("priority", "medium"))
                    val subtopics = topicItem.optInt("subtopics", 
                        topicItem.optInt("subtopics_count", 0))
                    val prerequisites = topicItem.optString("prerequisites", "")
                    val tags = parseTagsFromJson(topicItem)
                    val notes = topicItem.optString("notes", "")
                    
                    Topic(
                        subjectId = subjectId,
                        name = name,
                        estimatedTimeMinutes = duration.coerceIn(5, 120),
                        difficulty = difficulty,
                        type = type,
                        priority = priority,
                        subtopicsCount = subtopics,
                        prerequisites = prerequisites,
                        tags = tags,
                        notes = notes
                    )
                }
                is String -> {
                    // Simple string topic name
                    Topic(
                        subjectId = subjectId,
                        name = topicItem,
                        estimatedTimeMinutes = 20,
                        difficulty = Difficulty.MEDIUM,
                        type = TopicType.detectFromName(topicItem)
                    )
                }
                else -> null
            }
            
            topic?.let { topics.add(it) }
        }
        
        if (topics.isNotEmpty()) {
            topicRepository.insertTopics(topics)
        }
        
        return topics.size
    }
    
    private fun parseTagsFromJson(obj: JSONObject): String {
        return when {
            obj.has("tags") -> {
                val tagsValue = obj.get("tags")
                when (tagsValue) {
                    is JSONArray -> {
                        (0 until tagsValue.length())
                            .mapNotNull { tagsValue.optString(it) }
                            .joinToString(",")
                    }
                    is String -> tagsValue
                    else -> ""
                }
            }
            else -> ""
        }
    }
    
    private fun parseExamDate(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        
        val formats = listOf(
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()),
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()),
            java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()),
            java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        )
        
        for (format in formats) {
            try {
                return format.parse(dateStr)?.time
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }
    
    /**
     * Parse enhanced plain text format:
     * Subject: DBMS
     * Exam: 2026-02-15
     * 
     * # Theory Topics
     * Normalization Forms | 20min | Medium | High
     * ACID Properties | 15min | Easy | Medium
     */
    private suspend fun parsePlainTextFormat(content: String): Int {
        var topicCount = 0
        val lines = content.lines()
        
        var currentSubject: Subject? = null
        var currentSubjectId: Long? = null
        var currentExamDate: Long? = null
        val pendingTopics = mutableListOf<Topic>()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue
            
            // Check for subject declaration
            if (trimmed.lowercase().startsWith("subject:")) {
                // Save pending topics before switching subject
                if (currentSubjectId != null && pendingTopics.isNotEmpty()) {
                    topicRepository.insertTopics(pendingTopics)
                    pendingTopics.clear()
                }
                
                val subjectName = trimmed.substringAfter(":").trim()
                if (subjectName.isNotBlank()) {
                    currentSubject = Subject(
                        name = subjectName,
                        examDate = currentExamDate,
                        colorHex = generateRandomColor()
                    )
                    currentSubjectId = subjectRepository.insertSubject(currentSubject)
                    currentExamDate = null
                }
                continue
            }
            
            // Check for exam date
            if (trimmed.lowercase().startsWith("exam:") || 
                trimmed.lowercase().startsWith("exam date:")) {
                val dateStr = trimmed.substringAfter(":").trim()
                currentExamDate = parseExamDate(dateStr)
                
                // Update subject if already created
                val subjectIdToUpdate = currentSubjectId
                if (subjectIdToUpdate != null && currentExamDate != null) {
                    val updatedSubject = currentSubject?.copy(examDate = currentExamDate)
                    updatedSubject?.let { subjectRepository.updateSubject(it.copy(id = subjectIdToUpdate)) }
                }
                continue
            }
            
            // Check for inline subject:topics format
            val colonIndex = trimmed.indexOf(":")
            if (colonIndex > 0 && !trimmed.contains("|") && 
                !trimmed.lowercase().startsWith("subject") &&
                !trimmed.lowercase().startsWith("exam")) {
                
                // Save pending topics
                if (currentSubjectId != null && pendingTopics.isNotEmpty()) {
                    topicRepository.insertTopics(pendingTopics)
                    pendingTopics.clear()
                }
                
                val subjectName = trimmed.substring(0, colonIndex).trim()
                val topicsStr = trimmed.substring(colonIndex + 1).trim()
                
                val subject = Subject(name = subjectName, colorHex = generateRandomColor())
                currentSubjectId = subjectRepository.insertSubject(subject)
                
                // Parse topics
                val topicNames = topicsStr.split(Regex("[,;|]"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                
                for (topicName in topicNames) {
                    pendingTopics.add(createSmartTopic(currentSubjectId, topicName))
                    topicCount++
                }
                continue
            }
            
            // Parse topic line with | separator
            if (trimmed.contains("|") && currentSubjectId != null) {
                val parts = trimmed.split("|").map { it.trim() }
                val topicName = parts.getOrNull(0) ?: continue
                if (topicName.isBlank()) continue
                
                val duration = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 20
                val difficulty = parts.getOrNull(2)?.let { Difficulty.fromString(it) } ?: Difficulty.MEDIUM
                val priority = parts.getOrNull(3)?.let { parsePriority(it) } ?: 2
                
                pendingTopics.add(Topic(
                    subjectId = currentSubjectId,
                    name = topicName,
                    estimatedTimeMinutes = duration.coerceIn(5, 120),
                    difficulty = difficulty,
                    type = TopicType.detectFromName(topicName),
                    priority = priority
                ))
                topicCount++
                continue
            }
            
            // Simple topic name (one per line under a subject)
            if (currentSubjectId != null && trimmed.isNotBlank()) {
                pendingTopics.add(createSmartTopic(currentSubjectId, trimmed))
                topicCount++
            }
        }
        
        // Insert remaining topics
        if (currentSubjectId != null && pendingTopics.isNotEmpty()) {
            topicRepository.insertTopics(pendingTopics)
        }
        
        return topicCount
    }
    
    /**
     * Create a topic with smart defaults based on name analysis
     */
    private fun createSmartTopic(subjectId: Long, name: String): Topic {
        val type = TopicType.detectFromName(name)
        val nameLower = name.lowercase()
        
        // Smart difficulty detection
        val difficulty = when {
            nameLower.contains("introduction") || nameLower.contains("basic") ||
            nameLower.contains("overview") || nameLower.contains("simple") -> Difficulty.EASY
            
            nameLower.contains("advanced") || nameLower.contains("complex") ||
            nameLower.contains("optimization") || nameLower.contains("dynamic programming") -> Difficulty.HARD
            
            else -> Difficulty.MEDIUM
        }
        
        // Smart duration based on type and name
        val duration = when {
            nameLower.contains("introduction") || nameLower.contains("overview") -> 15
            type == TopicType.CODE -> 30
            type == TopicType.NUMERICAL -> 25
            difficulty == Difficulty.HARD -> 30
            difficulty == Difficulty.EASY -> 15
            else -> 20
        }
        
        // Smart priority
        val priority = when {
            nameLower.contains("important") || nameLower.contains("key") ||
            difficulty == Difficulty.HARD -> 3
            nameLower.contains("optional") || nameLower.contains("extra") -> 1
            else -> 2
        }
        
        return Topic(
            subjectId = subjectId,
            name = name,
            estimatedTimeMinutes = duration,
            difficulty = difficulty,
            type = type,
            priority = priority
        )
    }
    
    private fun parsePriority(str: String): Int {
        return when (str.lowercase().trim()) {
            "high", "3", "h" -> 3
            "low", "1", "l" -> 1
            else -> 2
        }
    }
    
    private fun generateRandomColor(): String {
        val colors = listOf(
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", 
            "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F",
            "#BB8FCE", "#85C1E9", "#F8B500", "#00CED1"
        )
        return colors.random()
    }
    
    fun regeneratePlan() {
        generatePlan()
    }
    
    /**
     * Distributes topics across days using a round-robin approach per subject.
     * This ensures each day has a mix of subjects based on their priority.
     * Higher priority subjects get proportionally more time.
     */
    private suspend fun distributTopicsAcrossDays(
        topicsBySubject: Map<Long, List<Topic>>,
        subjects: List<Subject>,
        dailyMinutes: Int
    ): List<RevisionSession> {
        val sessions = mutableListOf<RevisionSession>()
        val startDate = getStartOfDay(System.currentTimeMillis())
        
        // Create a queue of topics for each subject, sorted by priority (high priority first)
        val subjectQueues = subjects
            .sortedByDescending { it.priority }
            .mapNotNull { subject ->
                val topics = topicsBySubject[subject.id]
                if (topics.isNullOrEmpty()) null
                else subject to topics.toMutableList()
            }
            .toMutableList()
        
        if (subjectQueues.isEmpty()) return sessions
        
        // Calculate time allocation per subject based on priority
        // Priority 3 gets 3 shares, Priority 2 gets 2 shares, Priority 1 gets 1 share
        val totalPriorityWeight = subjectQueues.sumOf { it.first.priority }
        
        var currentDay = 0
        var currentDayTime = 0
        
        // Keep going until all subjects have no more topics
        while (subjectQueues.isNotEmpty()) {
            var madeProgress = false
            
            // Round-robin through subjects
            val iterator = subjectQueues.iterator()
            while (iterator.hasNext()) {
                val (subject, topicQueue) = iterator.next()
                
                if (topicQueue.isEmpty()) {
                    iterator.remove()
                    continue
                }
                
                // Calculate how much time this subject should get per day
                val subjectTimeShare = (dailyMinutes * subject.priority) / totalPriorityWeight
                val targetTimeForSubject = maxOf(subjectTimeShare, 15) // At least 15 mins
                
                var subjectTimeToday = 0
                
                // Add topics from this subject until we hit its share or run out
                while (topicQueue.isNotEmpty() && subjectTimeToday < targetTimeForSubject) {
                    val topic = topicQueue.first()
                    
                    // Check if we can fit this topic today
                    if (currentDayTime + topic.estimatedTimeMinutes > dailyMinutes && currentDayTime > 0) {
                        // Day is full, move to next day
                        currentDay++
                        currentDayTime = 0
                        break // Start fresh with round-robin for new day
                    }
                    
                    val sessionDate = startDate + (currentDay * 24L * 60L * 60L * 1000L)
                    
                    sessions.add(
                        RevisionSession(
                            topicId = topic.id,
                            date = sessionDate,
                            durationMinutes = topic.estimatedTimeMinutes,
                            isCompleted = false
                        )
                    )
                    
                    currentDayTime += topic.estimatedTimeMinutes
                    subjectTimeToday += topic.estimatedTimeMinutes
                    topicQueue.removeAt(0)
                    madeProgress = true
                }
                
                // If day is full after this subject, move to next day
                if (currentDayTime >= dailyMinutes) {
                    currentDay++
                    currentDayTime = 0
                    break // Start fresh with round-robin for new day
                }
            }
            
            // Safety check to prevent infinite loop
            if (!madeProgress && subjectQueues.isNotEmpty()) {
                // Move to next day if no progress was made
                currentDay++
                currentDayTime = 0
            }
        }
        
        return sessions
    }
    
    fun navigateToNextWeek() {
        val newWeekDates = _state.value.weekDates.map { it + (7 * 24 * 60 * 60 * 1000L) }
        _state.update { it.copy(weekDates = newWeekDates) }
    }
    
    fun navigateToPreviousWeek() {
        val newWeekDates = _state.value.weekDates.map { it - (7 * 24 * 60 * 60 * 1000L) }
        _state.update { it.copy(weekDates = newWeekDates) }
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

private fun generateWeekDates(): List<Long> {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    // Go to start of week (Monday)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
    
    val dates = mutableListOf<Long>()
    for (i in 0 until 7) {
        dates.add(calendar.timeInMillis)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    
    return dates
}
