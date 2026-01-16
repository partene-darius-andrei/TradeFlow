package com.tradeflow.backtesting.model

import com.tradeflow.backtesting.util.toDateString

data class Period(
    val startMs: Long,
    val endMs: Long
) {
    val durationDays: Int
        get() = ((endMs - startMs) / (1000 * 60 * 60 * 24)).toInt()

    val startDate: String
        get() = startMs.toDateString()

    val endDate: String
        get() = endMs.toDateString()

    override fun toString(): String = "$startDate to $endDate ($durationDays days)"

    companion object {
        fun from(pair: Pair<Long, Long>): Period = Period(pair.first, pair.second)
    }

    fun toPair(): Pair<Long, Long> = Pair(startMs, endMs)
}
