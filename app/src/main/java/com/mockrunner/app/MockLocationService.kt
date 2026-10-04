package com.mockrunner.app

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import kotlin.random.Random

class MockLocationService : Service() {

    companion object {
        const val TAG = "MockLocationService"
        const val CHANNEL_ID = "mock_runner_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_WAYPOINTS = "EXTRA_WAYPOINTS"
        const val EXTRA_MIN_SPEED = "EXTRA_MIN_SPEED"
        const val EXTRA_MAX_SPEED = "EXTRA_MAX_SPEED"

        // Broadcast actions
        const val ACTION_STATE_UPDATE = "com.mockrunner.app.STATE_UPDATE"
        const val EXTRA_STATE_SPEED = "EXTRA_STATE_SPEED"
        const val EXTRA_STATE_DISTANCE = "EXTRA_STATE_DISTANCE"
        const val EXTRA_STATE_ELAPSED = "EXTRA_STATE_ELAPSED"
        const val EXTRA_STATE_LAT = "EXTRA_STATE_LAT"
        const val EXTRA_STATE_LNG = "EXTRA_STATE_LNG"
        const val EXTRA_STATE_RUNNING = "EXTRA_STATE_RUNNING"
        const val EXTRA_STATE_PAUSED = "EXTRA_STATE_PAUSED"

        var isServiceRunning = false
            private set
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var waypoints: List<LatLngPoint> = emptyList()
    private var currentWaypointIndex = 0
    private var currentPosition: LatLngPoint? = null

    private var minSpeedKmh = 6.0
    private var maxSpeedKmh = 8.0
    private var currentSpeedKmh = 6.5
    private var totalDistanceMeters = 0.0
    private var elapsedSeconds = 0L

    private var isPaused = false
    private var currentBearing = 0f

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        createNotificationChannel()
        setupMockProviders()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val points = intent.getSerializableExtra(EXTRA_WAYPOINTS) as? ArrayList<LatLngPoint>
                if (!points.isNullOrEmpty()) {
                    waypoints = points
                    minSpeedKmh = intent.getDoubleExtra(EXTRA_MIN_SPEED, 6.0)
                    maxSpeedKmh = intent.getDoubleExtra(EXTRA_MAX_SPEED, 8.0)
                    startMocking()
                }
            }
            ACTION_PAUSE -> {
                isPaused = true
                updateNotification()
                broadcastState()
            }
            ACTION_RESUME -> {
                isPaused = false
                updateNotification()
                broadcastState()
            }
            ACTION_STOP -> {
                stopMocking()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun setupMockProviders() {
        try {
            // Thiết lập Google Play Services Mock Mode
            fusedLocationClient.setMockMode(true).addOnFailureListener { e ->
                Log.e(TAG, "Lỗi bật mock mode Google Fused Location: ${e.message}")
            }

            // Thiết lập Android System GPS Test Provider
            try {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    false, false, false, false,
                    true, true, true,
                    ProviderProperties.POWER_USAGE_LOW,
                    ProviderProperties.ACCURACY_FINE
                )
            } else {
                @Suppress("DEPRECATION")
                locationManager.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    "requiresNetwork" == "",
                    "requiresSatellite" == "",
                    "requiresCell" == "",
                    "hasMonetaryCost" == "",
                    "supportsAltitude" == "",
                    "supportsSpeed" == "",
                    "supportsBearing" == "",
                    1, // Power requirement: LOW
                    1  // Accuracy: FINE
                )
            }
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            Log.d(TAG, "Đã khởi tạo xong GPS Mock Provider thành công")
        } catch (e: Exception) {
            Log.e(TAG, "Không thể khởi tạo Mock Provider: ${e.message}")
        }
    }

    private fun startMocking() {
        if (waypoints.isEmpty()) return

        isServiceRunning = true
        isPaused = false
        currentWaypointIndex = 0
        currentPosition = waypoints[0]
        currentSpeedKmh = (minSpeedKmh + maxSpeedKmh) / 2.0
        totalDistanceMeters = 0.0
        elapsedSeconds = 0L

        startForegroundService()

        serviceScope.launch {
            while (isActive && isServiceRunning) {
                if (!isPaused && currentPosition != null) {
                    tickSimulation()
                }
                delay(1000L) // 1 tick mỗi giây
            }
        }
    }

    /**
     * Vòng lặp tính toán di chuyển mỗi 1 giây
     */
    private fun tickSimulation() {
        elapsedSeconds++

        // 1. Tính toán vận tốc ngẫu nhiên tự nhiên (6 - 8 km/h)
        currentSpeedKmh = GeoMath.getNextSpeed(currentSpeedKmh, minSpeedKmh, maxSpeedKmh)
        val speedMps = currentSpeedKmh / 3.6 // Chuyển sang m/s
        val stepDistance = speedMps * 1.0   // Quãng đường đi trong 1s

        totalDistanceMeters += stepDistance

        // 2. Di chuyển điểm hiện tại tới waypoint tiếp theo
        moveStepAlongRoute(stepDistance)

        // 3. Tạo vị trí GPS mô phỏng chân thực và gửi vào hệ thống Android
        currentPosition?.let { pos ->
            pushMockLocation(pos, speedMps.toFloat(), currentBearing)
        }

        // 4. Cập nhật giao diện & thông báo
        updateNotification()
        broadcastState()
    }

    private fun moveStepAlongRoute(stepDist: Double) {
        if (waypoints.size < 2) return

        var remainingDist = stepDist
        var pos = currentPosition ?: waypoints[0]

        while (remainingDist > 0) {
            val targetWaypoint = waypoints[(currentWaypointIndex + 1) % waypoints.size]
            val distToTarget = GeoMath.distanceBetween(pos, targetWaypoint)

            if (distToTarget <= 0.5) {
                // Đã tới gần sát waypoint, chuyển sang điểm kế tiếp
                pos = targetWaypoint
                currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size
                continue
            }

            currentBearing = GeoMath.bearingBetween(pos, targetWaypoint)

            if (remainingDist < distToTarget) {
                // Đi một phần quãng đường trên đoạn thẳng này
                pos = GeoMath.destinationPoint(pos, remainingDist, currentBearing)
                remainingDist = 0.0
            } else {
                // Vượt qua waypoint hiện tại, tiếp tục đoạn tiếp
                remainingDist -= distToTarget
                pos = targetWaypoint
                currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size
            }
        }
        currentPosition = pos
    }

    @SuppressLint("MissingPermission")
    private fun pushMockLocation(point: LatLngPoint, speedMps: Float, bearing: Float) {
        val mockLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = point.lat
            longitude = point.lng
            altitude = 12.5 + (Random.nextDouble() - 0.5) * 0.4 // Độ cao thực tế
            speed = speedMps
            this.bearing = bearing
            accuracy = 2.5f + Random.nextFloat() * 1.2f // Độ chính xác cực nét (2.5 - 3.7m)
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bearingAccuracyDegrees = 1.0f
                speedAccuracyMetersPerSecond = 0.15f
                verticalAccuracyMeters = 1.2f
            }
        }

        try {
            // Đẩy vào Google Play Services Fused Location
            fusedLocationClient.setMockLocation(mockLocation)
            // Đẩy vào Android Native GPS Provider
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi nạp mock location: ${e.message}")
        }
    }

    private fun startForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, MockLocationService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, MockLocationService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 2, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val kmStr = String.format("%.2f km", totalDistanceMeters / 1000.0)
        val speedStr = String.format("%.1f km/h", currentSpeedKmh)
        val timeStr = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)

        val statusText = if (isPaused) "Đang tạm dừng | $kmStr" else "$speedStr | $kmStr | $timeStr"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Strava Mock Runner")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Tiếp tục" else "Tạm dừng",
                pauseResumePendingIntent
            )
            .addAction(android.R.drawable.ic_delete, "Dừng hẳn", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun broadcastState() {
        val intent = Intent(ACTION_STATE_UPDATE).apply {
            putExtra(EXTRA_STATE_SPEED, currentSpeedKmh)
            putExtra(EXTRA_STATE_DISTANCE, totalDistanceMeters)
            putExtra(EXTRA_STATE_ELAPSED, elapsedSeconds)
            putExtra(EXTRA_STATE_LAT, currentPosition?.lat ?: 0.0)
            putExtra(EXTRA_STATE_LNG, currentPosition?.lng ?: 0.0)
            putExtra(EXTRA_STATE_RUNNING, isServiceRunning)
            putExtra(EXTRA_STATE_PAUSED, isPaused)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    @SuppressLint("MissingPermission")
    private fun stopMocking() {
        isServiceRunning = false
        isPaused = false
        try {
            fusedLocationClient.setMockMode(false)
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopMocking()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
