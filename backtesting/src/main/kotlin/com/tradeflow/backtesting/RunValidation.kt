package com.tradeflow.backtesting

import com.tradeflow.backtesting.optimization.ParameterValidator
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    ParameterValidator().invoke()
}
