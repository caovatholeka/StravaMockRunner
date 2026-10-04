package com.mockrunner.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RouteManager {

    /**
     * Danh sách các lộ trình có sẵn
     */
    val presetRoutes: List<Route> = listOf(
        Route(
            id = "hoan_kiem_lake",
            name = "Vòng quanh Hồ Hoàn Kiếm (~1.7 km)",
            description = "Lộ trình chạy bộ quanh bờ hồ trung tâm Hà Nội",
            points = listOf(
                LatLngPoint(21.02875, 105.85235), // Đinh Tiên Hoàng (Bưu điện)
                LatLngPoint(21.03050, 105.85330), // Tượng đài Cảm tử
                LatLngPoint(21.03180, 105.85300), // Cầu Thê Húc
                LatLngPoint(21.03150, 105.85150), // Đinh Tiên Hoàng - Hàng Đào
                LatLngPoint(21.02980, 105.85050), // Lê Thái Tổ (nhà hàng Thủy Tạ)
                LatLngPoint(21.02780, 105.85070), // Lê Thái Tổ
                LatLngPoint(21.02650, 105.85120), // Ngã ba Hàng Khay - Tràng Thi
                LatLngPoint(21.02680, 105.85320), // Hàng Khay
                LatLngPoint(21.02875, 105.85235)  // Về lại điểm xuất phát
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
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Tính tổng chiều dài lộ trình (mét)
     */
    fun calculateTotalLength(points: List<LatLngPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += GeoMath.distanceBetween(points[i], points[i + 1])
        }
        return total
    }
}
