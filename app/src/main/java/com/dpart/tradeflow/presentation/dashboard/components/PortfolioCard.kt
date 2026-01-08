package com.dpart.tradeflow.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.component.PriceDisplay
import com.tradeflow.core.ui.component.StatusCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme
import java.math.BigDecimal

@Composable
fun PortfolioCard(
    btcBalance: BigDecimal = BigDecimal.ZERO,
    usdBalance: BigDecimal = BigDecimal.ZERO,
    totalValue: BigDecimal = BigDecimal.ZERO
) {
    StatusCard(title = "Portfolio") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PriceDisplay(
                price = totalValue,
                previousPrice = totalValue,
                style = MaterialTheme.typography.displayMedium
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Live Data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(TradeFlowSpacing.md))

        AssetRow(
            asset = "BTC",
            amount = btcBalance.stripTrailingZeros().toPlainString(),
            value = "—"
        )
        AssetRow(
            asset = "USD",
            amount = "—",
            value = "$$usdBalance"
        )
    }
}

@Composable
private fun AssetRow(
    asset: String,
    amount: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$asset: $amount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioCardPreview() {
    TradeFlowTheme {
        PortfolioCard()
    }
}