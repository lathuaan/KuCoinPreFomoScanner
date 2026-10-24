package com.prefomo.scanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prefomo.scanner.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repo = ScanRepository(KuCoinApi(), db.scanLogDao())

    val logHistory: StateFlow<List<ScanLogEntity>> = db.scanLogDao().getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedTimeframe = MutableStateFlow("1D") // "1D" hoặc "1W"
    var isScanning = MutableStateFlow(false)
    var currentSymbol = MutableStateFlow("-")
    var progressText = MutableStateFlow("Sẵn sàng")

    fun setTimeframe(tf: String) {
        if (!isScanning.value) {
            selectedTimeframe.value = tf
        }
    }

    fun startScan() {
        if (isScanning.value) return
        isScanning.value = true
        val tf = selectedTimeframe.value

        viewModelScope.launch {
            if (tf == "1W") {
                repo.scan1W(
                    minVolWeeklyUsdt = 10000.0,
                    minHistoryWeeks = 30,
                    maxTokens = 2000,
                    onProgress = { symbol, cur, total ->
                        currentSymbol.value = symbol
                        progressText.value = "[1W] $cur / $total"
                    },
                    onFound = { }
                )
            } else {
                repo.scan1D(
                    minVol24hUsdt = 10000.0,
                    maxTokens = 2000,
                    onProgress = { symbol, cur, total ->
                        currentSymbol.value = symbol
                        progressText.value = "[1D] $cur / $total"
                    },
                    onFound = { }
                )
            }
            isScanning.value = false
            progressText.value = "Hoàn tất ($tf)"
        }
    }

    fun clearLogs() {
        viewModelScope.launch { db.scanLogDao().clearAll() }
    }

    // Xuất báo cáo chính xác theo nguyên bản Python của 1D và 1W
    fun exportReport(timeframeFilter: String? = null): String {
        val currentTf = timeframeFilter ?: selectedTimeframe.value
        val list = logHistory.value.filter { timeframeFilter == null || it.timeframe == currentTf }
        val sb = StringBuilder()
        sb.appendLine("=====================================================================")
        sb.appendLine("// DANH SÁCH QUÉT TÍN HIỆU PRE-FOMO KUCOIN - KHUNG THỜI GIAN: $currentTf SCAN")
        sb.appendLine("// Khối lượng: > $10,000 USDT | Mở rộng: 2000 token | Thiết bị: Samsung Galaxy A32")
        sb.appendLine("// Tổng số tín hiệu phát hiện: ${list.size}")
        sb.appendLine("=====================================================================")
        
        if (currentTf == "1W") {
            // Theo nguyên bản pre_fomo_weekly_scanner_khung_1w.py:
            // Symbol, Price, RSI_1W, Vol_Ratio_vs_8W, BB_Width_1W, History_Weeks, Vol_1W_USDT, Trạng Thái Pre-FOMO
            sb.appendLine("STT,Symbol,Price(USDT),RSI_1W,Vol_Ratio_vs_8W,BB_Width_1W,History_Weeks,Vol_1W_USDT,Trạng Thái Pre-FOMO")
            list.forEachIndexed { i, item ->
                sb.appendLine("${i + 1},${item.symbol},${item.price},${String.format("%.1f", item.rsi)},${String.format("%.2f", item.volRatio)},${String.format("%.3f", item.bbWidth)},${item.historyWeeks},${String.format("%.2f", item.volUsdt)},${item.signalNote}")
            }
        } else {
            // Theo nguyên bản pre_fomo_daily_scanner_khung_1d.py:
            // Symbol, Price, Change_1D_%, RSI_1D, Vol_Ratio_vs_20D, Vol_24h_USDT, Tín Hiệu Pre-FOMO 1D
            sb.appendLine("STT,Symbol,Price(USDT),Change_1D_%,RSI_1D,Vol_Ratio_vs_20D,Vol_24h_USDT,Tín Hiệu Pre-FOMO 1D")
            list.forEachIndexed { i, item ->
                sb.appendLine("${i + 1},${item.symbol},${item.price},${String.format("%.2f", item.changePct)}%,${String.format("%.1f", item.rsi)},${String.format("%.2f", item.volRatio)},${String.format("%.2f", item.volUsdt)},${item.signalNote}")
            }
        }
        return sb.toString()
    }
}
