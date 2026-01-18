package com.rever.app.ui.screens.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rever.app.data.model.RevisionSessionWithDetails
import com.rever.app.ui.viewmodel.DayProgress
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    totalTopics: Int,
    completedTopics: Int,
    pendingToday: Int,
    overdueCount: Int,
    overdueSessions: List<RevisionSessionWithDetails>,
    allSessions: List<RevisionSessionWithDetails>,
    weeklyProgress: List<DayProgress>,
    totalMinutesStudied: Int,
    currentStreak: Int,
    recentCompletedSessions: List<RevisionSessionWithDetails>,
    onToggleSessionComplete: (Long, Boolean) -> Unit
) {
    var showFullTimeline by remember { mutableStateOf(false) }
    
    val overallProgress = if (totalTopics > 0) {
        completedTopics.toFloat() / totalTopics
    } else {
        0f
    }
    
    if (showFullTimeline) {
        // Full Timeline View
        AllTopicsTimelineScreen(
            sessions = allSessions,
            onNavigateBack = { showFullTimeline = false }
        )
    } else {
        // Main Progress Screen
        Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Streak & Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Outlined.LocalFireDepartment,
                        iconColor = Color(0xFFFF6B35),
                        value = "$currentStreak",
                        label = "Day Streak",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Outlined.Schedule,
                        iconColor = Color(0xFF4285F4),
                        value = formatMinutes(totalMinutesStudied),
                        label = "Time Studied",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Overall Progress Ring - clickable to see all topics
            item {
                ProgressRingCard(
                    progress = overallProgress,
                    completedTopics = completedTopics,
                    totalTopics = totalTopics,
                    pendingToday = pendingToday,
                    overdueCount = overdueCount,
                    onClick = { showFullTimeline = true }
                )
            }
            
            // Weekly Progress Chart
            if (weeklyProgress.isNotEmpty()) {
                item {
                    WeeklyProgressChart(
                        weeklyProgress = weeklyProgress
                    )
                }
            }
            
            // Journey Timeline Section
            if (recentCompletedSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Learning Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recent completed topics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                item {
                    JourneyTimeline(
                        sessions = recentCompletedSessions.take(8)
                    )
                }
            }
            
            // Overdue Section
            if (overdueSessions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ Overdue Topics",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete these to stay on track",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                itemsIndexed(overdueSessions) { index, session ->
                    OverdueTimelineItem(
                        session = session,
                        isFirst = index == 0,
                        isLast = index == overdueSessions.size - 1,
                        onMarkComplete = { onToggleSessionComplete(session.sessionId, true) }
                    )
                }
            }
            
            // Empty state if no data
            if (totalTopics == 0) {
                item {
                    EmptyProgressState()
                }
            }
        }
    }
    } // end of else block
}

@Composable
private fun StatCard(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp),
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProgressRingCard(
    progress: Float,
    completedTopics: Int,
    totalTopics: Int,
    pendingToday: Int,
    overdueCount: Int,
    onClick: () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress Ring
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    
                    // Track
                    drawCircle(
                        color = trackColor,
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Progress arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$completedTopics/$totalTopics",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(
                    value = pendingToday.toString(),
                    label = "Today",
                    color = MaterialTheme.colorScheme.primary
                )
                
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
                
                MiniStat(
                    value = overdueCount.toString(),
                    label = "Overdue",
                    color = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
                
                MiniStat(
                    value = (totalTopics - completedTopics).toString(),
                    label = "Remaining",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Hint to tap
            Text(
                text = "Tap to view all topics →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MiniStat(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyProgressChart(
    weeklyProgress: List<DayProgress>
) {
    val maxMinutes = weeklyProgress.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(60) ?: 60
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyProgress.forEach { day ->
                    WeekDayBar(
                        label = day.dayLabel,
                        completedMinutes = day.completedMinutes,
                        totalMinutes = day.totalMinutes,
                        maxMinutes = maxMinutes,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDayBar(
    label: String,
    completedMinutes: Int,
    totalMinutes: Int,
    maxMinutes: Int,
    modifier: Modifier = Modifier
) {
    val barHeight = if (maxMinutes > 0) {
        (totalMinutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
    } else 0f
    
    val completedRatio = if (totalMinutes > 0) {
        (completedMinutes.toFloat() / totalMinutes).coerceIn(0f, 1f)
    } else 0f
    
    val animatedHeight by animateFloatAsState(
        targetValue = barHeight,
        animationSpec = tween(durationMillis = 800),
        label = "barHeight"
    )
    
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bar container
        Box(
            modifier = Modifier
                .weight(1f)
                .width(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background bar (total)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
            
            // Completed bar overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedHeight * completedRatio)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        when {
                            completedRatio >= 1f -> Color(0xFF34A853)
                            completedRatio >= 0.5f -> Color(0xFFFBBC04)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = label.take(3),
            style = MaterialTheme.typography.labelSmall,
            color = if (label == "Today") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (label == "Today") FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun JourneyTimeline(
    sessions: List<RevisionSessionWithDetails>
) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            sessions.forEachIndexed { index, session ->
                TimelineItem(
                    topicName = session.topicName,
                    subjectName = session.subjectName,
                    subjectColor = session.subjectColor,
                    date = dateFormat.format(Date(session.date)),
                    durationMinutes = session.durationMinutes,
                    isCompleted = true,
                    isFirst = index == 0,
                    isLast = index == sessions.size - 1
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    topicName: String,
    subjectName: String,
    subjectColor: String,
    date: String,
    durationMinutes: Int,
    isCompleted: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    val color = try {
        Color(android.graphics.Color.parseColor(subjectColor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Top line
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Node
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) color else MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Completed",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
            
            // Bottom line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
        }
        
        // Content
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 16.dp else 0.dp)
        ) {
            Text(
                text = topicName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${durationMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Date badge
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun OverdueTimelineItem(
    session: RevisionSessionWithDetails,
    isFirst: Boolean,
    isLast: Boolean,
    onMarkComplete: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(session.subjectColor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.error
    }
    
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Top line
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Node (hollow circle for pending)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
            
            // Bottom line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                )
            }
        }
        
        // Content Card
        Spacer(modifier = Modifier.width(12.dp))
        
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 12.dp else 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.topicName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session.subjectName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                TextButton(
                    onClick = onMarkComplete
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun EmptyProgressState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📊",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No progress yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add subjects and topics to start tracking your revision journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    return when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}

/**
 * Full screen timeline view showing all topics grouped by date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllTopicsTimelineScreen(
    sessions: List<RevisionSessionWithDetails>,
    onNavigateBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()) }
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    // Group sessions by date
    val sessionsByDate = remember(sessions) {
        sessions.groupBy { it.date }.toSortedMap()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "All Topics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sessions.size} topics scheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📅",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No upcoming topics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                sessionsByDate.forEach { (date, dateSessions) ->
                    val isToday = date == today
                    val formattedDate = if (isToday) "Today" else dateFormat.format(Date(date))
                    
                    // Date header
                    item(key = "header_$date") {
                        DateTimelineHeader(
                            date = formattedDate,
                            isToday = isToday,
                            completedCount = dateSessions.count { it.isCompleted },
                            totalCount = dateSessions.size
                        )
                    }
                    
                    // Topics for this date in timeline format
                    itemsIndexed(
                        items = dateSessions,
                        key = { _, session -> "session_${session.sessionId}" }
                    ) { index, session ->
                        AllTopicsTimelineItem(
                            session = session,
                            isFirst = index == 0,
                            isLast = index == dateSessions.size - 1
                        )
                    }
                    
                    // Spacer between date groups
                    item(key = "spacer_$date") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateTimelineHeader(
    date: String,
    isToday: Boolean,
    completedCount: Int,
    totalCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored line
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isToday) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$completedCount/$totalCount completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Progress chip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when {
                completedCount == totalCount -> Color(0xFF34A853).copy(alpha = 0.15f)
                completedCount > 0 -> Color(0xFFFBBC04).copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = "${(completedCount * 100 / totalCount.coerceAtLeast(1))}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    completedCount == totalCount -> Color(0xFF34A853)
                    completedCount > 0 -> Color(0xFFFBBC04)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AllTopicsTimelineItem(
    session: RevisionSessionWithDetails,
    isFirst: Boolean,
    isLast: Boolean
) {
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(session.subjectColor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    val isCompleted = session.isCompleted
    
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Top line
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(
                            if (isCompleted) Color(0xFF34A853).copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Node
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) Color(0xFF34A853)
                        else subjectColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Completed",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(subjectColor)
                    )
                }
            }
            
            // Bottom line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(
                            if (isCompleted) Color(0xFF34A853).copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 8.dp else 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCompleted) 
                    Color(0xFF34A853).copy(alpha = 0.08f)
                else 
                    MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isCompleted) 0.dp else 1.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject color indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(subjectColor)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.topicName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.Medium,
                        color = if (isCompleted) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(subjectColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session.subjectName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = " • ${session.durationMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status badge
                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF34A853).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "✓ Done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34A853),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (session.completionPercentage > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFBBC04).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${session.completionPercentage}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBBC04),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
