package com.tradeflow.backtesting.data

import com.tradeflow.core.domain.model.Candle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.Instant

object BinanceDataLoader {

    private val client = HttpClient(OkHttp)

    data class MultiTimeframeData(
        val candles1h: List<Candle>,
        val candles30m: List<Candle>,
        val candles15m: List<Candle>,
        val candles5m: List<Candle>,
        val candles1m: List<Candle>
    )

    suspend fun fetchPeriodData(period: Pair<Long, Long>): MultiTimeframeData = coroutineScope {

        val candles1h = async {
            fetchHistoricalCandles(
                interval = "1h",
                startTime = period.first,
                endTime = period.second,
            )
        }

        val candles30m = async {
            fetchHistoricalCandles(
                interval = "30m",
                startTime = period.first,
                endTime = period.second,
            )
        }

        val candles15m = async {
            fetchHistoricalCandles(
                interval = "15m",
                startTime = period.first,
                endTime = period.second,
            )
        }

        val candles5m = async {
            fetchHistoricalCandles(
                interval = "5m",
                startTime = period.first,
                endTime = period.second,
            )
        }

        val candles1m = async {
            fetchHistoricalCandles(
                interval = "1m",
                startTime = period.first,
                endTime = period.second,
            )
        }

        MultiTimeframeData(
            candles1h = candles1h.await(),
            candles30m = candles30m.await(),
            candles15m = candles15m.await(),
            candles5m = candles5m.await(),
            candles1m = candles1m.await()
        )
    }

    fun fetchHistoricalCandles(
        interval: String,
        startTime: Long,
        endTime: Long,
        limit: Int = 500
    ): List<Candle> = runBlocking {
        val url = buildString {
            append("https://api.binance.com/api/v3/klines")
            append("?symbol=BTCUSDT")
            append("&interval=$interval")
            append("&limit=$limit")
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
