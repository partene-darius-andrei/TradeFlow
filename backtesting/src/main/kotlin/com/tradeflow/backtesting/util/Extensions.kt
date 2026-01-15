package com.tradeflow.backtesting.util

import java.time.Instant

fun Long.toDateString(): String =
    Instant.ofEpochMilli(this).toString().substring(0, 10)
