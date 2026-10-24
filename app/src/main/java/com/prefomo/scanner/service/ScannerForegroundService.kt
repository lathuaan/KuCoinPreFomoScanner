package com.prefomo.scanner.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.prefomo.scanner.data.AppDatabase
import com.prefomo.scanner.data.KuCoinApi
import com.prefomo.scanner.data.ScanRepository
import kotlinx.coroutines.*

class ScannerForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timeframe = intent?.getStringExtra("TIMEFRAME") ?: "1D"
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "scan_channel")
            .setContentTitle("Pre-FOMO Scanner ($timeframe Active)")
            .setContentText("Đang chạy quét ngầm KuCoin $timeframe (2000 token, Vol > $10k)...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val repo = ScanRepository(KuCoinApi(), db.scanLogDao())
            if (timeframe == "1W") {
                repo.scan1W(
                    minVolWeeklyUsdt = 10000.0,
                    minHistoryWeeks = 30,
                    maxTokens = 2000,
                    onProgress = { _, _, _ -> },
                    onFound = { }
                )
            } else {
                repo.scan1D(
                    minVol24hUsdt = 10000.0,
                    maxTokens = 2000,
                    onProgress = { _, _, _ -> },
                    onFound = { }
                )
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "scan_channel",
                "KuCoin Scanner Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
