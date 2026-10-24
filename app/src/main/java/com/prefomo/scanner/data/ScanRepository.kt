package com.prefomo.scanner.data

import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

class ScanRepository(private val api: KuCoinApi, private val dao: ScanLogDao) {

    // Tính RSI chu kỳ 14 tương thích pandas: rolling(window=14).mean()
    private fun calculateRSI(closes: List<Double>, period: Int = 14): Double {
        if (closes.size < period + 1) return 50.0
        val deltas = mutableListOf<Double>()
        for (i in 1 until closes.size) {
            deltas.add(closes[i] - closes[i - 1])
        }
        if (deltas.size < period) return 50.0
        val lastWindow = deltas.takeLast(period)
        var gainSum = 0.0
        var lossSum = 0.0
        for (d in lastWindow) {
            if (d > 0) gainSum += d else lossSum += abs(d)
        }
        val avgGain = gainSum / period
        val avgLoss = lossSum / period
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / (avgLoss + 1e-9)
        return 100.0 - (100.0 / (1.0 + rs))
    }

    // Phát hiện nến rút chân chữ V hoặc rũ bỏ (Low thấp hơn 10 phiên trước nhưng close quay lại trên mở cửa)
    // Tương đương detect_spring_bear_trap(df) trong pre_fomo_daily_scanner_khung_1d.py
    private fun detectSpringBearTrap(klines: List<KlineRow>): Boolean {
        if (klines.size < 12) return false
        val latest = klines.last()
        val prev10 = klines.takeLast(11).dropLast(1)
        val lowest10 = prev10.minOf { it.low }
        return (latest.low < lowest10) && (latest.close > latest.open)
    }

    // Tính Bollinger Bandwidth chu kỳ 20 (Dải băng thắt chặt)
    // Tương đương calculate_bollinger_bandwidth(df, 20) trong pre_fomo_weekly_scanner_khung_1w.py
    private fun calculateBollingerBandwidth(closes: List<Double>, period: Int = 20): Double {
        if (closes.size < period) return 0.0
        val slice = closes.takeLast(period)
        val sma = slice.average()
        val variance = slice.map { (it - sma) * (it - sma) }.average()
        val std = sqrt(variance)
        val upper = sma + (std * 2)
        val lower = sma - (std * 2)
        return (upper - lower) / (sma + 1e-9)
    }

    // Tính Trung vị (Median) khối lượng 8 tuần trước
    // Tương đương prev_8['turnover'].median() trong pre_fomo_weekly_scanner_khung_1w.py
    private fun calculateMedian(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

    // ==========================================
    // 1. QUÉT HÀNG NGÀY (1D PRE-FOMO SCANNER)
    // Theo nguyên bản pre_fomo_daily_scanner_khung_1d_e03087a6.py
    // ==========================================
    suspend fun scan1D(
        minVol24hUsdt: Double = 10000.0,
        maxTokens: Int = 2000,
        onProgress: (String, Int, Int) -> Unit,
        onFound: (ScanLogEntity) -> Unit
    ) {
        val allSymbols = api.getUsdtSymbols()
        val symbols = allSymbols.take(maxTokens)
        val total = symbols.size

        symbols.forEachIndexed { index, symbol ->
            onProgress(symbol, index + 1, total)
            val klines = api.getKlines(symbol, "1day")
            if (klines != null && klines.size >= 30) {
                val closes = klines.map { it.close }
                val latest = klines.last()
                val prev20 = klines.takeLast(21).dropLast(1)

                val currClose = latest.close
                val currOpen = latest.open
                val currVol = latest.turnover
                val rsi1d = calculateRSI(closes, 14)

                val volMean20d = prev20.map { it.turnover }.average()
                val volRatio20d = currVol / (volMean20d + 1e-9)

                val priceChangePct = ((currClose - currOpen) / currOpen) * 100
                val isSpring = detectSpringBearTrap(klines)

                // CÁC TIÊU CHÍ BẤT THƯỜNG PRE-FOMO KHUNG 1D:
                // 1. Volume 24h tăng vọt ngầm (Stealth Vol >= 2.2x so với trung bình 20 ngày)
                // 2. Biên độ giá đi ngang hoặc chưa tăng quá sốc (-4.0% <= Change % <= +15.0%)
                // 3. Xuất hiện tín hiệu Spring / Bear Trap rũ bỏ hoặc RSI 1D tích lũy
                // 4. curr_vol >= MIN_VOL_24H_USDT (>= 10,000 USDT)
                if (volRatio20d >= 2.2 && priceChangePct >= -4.0 && priceChangePct <= 15.0 && currVol >= minVol24hUsdt) {
                    var statusNote = "Stealth Vol Gom Hàng"
                    if (isSpring) {
                        statusNote += " + Bẫy Rút Chân Spring"
                    }
                    if (priceChangePct <= 3.0) {
                        statusNote += " (Giá chưa chạy - Gom Ngầm)"
                    }

                    val result = ScanLogEntity(
                        symbol = symbol,
                        price = currClose,
                        changePct = priceChangePct,
                        rsi = rsi1d,
                        volRatio = volRatio20d,
                        volUsdt = currVol,
                        timeframe = "1D",
                        signalNote = statusNote,
                        isSpring = isSpring
                    )
                    dao.insertLog(result)
                    onFound(result)
                }
            }
            delay(40) // Nghỉ 40ms tối ưu cho CPU Helio G80 (Samsung A32) không bị quá nhiệt
        }
    }

    // ==========================================
    // 2. QUÉT HÀNG TUẦN (1W PRE-FOMO SCANNER)
    // Theo nguyên bản pre_fomo_weekly_scanner_khung_1w_9a0d7456.py
    // ==========================================
    suspend fun scan1W(
        minVolWeeklyUsdt: Double = 10000.0,
        minHistoryWeeks: Int = 30, // Tiêu chuẩn 100 tuần hoặc tối thiểu 30 tuần
        maxTokens: Int = 2000,
        onProgress: (String, Int, Int) -> Unit,
        onFound: (ScanLogEntity) -> Unit
    ) {
        val allSymbols = api.getUsdtSymbols()
        val symbols = allSymbols.take(maxTokens)
        val total = symbols.size

        symbols.forEachIndexed { index, symbol ->
            onProgress(symbol, index + 1, total)
            val klines = api.getKlines(symbol, "1week")
            if (klines != null && klines.size >= minHistoryWeeks) {
                val closes = klines.map { it.close }
                val rsi1w = calculateRSI(closes, 14)
                val bbWidth = calculateBollingerBandwidth(closes, 20)

                val latest = klines.last()
                val prev8 = klines.takeLast(9).dropLast(1)

                if (prev8.size >= 8) {
                    val currClose = latest.close
                    val currVol = latest.turnover
                    val historyWeeks = klines.size

                    val volMedian8w = calculateMedian(prev8.map { it.turnover })
                    val volRatio = currVol / (volMedian8w + 1e-9)

                    // CÁC TIÊU CHÍ BẤT THƯỜNG PRE-FOMO KHUNG 1W:
                    // 1. RSI 1W nén ở vùng an toàn (30.0 <= rsi_1w <= 58.0)
                    // 2. Volume 1W tăng vọt đột ngột (Vol Ratio >= 1.8x so với trung vị 8 tuần)
                    // 3. Giá trị giao dịch tuần đạt tối thiểu MIN_VOL_24H_USDT (>= 10,000 USDT)
                    if (rsi1w in 30.0..58.0 && volRatio >= 1.8 && currVol >= minVolWeeklyUsdt) {
                        val result = ScanLogEntity(
                            symbol = symbol,
                            price = currClose,
                            changePct = ((latest.close - latest.open) / latest.open) * 100,
                            rsi = rsi1w,
                            volRatio = volRatio,
                            volUsdt = currVol,
                            timeframe = "1W",
                            signalNote = "Nén BB 1W + Phân Kỳ Vol Spike",
                            bbWidth = bbWidth,
                            historyWeeks = historyWeeks
                        )
                        dao.insertLog(result)
                        onFound(result)
                    }
                }
            }
            delay(50) // Nghỉ 50ms cho khung tuần
        }
    }
}
