package com.mockrunner.app

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LatLngPoint(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
) : Serializable

data class Route(
    val id: String,
    val name: String,
    val description: String,
    val points: List<LatLngPoint>
) : Serializable

data class RunnerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentSpeedKmh: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0L,
    val currentPoint: LatLngPoint? = null
)
