package com.prefomo.scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prefomo.scanner.data.ScanLogEntity

val AmoledBlack = Color(0xFF000000)
val DarkCard = Color(0xFF161616)
val NeonGreen = Color(0xFF00E676)
val OrangeAlert = Color(0xFFFF9100)
val Blue1D = Color(0xFF00E5FF)
val Purple1W = Color(0xFFE040FB)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val logs by viewModel.logHistory.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val currentSymbol by viewModel.currentSymbol.collectAsState()
    val progressText by viewModel.progressText.collectAsState()
    val selectedTf by viewModel.selectedTimeframe.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var filterTf by remember { mutableStateOf("ALL") }

    val displayedLogs = remember(logs, filterTf) {
        if (filterTf == "ALL") logs else logs.filter { it.timeframe == filterTf }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(16.dp)
    ) {
        // TOP STATUS BOARD
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Hàng chọn khung thời gian 1D / 1W và nút quét
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PRE-FOMO SCANNER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Trạng thái: $progressText", color = if (isScanning) NeonGreen else Color.Gray, fontSize = 12.sp)
                    }

                    // Nút chuyển 1D / 1W
                    Row(
                        modifier = Modifier.background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(2.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setTimeframe("1D") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTf == "1D") Blue1D else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            enabled = !isScanning
                        ) {
                            Text("1D", color = if (selectedTf == "1D") Color.Black else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.setTimeframe("1W") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTf == "1W") Purple1W else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            enabled = !isScanning
                        ) {
                            Text("1W", color = if (selectedTf == "1W") Color.Black else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { viewModel.startScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        enabled = !isScanning
                    ) {
                        Text(if (isScanning) "ĐANG QUÉT" else "BẮT ĐẦU", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (isScanning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Đang kiểm tra [$selectedTf]: $currentSymbol (Mở rộng 2000 token, Vol > $10k)", color = OrangeAlert, fontSize = 11.sp)
                }
            }
        }

        // TABS & FILTER 1D/1W
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCard,
            contentColor = NeonGreen
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("DASHBOARD (${logs.size})", modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("NHẬT KÝ LƯU TRỮ", modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dòng ghi chú phân biệt danh sách 1D / 1W rõ ràng
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filterTf == "ALL") "DANH SÁCH TẤT CẢ (1D & 1W SCAN)" else "DANH SÁCH: KHUNG THỜI GIAN ${filterTf} SCAN",
                    color = if (filterTf == "1W") Purple1W else if (filterTf == "1D") Blue1D else NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Bộ lọc hiển thị
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ALL", "1D", "1W").forEach { tf ->
                        TextButton(
                            onClick = { filterTf = tf },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                tf,
                                color = if (filterTf == tf) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = if (filterTf == tf) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TAB CONTENT
        if (selectedTab == 0) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedLogs) { item ->
                    SignalCard(item)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Dữ liệu offline SQLite",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Button(
                        onClick = { viewModel.clearLogs() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Xóa lịch sử", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(displayedLogs) { item ->
                        SignalCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun SignalCard(item: ScanLogEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    // Dòng nhãn ghi chú 1D hay 1W phân biệt trực quan
                    Surface(
                        color = if (item.timeframe == "1W") Purple1W.copy(alpha = 0.2f) else Blue1D.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${item.timeframe} SCAN",
                            color = if (item.timeframe == "1W") Purple1W else Blue1D,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Text("$${item.price}", color = Color.White, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RSI: ${String.format("%.1f", item.rsi)} | Change: ${String.format("%.2f", item.changePct)}%", color = Color.Gray, fontSize = 12.sp)
                Text("Vol Ratio: ${String.format("%.2f", item.volRatio)}x", color = OrangeAlert, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = NeonGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text(item.signalNote, color = NeonGreen, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Text("Vol: > $10,000 USDT", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}
