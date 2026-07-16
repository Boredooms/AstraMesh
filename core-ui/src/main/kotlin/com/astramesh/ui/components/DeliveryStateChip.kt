package com.astramesh.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.astramesh.domain.model.DeliveryState
import com.astramesh.ui.theme.AstraCritical
import com.astramesh.ui.theme.AstraSuccess
import com.astramesh.ui.theme.AstraTextDisabled
import com.astramesh.ui.theme.AstraTextSecondary
import com.astramesh.ui.theme.AstraWarning

/**
 * Compact status indicator for a message's [DeliveryState] (docs/design.md §10, §5).
 * Color carries secondary meaning only — the label is always legible on its own so the
 * product still reads correctly if the accent is removed.
 */
@Composable
fun DeliveryStateChip(state: DeliveryState, modifier: Modifier = Modifier) {
    val (label, color) = state.chipStyle()
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private fun DeliveryState.chipStyle(): Pair<String, Color> = when (this) {
    DeliveryState.DRAFT -> "Draft" to AstraTextDisabled
    DeliveryState.PENDING -> "Pending" to AstraWarning
    DeliveryState.SENT -> "Sent" to AstraTextSecondary
    DeliveryState.RELAYED -> "Relayed" to AstraWarning
    DeliveryState.DELIVERED -> "Delivered" to AstraSuccess
    DeliveryState.FAILED -> "Failed" to AstraCritical
    DeliveryState.EXPIRED -> "Expired" to AstraTextDisabled
}
