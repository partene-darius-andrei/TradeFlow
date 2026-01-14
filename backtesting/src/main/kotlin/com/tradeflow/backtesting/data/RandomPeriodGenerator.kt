package com.tradeflow.backtesting.data

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

data class RandomPeriod(
    val startTime: Long,
    val endTime: Long,
    val durationDays: Int,
    val description: String
)

object RandomPeriodGenerator {

    private val BITCOIN_BIRTH = LocalDate.of(2017, 1, 1)
    private val TODAY = LocalDate.now()

    fun generateRandomPeriods(
        count: Int = 10,
        minDurationDays: Int = 30,
        maxDurationDays: Int = 180,
        seed: Long? = null
    ): List<RandomPeriod> {
        val random = seed?.let { Random(it) } ?: Random.Default
        val periods = mutableListOf<RandomPeriod>()
        val totalDaysAvailable = ChronoUnit.DAYS.between(BITCOIN_BIRTH, TODAY).toInt()

        repeat(count) {
            val durationDays = random.nextInt(minDurationDays, maxDurationDays + 1)
            val maxStartDay = totalDaysAvailable - durationDays
            val startDay = random.nextInt(0, maxStartDay)

            val startDate = BITCOIN_BIRTH.plusDays(startDay.toLong())
            val endDate = startDate.plusDays(durationDays.toLong())

            val startTime = startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val endTime = endDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

            val year = startDate.year
            val month = startDate.month
            val marketCondition = when (year) {
                in 2017..2018 -> "Bull Run → Bear"
                2019 -> "Recovery"
                2020 -> "COVID Crash → Bull"
                2021 -> "Peak Bull Run"
                2022 -> "Bear Market"
                2023 -> "Recovery"
                else -> "Current Era"
            }

            periods.add(
                RandomPeriod(
                    startTime = startTime,
                    endTime = endTime,
                    durationDays = durationDays,
                    description = "$year $month ($durationDays days) - $marketCondition"
                )
            )
        }

        return periods.sortedBy { it.startTime }
    }

    fun calculateRequiredCandles(period: RandomPeriod, interval: String): Int {
        val durationMillis = period.endTime - period.startTime
        val intervalMillis = when (interval) {
            "1m" -> 60_000L
            "5m" -> 300_000L
            "15m" -> 900_000L
            "1h" -> 3_600_000L
            "4h" -> 14_400_000L
            "1d" -> 86_400_000L
            else -> throw IllegalArgumentException("Unsupported interval: $interval")
        }
        return ((durationMillis / intervalMillis) + 200).toInt()
    }
}
