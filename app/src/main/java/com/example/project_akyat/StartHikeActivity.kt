package com.example.project_akyat

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class StartHikeActivity : AppCompatActivity() {
    private lateinit var btnStartPause: Button
    private lateinit var btnStop: Button
    private lateinit var tvDuration: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvSpeedAvg: TextView
    private lateinit var tvPaceAvg: TextView
    private lateinit var tvElevation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var locationCallback: LocationCallback? = null
    private var stepDetectorSensor: Sensor? = null
    private var isTracking = false
    private var isPaused = false
    private var isManualPause = false
    private var currentSteps = 0
    private var totalDistanceKm = 0.0
    private var elevationGain = 0.0
    private var currentGpsSpeedKmh = 0.0
    private var maxSpeedKmh = 0.0
    private var fastestPaceMinPerKm = 0.0
    private var activeTimeMillis = 0L
    private var lastTickTime = 0L
    private var lastActiveTimeMillis = 0L
    private var startWallTime = 0L
    private var lastLocation: Location? = null
    private var lastAltitude: Double? = null
    private var smoothedAltitude: Double? = null
    private val maxValidSpeedKmh = 25.0
    private val handler = Handler(Looper.getMainLooper())
    private var trackingService: HikeTrackingService? = null
    private var isBound = false

    private val userWeightKg: Double
        get() = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getFloat("weight_kg", 70f)
            .toDouble()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as HikeTrackingService.LocalBinder
            trackingService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isTracking) return
            val now = SystemClock.elapsedRealtime()
            val delta = now - lastTickTime
            lastTickTime = now

            if (!isPaused) {
                activeTimeMillis += delta
                if (now - lastActiveTimeMillis > 10000 && currentGpsSpeedKmh < 1.0) {
                    pauseTracking(true)
                }
            }
            updateUI()
            trackingService?.updateNotification(tvDuration.text.toString(), tvDistance.text.toString())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_hike)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )
            insets
        }

        initializeViews()
        initializeServices()
        setupButtons()
        setupBackHandler()
    }

    private fun initializeViews() {
        btnStartPause = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        tvDuration = findViewById(R.id.tvDuration)
        tvDistance = findViewById(R.id.tvDistance)
        tvSteps = findViewById(R.id.tvSteps)
        tvCalories = findViewById(R.id.tvCalories)
        tvSpeedAvg = findViewById(R.id.tvSpeedAvg)
        tvPaceAvg = findViewById(R.id.tvPaceAvg)
        tvElevation = findViewById(R.id.tvElevation)
    }

    private fun initializeServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    }

    private fun setupButtons() {
        btnStop.visibility = android.view.View.GONE
        btnStartPause.setOnClickListener {
            when {
                !isTracking -> checkAndRequestPermissions()
                isPaused -> resumeTracking(false)
                else -> pauseTracking(false)
            }
        }

        btnStop.setOnClickListener {
            if (!isTracking) return@setOnClickListener
            stopTracking()
            navigateToSummary()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            handleBack()
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this) {
            handleBack()
        }
    }

    private fun handleBack() {
        if (!isTracking) {
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Discard Hike?")
            .setMessage("Tracking is still running. Going back will discard your hike.")
            .setPositiveButton("Discard") { _, _ ->
                stopTracking()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                startTracking()
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return

        resetTrackingData()

        isTracking = true
        isPaused = false
        isManualPause = false

        val now = SystemClock.elapsedRealtime()

        lastTickTime = now
        lastActiveTimeMillis = now
        startWallTime = System.currentTimeMillis()

        btnStartPause.text = getString(R.string.pause)
        btnStop.visibility = android.view.View.VISIBLE

        updateUI()
        handler.post(timerRunnable)
        registerSensors()
        startTrackingService()
        startLocationUpdates()

        Toast.makeText(this, "Hike started!", Toast.LENGTH_SHORT).show()
    }

    private fun resetTrackingData() {
        currentSteps = 0
        totalDistanceKm = 0.0
        elevationGain = 0.0
        currentGpsSpeedKmh = 0.0
        maxSpeedKmh = 0.0
        fastestPaceMinPerKm = 0.0
        activeTimeMillis = 0L

        lastLocation = null
        lastAltitude = null
        smoothedAltitude = null
    }

    private fun registerSensors() {
        stepDetectorSensor?.let {
            sensorManager.registerListener(
                stepListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } ?: run {
            Toast.makeText(
                this,
                "Step detector unavailable.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, HikeTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1500)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach {
                    processLocation(it)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    private fun pauseTracking(isAuto: Boolean) {
        if (isPaused) return
        isPaused = true
        isManualPause = !isAuto
        currentGpsSpeedKmh = 0.0
        btnStartPause.text = getString(R.string.resume)

        vibratePhone()

        if (!isAuto) {
            Toast.makeText(this, getString(R.string.tracking_paused), Toast.LENGTH_SHORT).show()
        }
    }

    private fun resumeTracking(isAuto: Boolean) {
        if (!isPaused) return

        isPaused = false
        isManualPause = false

        val now = SystemClock.elapsedRealtime()
        lastTickTime = now
        lastActiveTimeMillis = now
        lastLocation = null
        lastAltitude = null
        btnStartPause.text = getString(R.string.pause)

        if (!isAuto) {
            Toast.makeText(
                this,
                getString(R.string.tracking_resumed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        handler.removeCallbacks(timerRunnable)
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        locationCallback = null
        sensorManager.unregisterListener(stepListener)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopService(Intent(this, HikeTrackingService::class.java))
    }

    private fun processLocation(location: Location) {
        if (location.accuracy > 40f) return

        processElevation(location)
        processDistance(location)
    }

    private fun processElevation(location: Location) {
        if (!location.hasAltitude()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (location.hasVerticalAccuracy() && location.verticalAccuracyMeters > 15f)
                return
        }

        val currentAltitude = location.altitude
        smoothedAltitude = smoothedAltitude?.let {
            (it * 0.8) + (currentAltitude * 0.2)
        } ?: currentAltitude

        if (isPaused) {
            lastAltitude = smoothedAltitude
            return
        }

        lastAltitude?.let { previous ->
            val diff = smoothedAltitude!! - previous
            when {
                diff > 1.0 -> {
                    elevationGain += diff
                    lastAltitude = smoothedAltitude
                }

                diff < -1.0 -> {
                    lastAltitude = smoothedAltitude
                }
            }
        } ?: run {
            lastAltitude = smoothedAltitude
        }

        tvElevation.text = getString(R.string.elevation_format, elevationGain)
    }

    private fun processDistance(location: Location) {
        val previousLocation = lastLocation ?: run {
            lastLocation = location
            return
        }

        val distanceMeters = previousLocation.distanceTo(location)

        val timeDifference = (location.time - previousLocation.time).coerceAtLeast(1)
        val timeSeconds = timeDifference / 1000.0
        val calculatedSpeedKmh = (distanceMeters / timeSeconds) * 3.6

        val gpsSpeedKmh = if (location.hasSpeed()) {
            location.speed * 3.6
        } else {
            calculatedSpeedKmh
        }

        if (isPaused) {
            if (isManualPause) {
                lastLocation = location
                return
            }

            if (distanceMeters > 5.0 || gpsSpeedKmh > 1.5) {
                resumeTracking(true)
            } else {
                return
            }
        }

        if (distanceMeters < 3.0 && calculatedSpeedKmh < 2.0) {
            currentGpsSpeedKmh = 0.0
            return
        }

        if (calculatedSpeedKmh > maxValidSpeedKmh) {
            lastLocation = location
            return
        }

        currentGpsSpeedKmh = gpsSpeedKmh
        lastActiveTimeMillis = SystemClock.elapsedRealtime()
        totalDistanceKm += distanceMeters / 1000.0

        if (gpsSpeedKmh > maxSpeedKmh) {
            maxSpeedKmh = gpsSpeedKmh
        }

        val currentPace =
            if (calculatedSpeedKmh > 0.1) {
                60.0 / calculatedSpeedKmh
            } else {
                0.0
            }

        if (
            currentPace > 0 && (fastestPaceMinPerKm == 0.0 || currentPace < fastestPaceMinPerKm)
        ) {
            fastestPaceMinPerKm = currentPace
        }

        tvDistance.text = getString(R.string.distance_format, totalDistanceKm)

        lastLocation = location
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_STEP_DETECTOR || !isTracking)
                return

            if (isPaused) {
                if (isManualPause) return

                resumeTracking(true)
            }

            lastActiveTimeMillis = SystemClock.elapsedRealtime()
            currentSteps++
            tvSteps.text = getString(R.string.active_step_counter, currentSteps)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun updateUI() {
        val seconds = (activeTimeMillis / 1000) % 60
        val minutes = (activeTimeMillis / 60000) % 60
        val hours = activeTimeMillis / 3600000

        tvDuration.text = if (isPaused) {
            "PAUSED"
        } else {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }
        tvCalories.text = getString(R.string.calorie_format, calculateCalories())

        val elapsedHours = activeTimeMillis / 3600000.0
        val avgSpeed = if (elapsedHours > 0) {
                totalDistanceKm / elapsedHours
        } else {
            0.0
        }

        val avgPace = if (totalDistanceKm > 0) {
            (elapsedHours * 60) / totalDistanceKm
        } else {
            0.0
        }

        tvSpeedAvg.text = getString(R.string.avg_speed_format, avgSpeed)
        tvPaceAvg.text = formatPace(avgPace)
    }

    private fun calculateCalories(): Int {
        val activeHours = activeTimeMillis / 3600000.0

        return (4.5 * userWeightKg * activeHours).toInt()
    }

    private fun navigateToSummary() {
        val elapsedHours = activeTimeMillis / 3600000.0
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

        isoFormatter.timeZone = TimeZone.getTimeZone("UTC")

        startActivity(
            Intent(this, HikeSummaryActivity::class.java).apply {
                putExtra("duration", tvDuration.text.toString())
                putExtra("durationSeconds", (activeTimeMillis / 1000).toInt())
                putExtra("distance", totalDistanceKm)
                putExtra("steps", currentSteps)
                putExtra("calories", calculateCalories())
                putExtra("avgSpeed", if (elapsedHours > 0) totalDistanceKm / elapsedHours else 0.0)
                putExtra("maxSpeed", maxSpeedKmh)
                putExtra("avgPace", if (totalDistanceKm > 0) (elapsedHours * 60) / totalDistanceKm else 0.0)
                putExtra("bestPace", fastestPaceMinPerKm)
                putExtra("elevationGain", elevationGain)
                putExtra("startedAt", isoFormatter.format(Date(startWallTime)))
                putExtra("endedAt", isoFormatter.format(Date()))
            }
        )
        finish()

    }

    private fun formatPace(pace: Double): String {
        if (pace <= 0.0 || pace.isInfinite() || pace.isNaN()) {
            return "0'00\""
        }

        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()

        return String.format(Locale.getDefault(), "%d'%02d\"", minutes, seconds)
    }

    private fun vibratePhone() {
        try {
            val vibrator = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(400)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }
}