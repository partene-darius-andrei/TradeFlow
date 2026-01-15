package com.tradeflow.backtesting.data

import com.tradeflow.backtesting.config.ValidationConfig
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

object RandomPeriodGenerator {

    private val BITCOIN_BIRTH = LocalDate.of(2017, 1, 1)
    private val TODAY = LocalDate.now()

    fun generateRandomPeriods(
        config: ValidationConfig = ValidationConfig(),
        count: Int = config.period.defaultNumPeriods,
        minDurationDays: Int = config.period.minPeriodDays,
        maxDurationDays: Int = config.period.maxPeriodDays,
        seed: Long? = null
    ): List<Pair<Long, Long>> {
        val random = seed?.let { Random(it) } ?: Random.Default
        val periods = emptyList<Pair<Long, Long>>().toMutableList()
        val totalDaysAvailable = ChronoUnit.DAYS.between(BITCOIN_BIRTH, TODAY).toInt()

        repeat(count) {
            val durationDays = random.nextInt(minDurationDays, maxDurationDays + 1)
            val maxStartDay = totalDaysAvailable - durationDays
            val startDay = random.nextInt(0, maxStartDay)

            val startDate = BITCOIN_BIRTH.plusDays(startDay.toLong())
            val endDate = startDate.plusDays(durationDays.toLong())

            val startTime = startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val endTime = endDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

            periods.add(startTime to endTime)
        }

        return periods.sortedBy { it.first }
    }

    fun calculateRequiredCandles(
        period: Pair<Long, Long>,
        interval: String,
        config: ValidationConfig = ValidationConfig()
    ): Int {
        val durationMillis = period.second - period.first
        val intervalMillis = when (interval) {
            "1m" -> 60_000L
            "5m" -> 300_000L
            "15m" -> 900_000L
            "1h" -> 3_600_000L
            "4h" -> 14_400_000L
            "1d" -> 86_400_000L
            else -> throw IllegalArgumentException("Unsupported interval: $interval")
        }
        return ((durationMillis / intervalMillis) + config.period.lookbackBuffer).toInt()
    }
}
