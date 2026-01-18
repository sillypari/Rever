package com.rever.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Timeline/Stepper component for showing a vertical list of items
 * with connected nodes, similar to metro station design
 */

enum class TimelineNodeState {
    COMPLETED,
    CURRENT,
    UPCOMING
}

data class TimelineItemData(
    val id: Long,
    val title: String,
    val subtitle: String,
    val colorHex: String,
    val durationMinutes: Int,
    val completionPercentage: Int,
    val state: TimelineNodeState,
    val tags: List<String> = emptyList()
)

@Composable
fun TimelineStepper(
    items: List<TimelineItemData>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            TimelineStepperItem(
                item = item,
                isFirst = index == 0,
                isLast = index == items.size - 1,
                onClick = { onItemClick(item.id) }
            )
        }
    }
}

@Composable
private fun TimelineStepperItem(
    item: TimelineItemData,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val itemColor = try {
        Color(android.graphics.Color.parseColor(item.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    val nodeColor = when (item.state) {
        TimelineNodeState.COMPLETED -> Color(0xFF34A853) // Green
        TimelineNodeState.CURRENT -> itemColor
        TimelineNodeState.UPCOMING -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    
    val lineColor = when (item.state) {
        TimelineNodeState.COMPLETED -> Color(0xFF34A853).copy(alpha = 0.5f)
        TimelineNodeState.CURRENT -> itemColor.copy(alpha = 0.5f)
        TimelineNodeState.UPCOMING -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Timeline column with node and lines
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // Top connector line
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .background(lineColor)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Node
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when (item.state) {
                            TimelineNodeState.COMPLETED -> nodeColor
                            TimelineNodeState.CURRENT -> nodeColor
                            TimelineNodeState.UPCOMING -> Color.Transparent
                        }
                    )
                    .then(
                        if (item.state == TimelineNodeState.UPCOMING) {
                            Modifier.background(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (item.state) {
                    TimelineNodeState.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                    TimelineNodeState.CURRENT -> {
                        // Pulsing inner dot for current item
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    TimelineNodeState.UPCOMING -> {
                        // Hollow circle
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        )
                    }
                }
            }
            
            // Bottom connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(44.dp)
                        .background(
                            if (item.state == TimelineNodeState.COMPLETED) 
                                lineColor 
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                )
            }
        }
        
        // Content
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 20.dp else 0.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (item.state == TimelineNodeState.COMPLETED) 
                            TextDecoration.LineThrough 
                        else null
                    ),
                    fontWeight = if (item.state == TimelineNodeState.CURRENT) FontWeight.SemiBold else FontWeight.Medium,
                    color = when (item.state) {
                        TimelineNodeState.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        TimelineNodeState.CURRENT -> MaterialTheme.colorScheme.onSurface
                        TimelineNodeState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                // Duration badge
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (item.state) {
                        TimelineNodeState.COMPLETED -> Color(0xFF34A853).copy(alpha = 0.15f)
                        TimelineNodeState.CURRENT -> itemColor.copy(alpha = 0.15f)
                        TimelineNodeState.UPCOMING -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = "${item.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when (item.state) {
                            TimelineNodeState.COMPLETED -> Color(0xFF34A853)
                            TimelineNodeState.CURRENT -> itemColor
                            TimelineNodeState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Subtitle row with subject color
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(itemColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Progress indicator for current
                if (item.state == TimelineNodeState.CURRENT && item.completionPercentage > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${item.completionPercentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            item.completionPercentage >= 100 -> Color(0xFF34A853)
                            item.completionPercentage >= 50 -> Color(0xFFFBBC04)
                            else -> itemColor
                        }
                    )
                }
            }
            
            // Tags
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.tags.take(2).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when(tag.lowercase()) {
                                "important" -> Color(0xFFFFD700).copy(alpha = 0.15f)
                                "weak-area", "weak area" -> Color(0xFFF39C12).copy(alpha = 0.15f)
                                "high priority" -> Color(0xFFEA4335).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = when(tag.lowercase()) {
                                    "important" -> Color(0xFFFFD700)
                                    "weak-area", "weak area" -> Color(0xFFF39C12)
                                    "high priority" -> Color(0xFFEA4335)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
