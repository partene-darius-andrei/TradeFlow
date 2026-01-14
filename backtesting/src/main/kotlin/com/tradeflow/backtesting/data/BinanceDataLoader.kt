package com.tradeflow.backtesting.data

import com.tradeflow.core.domain.model.Candle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.Instant

object BinanceDataLoader {

    private val client = HttpClient(OkHttp)

    fun fetchHistoricalCandles(
        symbol: String = "BTCUSDT",
        interval: String = "1h",
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int = 500
    ): List<Candle> = runBlocking {
        val url = buildString {
            append("https://api.binance.com/api/v3/klines")
            append("?symbol=$symbol")
            append("&interval=$interval")
            append("&limit=$limit")
            if (startTime != null) append("&startTime=$startTime")
            if (endTime != null) append("&endTime=$endTime")
        }

        val response: String = client.get(url).body()
        parseKlines(response, interval)
    }

    private fun parseKlines(jsonArray: String, interval: String): List<Candle> {
        val json = Json { ignoreUnknownKeys = true }
        val rawData = json.parseToJsonElement(jsonArray).jsonArray

        return rawData.map { element ->
            val kline = element.jsonArray
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
