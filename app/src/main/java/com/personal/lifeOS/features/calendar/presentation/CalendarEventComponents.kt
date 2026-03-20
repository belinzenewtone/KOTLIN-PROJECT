@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventStatus
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.ui.theme.CategoryOther
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.Warning

@Composable
internal fun CalendarEventCard(
    event: CalendarEvent,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val typeColor =
        when (event.type) {
            EventType.WORK -> Info
            EventType.HEALTH -> Success
            EventType.FINANCE -> Warning
            EventType.PERSONAL -> MaterialTheme.colorScheme.primary
            EventType.OTHER -> CategoryOther
        }
    val dismissState =
        rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance -> totalDistance * 0.4f },
            confirmValueChange = { target ->
                when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (event.status == EventStatus.PENDING) {
                            onComplete()
                            true
                        } else {
                            false
                        }
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        onDelete()
                        true
                    }
                    else -> false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val backgroundColor by animateColorAsState(
                targetValue =
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Success.copy(alpha = 0.3f)
                        SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    },
                label = "calendar-event-swipe-color",
            )
            val icon =
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Delete
                }
            val iconAlignment =
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 20.dp),
                contentAlignment = iconAlignment,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            elevated = true,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .width(4.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (event.status == EventStatus.COMPLETED) MaterialTheme.colorScheme.outline else typeColor),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration =
                            if (event.status == EventStatus.COMPLETED) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                        color = if (event.status == EventStatus.COMPLETED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventBadge(text = event.type.label, color = typeColor)
                        EventBadge(text = event.importance.label, color = importanceColor(event.importance))
                        if (event.status == EventStatus.COMPLETED) {
                            EventBadge(text = "Completed", color = Success)
                        }
                    }
                    Text(
                        text = "Time: ${DateUtils.formatDate(event.date, "MMM dd, yyyy h:mm a")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (event.endDate != null) {
                        Text(
                            text = "Due: ${DateUtils.formatDate(event.endDate, "MMM dd, yyyy h:mm a")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit event",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventBadge(
    text: String,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun importanceColor(importance: EventImportance): Color =
    when (importance) {
        EventImportance.URGENT -> Color(0xFFEF5350)
        EventImportance.IMPORTANT -> Color(0xFFFFB74D)
        EventImportance.NEUTRAL -> Color(0xFF42A5F5)
    }
