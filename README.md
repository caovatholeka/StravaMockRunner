# 🏃 Strava Mock Runner - Ứng dụng Mô Phỏng Di Chuyển Chạy Bộ Android

Ứng dụng mô phỏng di chuyển GPS theo lộ trình thực tế cho Android, được thiết kế chuyên biệt để hoạt động mượt mà với **Strava**, **Garmin Connect**, **Nike Run Club** mà không bị hệ thống phát hiện vị trí bất thường hay đứt quãng GPS.

---

## 🎯 Tính Năng Nổi Bật

1. **Vận tốc biến thiên tự nhiên (6.0 - 8.0 km/h)**:
   - Sử dụng thuật toán Random Walk có lực kéo hồi quy (Mean-reverting). Vận tốc dao động mượt mà từng giây thay vì nhảy đột ngột, giúp biểu đồ Pace trên Strava trông tự nhiên như người chạy thực tế.
2. **Đầy đủ tham số GPS chuẩn quân sự / dân sự**:
   - `time` & `elapsedRealtimeNanos`: Bắt buộc để Strava không coi điểm GPS là "stale" (hết hạn).
   - `accuracy`: Dao động tự nhiên từ 2.5m - 3.7m.
   - `bearing`: Hướng quay la bàn chính xác theo cung đường.
   - `altitude`: Độ cao thực tế kèm dao động nhẹ.
   - `speed`: Đơn vị m/s chính xác.
3. **Cơ chế Fake Vị Trí Kép (Dual-Layer Mock Provider)**:
   - Đẩy đồng thời vào **Google Play Services Fused Location** và **Android Native GPS Provider**, đảm bảo bất kỳ ứng dụng nào (kể cả chạy nền) cũng nhận đúng tọa độ.
4. **Foreground Service Chạy Nền Bền Bỉ**:
   - Có Notification điều khiển trên màn hình khóa. Bạn có thể mở Strava, tắt màn hình, bỏ điện thoại vào túi mà GPS vẫn tiếp tục chạy.
5. **Bản đồ tương tác**:
   - Xem lộ trình trực quan và vị trí con trỏ đang chạy từng bước theo thời gian thực.
   - Lộ trình có sẵn: Vòng quanh Hồ Hoàn Kiếm, Công viên Thống Nhất, Vòng sân điền kinh 400m (tự động lặp vòng vô hạn cho đến khi bạn bấm dừng).

---

## 🛠 Hướng Dẫn Cài Đặt Lên Điện Thoại

### Bước 1: Mở Project trong Android Studio
1. Mở **Android Studio**.
2. Chọn **Open** -> Điều hướng đến thư mục:
   `c:\Users\Admin\Documents\MockRunnerApp`
3. Chờ Android Studio đồng bộ Gradle (Sync Gradle) xong.

### Bước 2: Cắm điện thoại Android hoặc Build APK
1. **Cách 1 (Chạy trực tiếp từ Android Studio)**:
   - Cắm điện thoại Android vào máy tính qua cáp USB.
   - Trên điện thoại, bật **Gỡ lỗi USB (USB Debugging)**.
   - Trên Android Studio, chọn thiết bị của bạn và bấm nút **Run (Tam giác màu xanh)**.
2. **Cách 2 (Build file APK để cài)**:
   - Vào menu: **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
   - Khi build xong, bấm vào nút **locate** ở góc dưới bên phải để lấy file `app-debug.apk` và gửi sang điện thoại để cài đặt.

---

## 📱 Cấu Hình Bắt Buộc Trên Điện Thoại Android

Để Android cho phép ứng dụng can thiệp vào GPS:

1. **Bật Tùy chọn nhà phát triển (Developer Options)**:
   - Vào **Cài đặt (Settings)** -> **Thông tin điện thoại (About phone)** -> **Thông tin phần mềm**.
   - Chạm liên tục **7 lần** vào dòng **Số hiệu bản tạo (Build number)** cho đến khi máy báo *"Bạn đã là nhà phát triển"*.
2. **Chọn Ứng dụng Vị trí Giả lập (Mock Location App)**:
   - Quay lại Cài đặt -> Vào mục **Tùy chọn cho nhà phát triển (Developer options)**.
   - Cuộn xuống tìm dòng: **Chọn ứng dụng giả lập vị trí** (hoặc *Select mock location app*).
   - Nhấn vào và chọn: **Strava Mock Runner** (hoặc bấm nút "Mở Cài đặt nhà phát triển" ngay trong ứng dụng).

---

## 🚀 Các Bước Chạy Phối Hợp Với Strava

1. Mở app **Strava Mock Runner**.
2. Chọn **Lộ trình chạy** mong muốn (ví dụ: *Vòng quanh Hồ Hoàn Kiếm*).
3. Bấm nút **BẮT ĐẦU CHẠY**.
4. Chuyển sang ứng dụng **Strava**:
   - Vào mục **Ghi lại (Record)**.
   - Chọn loại bài tập: **Chạy (Run)** hoặc **Đi bộ (Walk)**.
   - Bạn sẽ thấy biểu tượng GPS trên Strava đã chuyển sang màu xanh lá và vị trí đang bắt đầu dịch chuyển với tốc độ ~6-8 km/h.
   - Bấm nút tròn màu cam **Bắt đầu (Start)** trên Strava.
5. Bạn có thể khóa màn hình điện thoại hoặc sử dụng các ứng dụng khác bình thường.
6. Khi muốn hoàn thành bài chạy:
   - Mở Strava -> Bấm **Dừng (Pause)** -> Bấm **Hoàn tất (Finish)** và lưu bài chạy.
   - Mở app **Strava Mock Runner** -> Bấm **KẾT THÚC**.
