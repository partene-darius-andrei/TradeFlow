package com.tradeflow.backtesting

import com.tradeflow.backtesting.optimization.ParameterOptimizer
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    ParameterOptimizer().run(args)
}
