package com.prefomo.scanner.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_logs")
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val price: Double,
    val changePct: Double,
    val rsi: Double,
    val volRatio: Double,
    val volUsdt: Double,
    val timeframe: String, // "1D" hoặc "1W"
    val signalNote: String,
    val isSpring: Boolean = false, // Phát hiện nến rút chân chữ V / Spring rũ bỏ (1D)
    val bbWidth: Double = 0.0, // Dải Bollinger Bandwidth thắt chặt (1W)
    val historyWeeks: Int = 0, // Số tuần dữ liệu lịch sử (1W)
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ScanLogDao {
    @Query("SELECT * FROM scan_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ScanLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ScanLogEntity)

    @Query("DELETE FROM scan_logs")
    suspend fun clearAll()
}

@Database(entities = [ScanLogEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanLogDao(): ScanLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prefomo_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
