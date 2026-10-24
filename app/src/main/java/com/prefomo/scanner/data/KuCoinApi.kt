package com.prefomo.scanner.data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.json.JSONObject

class KuCoinApi {
    private val client = HttpClient(CIO)

    suspend fun getUsdtSymbols(): List<String> {
        return try {
            val response: String = client.get("https://api.kucoin.com/api/v1/symbols").bodyAsText()
            val json = JSONObject(response)
            val data = json.getJSONArray("data")
            val list = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val symbol = obj.getString("symbol")
                val enableTrading = obj.getBoolean("enableTrading")
                if (symbol.endsWith("-USDT") && enableTrading) {
                    list.add(symbol)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getKlines(symbol: String, granularity: String): List<KlineRow>? {
        return try {
            val url = "https://api.kucoin.com/api/v1/market/candles?symbol=$symbol&type=$granularity"
            val response: String = client.get(url).bodyAsText()
            val json = JSONObject(response)
            if (!json.has("data") || json.isNull("data")) return null
            
            val array = json.getJSONArray("data")
            val list = mutableListOf<KlineRow>()
            for (i in 0 until array.length()) {
                val row = array.getJSONArray(i)
                list.add(
                    KlineRow(
                        time = row.getLong(0),
                        open = row.getDouble(1),
                        close = row.getDouble(2),
                        high = row.getDouble(3),
                        low = row.getDouble(4),
                        volume = row.getDouble(5),
                        turnover = row.getDouble(6)
                    )
                )
            }
            list.reversed() // Sắp xếp từ cũ đến mới để phân tích kỹ thuật
        } catch (e: Exception) {
            null
        }
    }
}

data class KlineRow(
    val time: Long,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val turnover: Double
)
