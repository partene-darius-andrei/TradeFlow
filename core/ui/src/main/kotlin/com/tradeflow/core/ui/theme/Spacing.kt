package com.tradeflow.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * TradeFlow spacing system
 * Use these instead of hardcoded dp values for consistency
 */
object TradeFlowSpacing {
    val xs = 4.dp     // Icon padding, tight spacing
    val sm = 8.dp     // List item internal spacing
    val md = 16.dp    // Default card padding, general spacing
    val lg = 24.dp    // Screen edge padding
    val xl = 32.dp    // Section spacing, large gaps
    val xxl = 48.dp   // Major section dividers
}

/**
 * Corner radius values
 */
object TradeFlowShapes {
    val sm = 4.dp     // Small buttons, chips
    val md = 8.dp     // Cards, inputs (default)
    val lg = 16.dp    // Large cards, dialogs
    val xl = 24.dp    // Bottom sheets
}
