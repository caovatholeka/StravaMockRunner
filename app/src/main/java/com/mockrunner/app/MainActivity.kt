package com.mockrunner.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.mockrunner.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedRoute: Route = RouteManager.presetRoutes[0]
    private var isRunning = false
    private var isPaused = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MockLocationService.ACTION_STATE_UPDATE -> {
                    val speed = intent.getDoubleExtra(MockLocationService.EXTRA_STATE_SPEED, 0.0)
                    val dist = intent.getDoubleExtra(MockLocationService.EXTRA_STATE_DISTANCE, 0.0)
                    val elapsed = intent.getLongExtra(MockLocationService.EXTRA_STATE_ELAPSED, 0L)
                    val lat = intent.getDoubleExtra(MockLocationService.EXTRA_STATE_LAT, 0.0)
                    val lng = intent.getDoubleExtra(MockLocationService.EXTRA_STATE_LNG, 0.0)
                    isRunning = intent.getBooleanExtra(MockLocationService.EXTRA_STATE_RUNNING, false)
                    isPaused = intent.getBooleanExtra(MockLocationService.EXTRA_STATE_PAUSED, false)

                    updateMetricsUI(speed, dist, elapsed)
                    updateMapMarker(lat, lng)
                    updateControlButtonsState()
                }
                MockLocationService.ACTION_MOCK_PERMISSION_ERROR -> {
                    showMockPermissionDialog()
                }
            }
        }
    }

    private fun showMockPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Chưa cấp quyền Vị trí giả lập")
            .setMessage("Điện thoại chưa cho phép ứng dụng can thiệp GPS!\n\nVui lòng vào: Cài đặt cho nhà phát triển ➔ 'Chọn ứng dụng vị trí giả' ➔ Chọn 'Strava Mock Runner' để kích hoạt.")
            .setPositiveButton("Mở Cài Đặt Ngay") { _, _ ->
                openDeveloperSettings()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!fineLocationGranted) {
            Toast.makeText(this, "Cần cấp quyền vị trí chính xác để ứng dụng hoạt động", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions()
        setupWebView()
        setupRouteSpinner()
        setupActionListeners()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(MockLocationService.ACTION_STATE_UPDATE)
            addAction(MockLocationService.ACTION_MOCK_PERMISSION_ERROR)
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewMap.settings.javaScriptEnabled = true
        binding.webViewMap.settings.domStorageEnabled = true
        binding.webViewMap.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        binding.webViewMap.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadRouteOnMap(selectedRoute)
            }
        }
        binding.webViewMap.loadUrl("file:///android_asset/map.html")
    }

    private fun setupRouteSpinner() {
        val routeNames = RouteManager.presetRoutes.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, routeNames)
        binding.spinnerRoutes.adapter = adapter

        binding.spinnerRoutes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isRunning) {
                    selectedRoute = RouteManager.presetRoutes[position]
                    val lenMeters = RouteManager.calculateTotalLength(selectedRoute.points)
                    binding.tvRouteInfo.text = String.format("Chiều dài vòng lặp: %.2f km • Tự động lặp vô hạn", lenMeters / 1000.0)
                    loadRouteOnMap(selectedRoute)
                } else {
                    Toast.makeText(this@MainActivity, "Hãy kết thúc lộ trình hiện tại trước khi đổi", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadRouteOnMap(route: Route) {
        val json = Gson().toJson(route.points)
        binding.webViewMap.evaluateJavascript("displayRoute($json);", null)
    }

    private fun updateMapMarker(lat: Double, lng: Double) {
        if (lat != 0.0 && lng != 0.0) {
            binding.webViewMap.evaluateJavascript("updateRunnerPosition($lat, $lng);", null)
        }
    }

    private fun updateMetricsUI(speed: Double, distanceMeters: Double, elapsedSeconds: Long) {
        binding.tvSpeed.text = String.format("%.1f", speed)
        binding.tvDistance.text = String.format("%.2f", distanceMeters / 1000.0)
        binding.tvDuration.text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    }

    private fun updateControlButtonsState() {
        if (!isRunning) {
            binding.tvStatusBadge.text = "SẴN SÀNG"
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            binding.btnStartResume.visibility = View.VISIBLE
            binding.btnStartResume.text = getString(R.string.btn_start)
            binding.layoutRunningActions.visibility = View.GONE
            binding.spinnerRoutes.isEnabled = true
        } else if (isPaused) {
            binding.tvStatusBadge.text = "TẠM DỪNG"
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_yellow))
            binding.btnStartResume.visibility = View.GONE
            binding.layoutRunningActions.visibility = View.VISIBLE
            binding.btnPause.text = getString(R.string.btn_resume)
            binding.btnPause.setIconResource(R.drawable.ic_play)
            binding.spinnerRoutes.isEnabled = false
        } else {
            binding.tvStatusBadge.text = "ĐANG CHẠY"
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_orange))
            binding.btnStartResume.visibility = View.GONE
            binding.layoutRunningActions.visibility = View.VISIBLE
            binding.btnPause.text = getString(R.string.btn_pause)
            binding.btnPause.setIconResource(R.drawable.ic_pause)
            binding.spinnerRoutes.isEnabled = false
        }
    }

    private fun setupActionListeners() {
        binding.btnStartResume.setOnClickListener {
            startSimulation()
        }

        binding.btnPause.setOnClickListener {
            if (isPaused) {
                val intent = Intent(this, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_RESUME
                }
                startService(intent)
            } else {
                val intent = Intent(this, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_PAUSE
                }
                startService(intent)
            }
        }

        binding.btnStop.setOnClickListener {
            val intent = Intent(this, MockLocationService::class.java).apply {
                action = MockLocationService.ACTION_STOP
            }
            startService(intent)
            isRunning = false
            isPaused = false
            updateControlButtonsState()
            Toast.makeText(this, "Đã kết thúc buổi chạy", Toast.LENGTH_SHORT).show()
        }

        binding.btnOpenDevSettings.setOnClickListener {
            openDeveloperSettings()
        }

        binding.btnUseCurrentLocation.setOnClickListener {
            if (!isRunning) {
                createRouteFromCurrentLocation()
            } else {
                Toast.makeText(this, "Hãy dừng bài chạy hiện tại trước khi đổi lộ trình", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createRouteFromCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Hãy cấp quyền vị trí để lấy tọa độ hiện tại", Toast.LENGTH_SHORT).show()
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            return
        }

        Toast.makeText(this, "Đang lấy vị trí GPS của bạn...", Toast.LENGTH_SHORT).show()
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    applyCurrentLocationRoute(loc.latitude, loc.longitude)
                } else {
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            applyCurrentLocationRoute(lastLoc.latitude, lastLoc.longitude)
                        } else {
                            Toast.makeText(this, "Không lấy được vị trí GPS. Hãy kiểm tra bật Định vị trên máy", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi lấy GPS: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyCurrentLocationRoute(lat: Double, lng: Double) {
        val userPt = LatLngPoint(lat, lng)
        val localRoute = RouteManager.generateLocalLoop(userPt, 450.0)
        selectedRoute = localRoute
        val lenMeters = RouteManager.calculateTotalLength(selectedRoute.points)
        binding.tvRouteInfo.text = String.format("Vòng chạy quanh nhà bạn (~%.2f km) • Tự động lặp", lenMeters / 1000.0)
        loadRouteOnMap(selectedRoute)
        Toast.makeText(this, "Đã tạo lộ trình chạy quanh vị trí của bạn thành công!", Toast.LENGTH_SHORT).show()
    }

    private fun startSimulation() {
        val intent = Intent(this, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_START
            putExtra(MockLocationService.EXTRA_WAYPOINTS, ArrayList(selectedRoute.points))
            putExtra(MockLocationService.EXTRA_MIN_SPEED, 6.0) // 6 km/h
            putExtra(MockLocationService.EXTRA_MAX_SPEED, 8.0) // 8 km/h
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        isRunning = true
        isPaused = false
        updateControlButtonsState()
        Toast.makeText(this, "Bắt đầu mô phỏng! Hãy mở Strava và bấm Record", Toast.LENGTH_LONG).show()
    }

    private fun openDeveloperSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Hướng dẫn kích hoạt")
                .setMessage("Hãy vào Cài đặt máy -> Giới thiệu điện thoại -> Chạm 7 lần vào 'Số bản dựng' (Build Number) để bật Tùy chọn nhà phát triển. Sau đó vào Tùy chọn nhà phát triển -> 'Chọn ứng dụng vị trí giả' -> Chọn 'Strava Mock Runner'.")
                .setPositiveButton("Đã hiểu", null)
                .show()
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun onPointAdded(pointsJson: String) {
            // Callback khi người dùng vẽ điểm trên bản đồ
        }
    }
}
