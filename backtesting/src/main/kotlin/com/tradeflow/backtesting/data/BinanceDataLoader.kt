package com.tradeflow.backtesting.data

import com.tradeflow.backtesting.model.Period
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Interval
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.Instant

object BinanceDataLoader {

    private val client = HttpClient(OkHttp)

    fun fetchPeriodData(
        period: Period,
        interval: Interval
    ): List<Candle> {
        return fetchHistoricalCandles(
            startTime = period.startMs,
            endTime = period.endMs,
            interval = interval
        )
    }

    fun fetchHistoricalCandles(
        startTime: Long,
        endTime: Long,
        interval: Interval
    ): List<Candle> = runBlocking {
        val url = buildString {
            append("https://api.binance.com/api/v3/klines")
            append("?symbol=BTCUSDT")
            append("&interval=${interval.apiString}")
            append("&limit=1000")
            append("&startTime=$startTime")
            append("&endTime=$endTime")
        }

        val response: String = client.get(url).body()
        parseKlines(response)
    }

    private fun parseKlines(jsonResponse: String): List<Candle> {
        val json = Json { ignoreUnknownKeys = true }
        val element = json.parseToJsonElement(jsonResponse)

        // Check if response is an error object
        if (element is JsonObject) {
            val code = element["code"]?.jsonPrimitive?.content
            val msg = element["msg"]?.jsonPrimitive?.content
            throw IllegalStateException("Binance API error: code=$code, msg=$msg")
        }

        // Parse array of candles
        val rawData = element.jsonArray

        return rawData.map { candleElement ->
            val kline = candleElement.jsonArray
            Candle(
                timestamp = Instant.ofEpochMilli(kline[0].jsonPrimitive.content.toLong()),
                open = BigDecimal(kline[1].jsonPrimitive.content),
                high = BigDecimal(kline[2].jsonPrimitive.content),
                low = BigDecimal(kline[3].jsonPrimitive.content),
                close = BigDecimal(kline[4].jsonPrimitive.content),
                volume = BigDecimal(kline[5].jsonPrimitive.content)
            )
        }
    }
}
