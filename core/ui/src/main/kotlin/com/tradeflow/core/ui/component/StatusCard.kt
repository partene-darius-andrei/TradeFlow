package com.tradeflow.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(TradeFlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusCardPreview() {
    TradeFlowTheme {
        StatusCard(
            title = "Portfolio",
            modifier = Modifier.padding(TradeFlowSpacing.md)
        ) {
            Text("Content goes here")
        }
    }
}
