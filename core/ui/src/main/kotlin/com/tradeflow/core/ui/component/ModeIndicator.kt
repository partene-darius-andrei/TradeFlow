package com.tradeflow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun ModeIndicator(
    mode: String,
    modifier: Modifier = Modifier
) {
    val (color, icon, description) = when (mode.uppercase()) {
        "DEFENSE" -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.Shield,
            "Defense Mode"
        )
        "TREND" -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.AutoMirrored.Filled.TrendingUp,
            "Trend Mode"
        )
        "RANGE" -> Triple(
            Color(0xFF2196F3),
            Icons.Default.SwapVert,
            "Range Mode"
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            Icons.Default.HourglassEmpty,
            "Wait"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = TradeFlowSpacing.md, vertical = TradeFlowSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = mode.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModeIndicatorPreview() {
    TradeFlowTheme {
        Column(
            modifier = Modifier.padding(TradeFlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm)
        ) {
            ModeIndicator(mode = "DEFENSE")
            ModeIndicator(mode = "TREND")
            ModeIndicator(mode = "RANGE")
            ModeIndicator(mode = "WAIT")
        }
    }
}
