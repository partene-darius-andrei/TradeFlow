package com.dpart.tradeflow.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dpart.tradeflow.presentation.dashboard.components.ModeCard
import com.dpart.tradeflow.presentation.dashboard.components.OrdersList
import com.dpart.tradeflow.presentation.dashboard.components.PortfolioCard
import com.dpart.tradeflow.presentation.dashboard.components.ServiceCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TradeFlow") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = TradeFlowSpacing.md)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(TradeFlowSpacing.sm))

            PortfolioCard()

            ModeCard()

            ServiceCard()

            OrdersList()

            Spacer(modifier = Modifier.height(TradeFlowSpacing.md))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    TradeFlowTheme {
        DashboardScreen()
    }
}
