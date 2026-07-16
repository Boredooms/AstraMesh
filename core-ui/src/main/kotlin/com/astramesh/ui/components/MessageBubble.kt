package com.astramesh.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.astramesh.domain.model.DeliveryState
import com.astramesh.ui.theme.AstraBubbleIncoming
import com.astramesh.ui.theme.AstraBubbleOutgoing
import com.astramesh.ui.theme.AstraSpacing
import com.astramesh.ui.theme.AstraTextDisabled
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A single chat message bubble (docs/design.md §8, §10). Outgoing bubbles align right and
 * carry a [DeliveryStateChip]; incoming bubbles align left and show the relay hop count when
 * the message actually traveled through the mesh (hopCount > 0), making relay visible in the
 * UI as called for in docs/architecture.md §14.
 */
@Composable
fun MessageBubble(
    text: String,
    timestamp: Long,
    outgoing: Boolean,
    state: DeliveryState,
    hopCount: Int,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (outgoing) AstraBubbleOutgoing else AstraBubbleIncoming
    val shape = if (outgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
        ) {
            Surface(color = bubbleColor, shape = shape) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = AstraSpacing.md,
                        vertical = AstraSpacing.sm,
                    ),
                )
            }
            Row(
                modifier = Modifier.padding(top = AstraSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.sm),
            ) {
                Text(
                    text = formatTime(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraTextDisabled,
                )
                if (!outgoing && hopCount > 0) {
                    Text(
                        text = "· relayed ×$hopCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = AstraTextDisabled,
                    )
                }
                if (outgoing) {
                    DeliveryStateChip(state = state)
                }
            }
        }
    }
}

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatTime(epochMillis: Long): String = timeFormatter.format(Date(epochMillis))
