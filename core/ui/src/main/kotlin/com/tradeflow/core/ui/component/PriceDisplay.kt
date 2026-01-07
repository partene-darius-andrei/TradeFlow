package com.tradeflow.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun PriceDisplay(
    price: BigDecimal,
    modifier: Modifier = Modifier,
    previousPrice: BigDecimal? = null,
    style: TextStyle = MaterialTheme.typography.headlineLarge
) {
    val color = when {
        previousPrice == null -> MaterialTheme.colorScheme.onSurface
        price > previousPrice -> MaterialTheme.colorScheme.primary
        price < previousPrice -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val changeSymbol = when {
        previousPrice == null -> ""
        price > previousPrice -> "↗"
        price < previousPrice -> "↘"
        else -> ""
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$${String.format("%,.2f", price)}",
            style = style,
            color = color
        )
        if (changeSymbol.isNotEmpty()) {
            Text(
                text = changeSymbol,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceDisplayPreview() {
    TradeFlowTheme {
        Column(
            modifier = Modifier.padding(TradeFlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm)
        ) {
            PriceDisplay(
                price = BigDecimal("61500.00"),
                previousPrice = BigDecimal("61000.00")
            )
            PriceDisplay(
                price = BigDecimal("61500.00"),
                previousPrice = BigDecimal("62000.00")
            )
            PriceDisplay(price = BigDecimal("61500.00"))
        }
    }
}
