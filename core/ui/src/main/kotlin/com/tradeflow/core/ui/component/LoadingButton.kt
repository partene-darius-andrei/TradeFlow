package com.tradeflow.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun LoadingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(text)
            }
        } else {
            Text(text)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingButtonPreview() {
    TradeFlowTheme {
        Column(
            modifier = Modifier.padding(TradeFlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm)
        ) {
            LoadingButton(text = "Normal", onClick = {})
            LoadingButton(text = "Loading", onClick = {}, loading = true)
            LoadingButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
