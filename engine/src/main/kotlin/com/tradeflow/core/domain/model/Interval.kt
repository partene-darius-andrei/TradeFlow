package com.tradeflow.core.domain.model

sealed class Interval(
    val apiString: String,
    val minutes: Int,
    val displayName: String
) {
    data object OneMinute : Interval("1m", 1, "1 Minute")
    data object FiveMinutes : Interval("5m", 5, "5 Minutes")
    data object FifteenMinutes : Interval("15m", 15, "15 Minutes")
    data object ThirtyMinutes : Interval("30m", 30, "30 Minutes")
    data object OneHour : Interval("1h", 60, "1 Hour")
    data object TwoHours : Interval("2h", 120, "2 Hours")
    data object FourHours : Interval("4h", 240, "4 Hours")
    data object SixHours : Interval("6h", 360, "6 Hours")
    data object EightHours : Interval("8h", 480, "8 Hours")
    data object TwelveHours : Interval("12h", 720, "12 Hours")
    data object OneDay : Interval("1d", 1440, "1 Day")
    data object ThreeDays : Interval("3d", 4320, "3 Days")
    data object OneWeek : Interval("1w", 10080, "1 Week")
    data object OneMonth : Interval("1M", 43200, "1 Month")

    override fun toString(): String = displayName
}
