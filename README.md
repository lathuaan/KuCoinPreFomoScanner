# KuCoin Pre-FOMO Scanner for Samsung Galaxy A32 (Android 12 / One UI 4.1)

## Thông tin thiết bị mục tiêu:
- **Model**: Samsung Galaxy A32 (SM-A325F)
- **One UI Version**: 4.1
- **Android Version**: 12 (API Level 31)
- **Chipset**: MediaTek Helio G80 (Mali-G52 MC2)

---

## 🚀 Hướng dẫn mở dự án & Build APK bằng Android Studio

### Bước 1: Mở Project trong Android Studio
1. Tải file ZIP dự án và giải nén thư mục `KuCoinPreFomoScanner`.
2. Mở Android Studio (Hedgehog 2023.1.1, Iguana, hoặc Jellyfish).
3. Chọn **File > Open...** và trỏ đến thư mục `KuCoinPreFomoScanner`.
4. Chờ Gradle Sync hoàn tất (Gradle 8.3 + Kotlin 1.9.22).

### Bước 2: Bật Tùy Chọn Nhà Phát Triển trên Samsung A32
1. Mở **Cài đặt (Settings)** trên Samsung Galaxy A32.
2. Cuộn xuống chọn **Thông tin điện thoại (About phone)** > **Thông tin phần mềm (Software information)**.
3. Chạm 7 lần liên tiếp vào **Số hiệu bản tạo (Build number)** đến khi máy báo *"Đã bật chế độ cho nhà phát triển"*.
4. Quay lại menu Cài đặt chính, vào **Cài đặt cho người phát triển (Developer options)**.
5. Bật mục **Gỡ lỗi USB (USB debugging)**.

### Bước 3: Build & Cài đặt trực tiếp lên Samsung A32
1. Cắm cáp USB kết nối Samsung A32 với máy tính. Trên màn hình điện thoại chọn **Cho phép gỡ lỗi USB (Always allow from this computer)**.
2. Trong Android Studio, tại danh sách thiết bị trên thanh công cụ, bạn sẽ thấy **Samsung SM-A325F**.
3. Bấm nút **Run 'app' (Shift + F10)** hoặc biểu tượng Tam giác Xanh để biên dịch và cài đặt APK.

*Hoặc build file APK cài đặt thủ công:*
```bash
./gradlew assembleDebug
# File APK sẽ nằm tại: app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚡ Cực kỳ quan trọng: Thiết lập Pin One UI 4.1 để Scanner chạy ngầm 24/7

Samsung One UI 4.1 có cơ chế quản lý pin cực kỳ khắt khe, tự động đóng các ứng dụng chạy ngầm sau một khoảng thời gian. Để `ScannerForegroundService` quét liên tục:

1. Nhấn giữ vào biểu tượng ứng dụng **KuCoin Pre-FOMO** trên màn hình chính > Chọn biểu tượng **(i)** (Thông tin ứng dụng).
2. Chọn mục **Pin (Battery)**.
3. Chuyển từ chế độ *Được tối ưu (Optimized)* sang **Không hạn chế (Unrestricted)**.
4. Mở tiếp **Cài đặt > Chăm sóc pin và thiết bị > Pin > Giới hạn sử dụng dưới nền > Ứng dụng không bao giờ nghỉ (Never sleeping apps)** > Nhấn dấu **(+)** và thêm **KuCoin Pre-FOMO**.
5. Cấp quyền Thông báo: Vào mục **Thông báo (Notifications)** trong Thông tin ứng dụng và đảm bảo đã bật để Foreground Service duy trì Notification.

---

## 📊 Thông số Thuật toán Pre-FOMO Scanner (Cập nhật mới)
- **Timeframe**: Hỗ trợ đầy đủ cả **1D** (Nến ngày) và **1W** (Nến tuần).
- **Mở rộng danh sách token**: Quét lên đến **2,000 token** cho cả 1D và 1W.
- **Khối lượng giao dịch tối thiểu (Min Vol USDT)**: **Lớn hơn 10,000 USDT** (`turnover > 10,000`).
- **Phân biệt danh sách xuất**: Bổ sung dòng ghi chú rõ ràng `// DANH SÁCH QUÉT TÍN HIỆU PRE-FOMO KUCOIN - KHUNG THỜI GIAN: 1D (hoặc 1W)`.
- **RSI Period**: 14 (Chu kỳ 14 nến).
- **Volume Ratio (Tỷ lệ Volume)**: $Turnover_{latest} / Mean_{20d} ge 2.2x$.
- **Biên độ giá**: Từ $-4.0%$ đến $+15.0%$ (nếu $le 3.0%$ gán nhãn *"Stealth Vol Gom Hàng (Chưa tăng)"*).
- **Độ trễ Coroutine delay(40)**: Tối ưu chống quá nhiệt CPU MediaTek Helio G80 trên Samsung A32.
