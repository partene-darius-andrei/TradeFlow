package com.tradeflow.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun ThemePreview() {
    TradeFlowTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Display Large",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Headline Large",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Title Large",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Body Large",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}
