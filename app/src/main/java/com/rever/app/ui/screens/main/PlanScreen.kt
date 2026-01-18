package com.rever.app.ui.screens.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rever.app.data.model.RevisionSessionWithDetails
import com.rever.app.data.model.SubjectWithSessions
import com.rever.app.data.model.TopicType
import com.rever.app.ui.components.ReverFilledButton
import com.rever.app.ui.components.ReverOutlinedButton
import com.rever.app.ui.components.TimelineItemData
import com.rever.app.ui.components.TimelineNodeState
import com.rever.app.ui.components.TimelineStepper
import com.rever.app.ui.viewmodel.DaySessionGroup
import java.text.SimpleDateFormat
import java.util.*

// Helper function to get icon for topic type
private fun TopicType.icon(): ImageVector = when (this) {
    TopicType.THEORY -> Icons.AutoMirrored.Outlined.MenuBook
    TopicType.NUMERICAL -> Icons.Outlined.Calculate
    TopicType.CODE -> Icons.Outlined.Code
    TopicType.MCQ -> Icons.Outlined.Quiz
    TopicType.PRACTICAL -> Icons.Outlined.Science
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    weekDates: List<Long>,
    selectedDate: Long,
    sessions: List<RevisionSessionWithDetails>,
    groupedSessions: List<SubjectWithSessions>,
    monthSessionsByDate: Map<Long, List<String>>,
    infiniteScrollDays: List<DaySessionGroup>,
    isLoadingMore: Boolean,
    totalMinutesForDay: Int,
    completedMinutesForDay: Int,
    dailyTime: Int,
    isPlanGenerated: Boolean,
    onDateSelected: (Long) -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onCompleteWithConfidence: (Long, Long, Int) -> Unit, // sessionId, topicId, confidence
    onUpdateCompletionPercentage: (Long, Int) -> Unit, // sessionId, percentage (0-100)
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onGeneratePlan: () -> Unit,
    onImportFile: (Uri) -> Unit,
    onShowDailyTimeDialog: () -> Unit,
    onLoadMoreDays: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    importSuccess: Boolean,
    importedCount: Int,
    onClearImportSuccess: () -> Unit
) {
    // State for confidence rating dialog
    var showConfidenceDialog by remember { mutableStateOf(false) }
    var pendingSession by remember { mutableStateOf<RevisionSessionWithDetails?>(null) }
    
    // State for calendar view mode
    var isMonthView by remember { mutableStateOf(false) }
    
    // State for content layout mode (cards vs timeline)
    var isTimelineView by remember { mutableStateOf(false) }
    
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportFile(it) }
    }
    
    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            onClearError()
        }
    }
    
    // Show import success snackbar
    LaunchedEffect(importSuccess) {
        if (importSuccess) {
            snackbarHostState.showSnackbar(
                message = "Successfully imported $importedCount topics!",
                duration = SnackbarDuration.Short
            )
            onClearImportSuccess()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Revision Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { 
                            filePickerLauncher.launch(arrayOf(
                                "application/json",
                                "text/plain",
                                "text/*",
                                "*/*"
                            ))
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = "Import List",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onGeneratePlan,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Regenerate Plan",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month header with view toggle
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val monthName = monthFormat.format(Date(selectedDate))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousWeek) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = if (isMonthView) "Previous Month" else "Previous Week"
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // View toggle button
                    IconButton(
                        onClick = { isMonthView = !isMonthView },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isMonthView) 
                                Icons.Outlined.CalendarViewWeek 
                            else 
                                Icons.Outlined.CalendarMonth,
                            contentDescription = if (isMonthView) "Week View" else "Month View",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                IconButton(onClick = onNextWeek) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = if (isMonthView) "Next Month" else "Next Week"
                    )
                }
            }
            
            // Calendar view - Week or Month
            if (isMonthView) {
                MonthCalendarView(
                    selectedDate = selectedDate,
                    today = today,
                    sessionsByDate = monthSessionsByDate,
                    onDateSelected = onDateSelected
                )
            } else {
                // Week strip
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(weekDates) { date ->
                        DayChip(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            onClick = { onDateSelected(date) }
                        )
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            
            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (!isPlanGenerated) {
                // No plan generated yet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No revision plan yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generate a plan to start your revision journey, or import a list from file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        ReverFilledButton(
                            text = "Generate Plan",
                            onClick = onGeneratePlan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ReverOutlinedButton(
                            text = "Import from File",
                            onClick = {
                                filePickerLauncher.launch(arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "text/*",
                                    "*/*"
                                ))
                            }
                        )
                    }
                }
            } else {
                // Infinite scroll with date sections
                val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()) }
                val listState = rememberLazyListState()
                
                // Trigger load more when reaching end
                val shouldLoadMore = remember {
                    derivedStateOf {
                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItems = listState.layoutInfo.totalItemsCount
                        lastVisibleItem >= totalItems - 3 && !isLoadingMore
                    }
                }
                
                LaunchedEffect(shouldLoadMore.value) {
                    if (shouldLoadMore.value && infiniteScrollDays.isNotEmpty()) {
                        onLoadMoreDays()
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    infiniteScrollDays.forEachIndexed { dayIndex, dayGroup ->
                        val isToday = dayGroup.date == today
                        val formattedDate = dateFormat.format(Date(dayGroup.date))
                        
                        // Date separator header
                        item(key = "header_${dayGroup.date}") {
                            DateSeparator(
                                date = formattedDate,
                                isToday = isToday,
                                totalMinutes = dayGroup.totalMinutes,
                                completedMinutes = dayGroup.completedMinutes
                            )
                        }
                        
                        if (dayGroup.sessions.isEmpty()) {
                            // Empty day
                            item(key = "empty_${dayGroup.date}") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "No topics scheduled",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (isTimelineView) {
                            // Timeline view for this day
                            item(key = "timeline_${dayGroup.date}") {
                                val timelineItems = dayGroup.sessions.mapIndexed { index, session ->
                                    val state = when {
                                        session.completionPercentage >= 100 -> TimelineNodeState.COMPLETED
                                        session.completionPercentage > 0 -> TimelineNodeState.CURRENT
                                        dayGroup.sessions.take(index).all { it.completionPercentage >= 100 } && 
                                            dayGroup.sessions.take(index + 1).any { it.completionPercentage < 100 } -> 
                                                TimelineNodeState.CURRENT
                                        else -> TimelineNodeState.UPCOMING
                                    }
                                    
                                    TimelineItemData(
                                        id = session.sessionId,
                                        title = session.topicName,
                                        subtitle = session.subjectName,
                                        colorHex = session.subjectColor,
                                        durationMinutes = session.durationMinutes,
                                        completionPercentage = session.completionPercentage,
                                        state = state,
                                        tags = session.getTagsList()
                                    )
                                }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    TimelineStepper(
                                        items = timelineItems,
                                        onItemClick = { sessionId ->
                                            val session = dayGroup.sessions.find { it.sessionId == sessionId }
                                            if (session != null && session.completionPercentage < 100) {
                                                pendingSession = session
                                                showConfidenceDialog = true
                                                onUpdateCompletionPercentage(sessionId, 100)
                                            }
                                        },
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            // Card view - Session cards grouped by subject
                            items(
                                items = dayGroup.groupedBySubject,
                                key = { "card_${dayGroup.date}_${it.subjectId}" }
                            ) { subjectGroup ->
                                SubjectSessionCard(
                                    subjectWithSessions = subjectGroup,
                                    onUpdatePercentage = { sessionId, percentage ->
                                        val session = subjectGroup.sessions.find { it.sessionId == sessionId }
                                        if (percentage >= 100 && session != null) {
                                            pendingSession = session
                                            showConfidenceDialog = true
                                        }
                                        onUpdateCompletionPercentage(sessionId, percentage)
                                    }
                                )
                            }
                        }
                    }
                    
                    // View toggle row (sticky at top of first day's content)
                    // Moved to be floating action or always visible
                    
                    // Loading more indicator
                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
                
                    // Floating view toggle button
                    FloatingActionButton(
                        onClick = { isTimelineView = !isTimelineView },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isTimelineView) 
                                Icons.Outlined.ViewAgenda 
                            else 
                                Icons.Outlined.Timeline,
                            contentDescription = if (isTimelineView) "Card View" else "Timeline View",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } // End of Box
            }
        }
        
        // Confidence Rating Dialog
        if (showConfidenceDialog && pendingSession != null) {
            ConfidenceRatingDialog(
                topicName = pendingSession!!.topicName,
                currentConfidence = pendingSession!!.confidenceLevel,
                onDismiss = {
                    showConfidenceDialog = false
                    pendingSession = null
                },
                onConfirm = { confidence ->
                    pendingSession?.let { session ->
                        onCompleteWithConfidence(session.sessionId, session.topicId, confidence)
                    }
                    showConfidenceDialog = false
                    pendingSession = null
                }
            )
        }
    }
}

@Composable
private fun SubjectSessionCard(
    subjectWithSessions: SubjectWithSessions,
    onUpdatePercentage: (Long, Int) -> Unit
) {
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(subjectWithSessions.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Colored top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        subjectColor,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                // Subject header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(subjectColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = subjectWithSessions.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Session count badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = subjectColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${subjectWithSessions.completedMinutes}/${subjectWithSessions.totalMinutes}m",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = subjectColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Progress bar for subject
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { subjectWithSessions.completionPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = subjectColor,
                    trackColor = subjectColor.copy(alpha = 0.15f)
                )
                
                // Topics
                Spacer(modifier = Modifier.height(16.dp))
                subjectWithSessions.sessions.forEachIndexed { index, session ->
                    TopicSliderRow(
                        session = session,
                        onPercentageChange = { percentage -> 
                            onUpdatePercentage(session.sessionId, percentage) 
                        }
                    )
                    if (index < subjectWithSessions.sessions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicSliderRow(
    session: RevisionSessionWithDetails,
    onPercentageChange: (Int) -> Unit
) {
    var sliderValue by remember(session.completionPercentage) { 
        mutableStateOf(session.completionPercentage.toFloat()) 
    }
    
    val isCompleted = session.completionPercentage >= 100
    
    // Dynamic colors based on progress
    val progressColor = when {
        sliderValue >= 100 -> Color(0xFF34A853) // Success green
        sliderValue >= 50 -> Color(0xFFFBBC04) // Warning yellow
        else -> MaterialTheme.colorScheme.primary
    }
    
    Row(modifier = Modifier.fillMaxWidth()) {
        // Priority stripe on left
        if (session.priority == 3) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFEA4335))
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            // Topic header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Topic type icon with background
                    Surface(
                        shape = CircleShape,
                        color = if (isCompleted) 
                            progressColor.copy(alpha = 0.15f)
                        else 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = session.topicType.icon(),
                            contentDescription = session.topicType.displayName(),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp),
                            tint = if (isCompleted) 
                                progressColor
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        // Topic name
                        Text(
                            text = session.topicName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (isCompleted) 
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough 
                                else null
                            ),
                            fontWeight = FontWeight.Medium,
                            color = if (isCompleted) 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Tags row
                        val tags = mutableListOf<String>()
                        if (session.isImportant()) tags.add("Important")
                        if (session.isWeakArea()) tags.add("Weak Area")
                        if (session.priority == 3) tags.add("High Priority")
                        
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                tags.take(2).forEach { tag ->
                                    TagChip(
                                        text = tag,
                                        color = when(tag) {
                                            "Important" -> Color(0xFFFFD700)
                                            "Weak Area" -> Color(0xFFF39C12)
                                            "High Priority" -> Color(0xFFEA4335)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Right side: percentage badge + duration
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = progressColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${sliderValue.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = progressColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${session.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Slider
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    onPercentageChange(sliderValue.toInt())
                },
                valueRange = 0f..100f,
                steps = 9, // Creates 10% increments
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = progressColor,
                    activeTrackColor = progressColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DayChip(
    date: Long,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    
    val dayName = dayFormat.format(Date(date))
    val dateNumber = dateFormat.format(Date(date))
    
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = dateNumber,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        
        if (isToday && !isSelected) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MonthCalendarView(
    selectedDate: Long,
    today: Long,
    sessionsByDate: Map<Long, List<String>>,
    onDateSelected: (Long) -> Unit
) {
    val calendar = remember(selectedDate) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    // Get the first day of the month and total days
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // Day labels
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Day of week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayLabels.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        var dayCounter = 1
        val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
        
        for (week in 0 until (totalCells / 7)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayOfWeek in 0..6) {
                    val cellIndex = week * 7 + dayOfWeek
                    
                    if (cellIndex < firstDayOfWeek || dayCounter > daysInMonth) {
                        // Empty cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        // Day cell
                        val dayOfMonth = dayCounter
                        val dayTimestamp = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        val isSelected = dayTimestamp == selectedDate
                        val isToday = dayTimestamp == today
                        val subjectColors = sessionsByDate[dayTimestamp] ?: emptyList()
                        
                        MonthDayCell(
                            day = dayOfMonth,
                            isSelected = isSelected,
                            isToday = isToday,
                            subjectColors = subjectColors,
                            onClick = { onDateSelected(dayTimestamp) },
                            modifier = Modifier.weight(1f)
                        )
                        dayCounter++
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun MonthDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    subjectColors: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        
        // Subject color dots
        if (subjectColors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                subjectColors.take(3).forEach { colorHex ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                else color
                            )
                    )
                }
                if (subjectColors.size > 3) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfidenceRatingDialog(
    topicName: String,
    currentConfidence: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedConfidence by remember { mutableStateOf(if (currentConfidence > 0) currentConfidence else 3) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = topicName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Simple star rating - black filled / white outline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { level ->
                        Icon(
                            imageVector = if (level <= selectedConfidence) 
                                Icons.Rounded.Star 
                            else 
                                Icons.Rounded.StarOutline,
                            contentDescription = "Star $level",
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedConfidence = level }
                                .padding(2.dp),
                            tint = if (level <= selectedConfidence) 
                                MaterialTheme.colorScheme.onSurface
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedConfidence) }
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}

/**
 * Date separator header for infinite scroll
 */
@Composable
private fun DateSeparator(
    date: String,
    isToday: Boolean,
    totalMinutes: Int,
    completedMinutes: Int
) {
    val progress = if (totalMinutes > 0) completedMinutes.toFloat() / totalMinutes else 0f
    val progressColor = when {
        progress >= 1f -> Color(0xFF34A853)
        progress >= 0.5f -> Color(0xFFFBBC04)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left divider line
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = if (isToday) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            // Date label
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isToday) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isToday) "Today" else date,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isToday) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Progress indicator
                    if (totalMinutes > 0) {
                        Text(
                            text = "•",
                            color = if (isToday) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${completedMinutes}/${totalMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) 
                            else 
                                progressColor
                        )
                    }
                }
            }
            
            // Right divider line
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = if (isToday) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}
