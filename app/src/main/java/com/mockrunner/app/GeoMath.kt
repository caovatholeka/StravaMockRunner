package com.mockrunner.app

import kotlin.math.*
import kotlin.random.Random

object GeoMath {
    private const val EARTH_RADIUS = 6371000.0 // Bán kính Trái Đất theo mét

    /**
     * Tính khoảng cách Haversine giữa 2 điểm (mét)
     */
    fun distanceBetween(p1: LatLngPoint, p2: LatLngPoint): Double {
        val dLat = Math.toRadians(p2.lat - p1.lat)
        val dLng = Math.toRadians(p2.lng - p1.lng)
        val lat1 = Math.toRadians(p1.lat)
        val lat2 = Math.toRadians(p2.lat)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS * c
    }

    /**
     * Tính góc hướng di chuyển (Bearing: 0° - 360°) từ p1 đến p2
     */
    fun bearingBetween(p1: LatLngPoint, p2: LatLngPoint): Float {
        val lat1 = Math.toRadians(p1.lat)
        val lat2 = Math.toRadians(p2.lat)
        val dLng = Math.toRadians(p2.lng - p1.lng)

        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360.0) % 360.0).toFloat()
    }

    /**
     * Tìm tọa độ điểm tiếp theo sau khi di chuyển khoảng cách distanceMeters theo hướng bearingDegrees
     */
    fun destinationPoint(start: LatLngPoint, distanceMeters: Double, bearingDegrees: Float): LatLngPoint {
        val distRatio = distanceMeters / EARTH_RADIUS
        val bearingRad = Math.toRadians(bearingDegrees.toDouble())
        val lat1 = Math.toRadians(start.lat)
        val lng1 = Math.toRadians(start.lng)

        val lat2 = asin(sin(lat1) * cos(distRatio) + cos(lat1) * sin(distRatio) * cos(bearingRad))
        val lng2 = lng1 + atan2(
            sin(bearingRad) * sin(distRatio) * cos(lat1),
            cos(distRatio) - sin(lat1) * sin(lat2)
        )

        return LatLngPoint(Math.toDegrees(lat2), Math.toDegrees(lng2))
    }

    /**
     * Sinh vận tốc tự nhiên dao động mượt mà trong khoảng [minKmh, maxKmh].
     * Sử dụng thuật toán Random Walk có lực kéo về tâm (Mean Reverting)
     * giúp đồ thị vận tốc trên Strava trông tự nhiên như người chạy thật.
     */
    fun getNextSpeed(
        currentSpeed: Double,
        minKmh: Double = 6.0,
        maxKmh: Double = 8.0
    ): Double {
        val targetCenter = (minKmh + maxKmh) / 2.0
        val centerPull = (targetCenter - currentSpeed) * 0.1
        // Bước dao động ngẫu nhiên nhỏ từ -0.15 đến +0.15 km/h mỗi giây
        val delta = (Random.nextDouble() - 0.5) * 0.3 + centerPull
        val newSpeed = currentSpeed + delta
        return newSpeed.coerceIn(minKmh, maxKmh)
    }
}
