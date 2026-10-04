package com.mockrunner.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.cos
import kotlin.math.sin

object RouteManager {

    /**
     * Tự động tạo một vòng chạy khép kín hình đa giác tự nhiên quanh vị trí hiện tại
     * @param center Điểm xuất phát (vị trí GPS thực tế của người dùng)
     * @param radiusMeters Bán kính vòng chạy (mặc định ~400m tạo thành vòng tròn chu vi ~2.5km)
     */
    fun generateLocalLoop(center: LatLngPoint, radiusMeters: Double = 400.0): Route {
        val points = mutableListOf<LatLngPoint>()
        // Tạo 12 điểm mốc tạo thành 1 cung đường chạy quanh khu phố
        val numPoints = 12
        for (i in 0 until numPoints) {
            val angle = 2.0 * Math.PI * i / numPoints
            // Thêm độ biến thiên nhẹ để cung đường uốn lượn tự nhiên thay vì tròn xoe
            val naturalRadius = radiusMeters * (0.85 + (i % 3) * 0.1)
            
            // Độ dịch chuyển theo lat/lng
            val dLat = (naturalRadius * cos(angle)) / 111320.0
            val dLng = (naturalRadius * sin(angle)) / (111320.0 * cos(Math.toRadians(center.lat)))
            
            points.add(LatLngPoint(center.lat + dLat, center.lng + dLng))
        }
        // Điểm kết thúc trùng điểm xuất phát để tạo vòng lặp khép kín
        points.add(points[0])

        val totalLengthKm = calculateTotalLength(points) / 1000.0
        return Route(
            id = "current_location_loop",
            name = String.format("Vòng chạy tại vị trí hiện tại (~%.1f km)", totalLengthKm),
            description = "Tự động tạo lộ trình khép kín quanh khu vực bạn đang đứng",
            points = points
        )
    }

    /**
     * Danh sách các lộ trình có sẵn
     */
    val presetRoutes: List<Route> = listOf(
        Route(
            id = "hoan_kiem_lake",
            name = "Vòng quanh Hồ Hoàn Kiếm (~1.7 km)",
            description = "Lộ trình chạy bộ quanh bờ hồ trung tâm Hà Nội",
            points = listOf(
                LatLngPoint(21.02875, 105.85235),
                LatLngPoint(21.03050, 105.85330),
                LatLngPoint(21.03180, 105.85300),
                LatLngPoint(21.03150, 105.85150),
                LatLngPoint(21.02980, 105.85050),
                LatLngPoint(21.02780, 105.85070),
                LatLngPoint(21.02650, 105.85120),
                LatLngPoint(21.02680, 105.85320),
                LatLngPoint(21.02875, 105.85235)
            )
        ),
        Route(
            id = "thong_nhat_park",
            name = "Vòng quanh Công viên Thống Nhất (~2.1 km)",
            description = "Lộ trình chạy khép kín quanh hồ Bảy Mẫu",
            points = listOf(
                LatLngPoint(21.01520, 105.84500),
                LatLngPoint(21.01800, 105.84550),
                LatLngPoint(21.01850, 105.84200),
                LatLngPoint(21.01500, 105.83900),
                LatLngPoint(21.01250, 105.84150),
                LatLngPoint(21.01350, 105.84450),
                LatLngPoint(21.01520, 105.84500)
            )
        ),
        Route(
            id = "stadium_track",
            name = "Đường piste sân vận động 400m",
            description = "Lộ trình vòng tròn sân điền kinh 400 mét tiêu chuẩn",
            points = listOf(
                LatLngPoint(21.02050, 105.76400),
                LatLngPoint(21.02130, 105.76400),
                LatLngPoint(21.02170, 105.76440),
                LatLngPoint(21.02170, 105.76480),
                LatLngPoint(21.02130, 105.76520),
                LatLngPoint(21.02050, 105.76520),
                LatLngPoint(21.02010, 105.76480),
                LatLngPoint(21.02010, 105.76440),
                LatLngPoint(21.02050, 105.76400)
            )
        )
    )

    fun parseCustomRoute(json: String): List<LatLngPoint> {
        return try {
            val type = object : TypeToken<List<LatLngPoint>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun calculateTotalLength(points: List<LatLngPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += GeoMath.distanceBetween(points[i], points[i + 1])
        }
        return total
    }
}
