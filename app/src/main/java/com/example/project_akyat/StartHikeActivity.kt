package com.example.project_akyat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import java.util.Locale

class StartHikeActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null

    private lateinit var btnStartPause: Button
    private lateinit var btnStop: Button
    private lateinit var tvDuration: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvSpeedAvg: TextView
    private lateinit var tvPaceAvg: TextView
    private lateinit var tvElevation: TextView

    private var lastLocation: Location? = null
    private val handler = Handler(Looper.getMainLooper())

    private var activeTimeMillis = 0L
    private var lastTickTime = 0L
    private var lastActiveTimeMillis = 0L

    private var totalDistanceKm = 0.0
    private var maxSpeedKmh = 0.0
    private var bestPaceMinPerKm = 0.0
    private var currentGpsSpeedKmh = 0.0

    private var isTracking = false
    private var isPaused = false
    private var isManualPause = false
    private var currentSteps = 0
    private var lastAltitude: Double? = null
    private var smoothedAltitude: Double? = null
    private var elevationGain = 0.0

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isTracking) return

            val now = SystemClock.elapsedRealtime()
            val delta = now - lastTickTime
            lastTickTime = now

            if (!isPaused) {
                activeTimeMillis += delta

                // Auto-Pause check: 10 seconds of complete inactivity
                val inactiveDuration = now - lastActiveTimeMillis
                if (inactiveDuration > 10000) {
                    pauseTracking(isAuto = true)
                }
            }

            val seconds = (activeTimeMillis / 1000) % 60
            val minutes = (activeTimeMillis / 60000) % 60
            val hours = activeTimeMillis / 3600000
            tvDuration.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

            val calories = calculateCalories()
            tvCalories.text = getString(R.string.calorie_format, calories)

            val elapsedHours = activeTimeMillis / 3600000.0
            val averageSpeed = if (elapsedHours > 0) totalDistanceKm / elapsedHours else 0.0
            val averagePace = if (totalDistanceKm > 0) (elapsedHours * 60) / totalDistanceKm else 0.0

            tvSpeedAvg.text = getString(R.string.avg_speed_format, averageSpeed)

            if (isPaused) {
                tvPaceAvg.text = getString(R.string.pause)
            } else {
                tvPaceAvg.text = formatPace(averagePace)
            }

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_hike)

        findViewById<View>(R.id.main)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        bindViews()
        setupButtons()
    }

    private fun bindViews() {
        tvDuration = findViewById(R.id.tvDuration)
        tvDistance = findViewById(R.id.tvDistance)
        tvSteps = findViewById(R.id.tvSteps)
        tvCalories = findViewById(R.id.tvCalories)
        tvSpeedAvg = findViewById(R.id.tvSpeedAvg)
        tvPaceAvg = findViewById(R.id.tvPaceAvg)
        tvElevation = findViewById(R.id.tvElevation)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    }

    private fun setupButtons() {
        btnStartPause = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        findViewById<Button>(R.id.btnPause)?.visibility = View.GONE
        btnStop.visibility = View.GONE

        btnStartPause.setOnClickListener {
            if (!isTracking) {
                checkAndRequestPermissions()
            } else {
                if (isPaused) {
                    resumeTracking(isAuto = false)
                } else {
                    pauseTracking(isAuto = false)
                }
            }
        }

        btnStop.setOnClickListener {
            if (!isTracking) return@setOnClickListener
            stopTracking()
            navigateToSummary()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (fineLocationGranted) {
                startTracking()
            } else {
                Toast.makeText(this, "Location permission denied. Cannot track hike.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        isTracking = true
        isPaused = false
        isManualPause = false

        lastLocation = null
        lastAltitude = null
        smoothedAltitude = null
        totalDistanceKm = 0.0
        maxSpeedKmh = 0.0
        bestPaceMinPerKm = 0.0
        currentSteps = 0
        currentGpsSpeedKmh = 0.0
        elevationGain = 0.0

        activeTimeMillis = 0L
        val now = SystemClock.elapsedRealtime()
        lastTickTime = now
        lastActiveTimeMillis = now

        // UI Reset
        tvDistance.text = getString(R.string.distance_format, 0.0)
        tvSteps.text = getString(R.string.active_step_counter_default)
        tvCalories.text = getString(R.string.active_calorie_counter_default)
        tvSpeedAvg.text = getString(R.string.avg_speed_format, 0.0)
        tvPaceAvg.text = formatPace(0.0)
        tvElevation.text = getString(R.string.elevation_format, 0.0)

        btnStartPause.text = getString(R.string.pause)
        btnStop.visibility = View.VISIBLE

        handler.post(timerRunnable)

        stepDetectorSensor?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: Toast.makeText(this, "Step counter not available", Toast.LENGTH_SHORT).show()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    handleLocation(location)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        Toast.makeText(this, "Hike started!", Toast.LENGTH_SHORT).show()

    }

    private fun handleLocation(location: Location) {
        if (location.accuracy > 50f) return

        handleElevation(location)
        handleDistanceAndSpeed(location)
    }

    private fun handleElevation(location: Location) {
        if (!location.hasAltitude()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (location.hasVerticalAccuracy() && location.verticalAccuracyMeters > 15f) return
        }

        val currentAltitude = location.altitude

        smoothedAltitude = smoothedAltitude?.let { (it * 0.8) + (currentAltitude * 0.2) } ?: currentAltitude

        if (isPaused) {
            lastAltitude = smoothedAltitude
            return
        }

        lastAltitude?.let { last ->
            val diff = smoothedAltitude!! - last

            if (diff > 1.0) {
                elevationGain += diff
                lastAltitude = smoothedAltitude
                tvElevation.text = getString(R.string.elevation_format, elevationGain)
            } else if (diff < -1.0) {
                lastAltitude = smoothedAltitude
            }
        } ?: run {
            lastAltitude = smoothedAltitude
        }
    }

    private fun handleDistanceAndSpeed(location: Location) {
        val last = lastLocation ?: run {
            lastLocation = location
            return
        }

        val distanceMeters = last.distanceTo(location)
        val timeSec = (location.time - last.time) / 1000.0

        if (timeSec <= 0.0) return

        val calculatedSpeedKmh = (distanceMeters / timeSec) * 3.6
        val gpsSpeedKmh = if (location.hasSpeed()) location.speed * 3.6 else calculatedSpeedKmh

        if (isPaused) {
            if (isManualPause) {
                lastLocation = location
                return
            } else {
                if (distanceMeters > 3.0 || gpsSpeedKmh > 1.5) {
                    resumeTracking(isAuto = true)
                } else {
                    return
                }
            }
        }

        if (distanceMeters < 1.5 && calculatedSpeedKmh < 1.0) {
            currentGpsSpeedKmh = 0.0
            return
        }

        if (calculatedSpeedKmh > 25.0) {
            lastLocation = location
            return
        }
        lastActiveTimeMillis = SystemClock.elapsedRealtime()
        currentGpsSpeedKmh = gpsSpeedKmh
        totalDistanceKm += (distanceMeters / 1000.0)

        if (gpsSpeedKmh > maxSpeedKmh && gpsSpeedKmh <= 35.0) maxSpeedKmh = gpsSpeedKmh

        val currentPace = if (calculatedSpeedKmh > 0.1) 60.0 / calculatedSpeedKmh else 0.0
        if (currentPace > 0 && (bestPaceMinPerKm == 0.0 || currentPace < bestPaceMinPerKm)) {
            bestPaceMinPerKm = currentPace
        }

        tvDistance.text = getString(R.string.distance_format, totalDistanceKm)
        lastLocation = location
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                if (!isTracking) return

                if (isPaused) {
                    if (!isManualPause) {
                        resumeTracking(isAuto = true)
                    } else {
                        return
                    }
                }

                lastActiveTimeMillis = SystemClock.elapsedRealtime()
                currentSteps++
                tvSteps.text = getString(R.string.active_step_counter, currentSteps)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
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

        if (!isAuto && isManualPause) {
            lastLocation = null
            lastAltitude = null
        }

        isPaused = false
        isManualPause = false
        lastActiveTimeMillis = SystemClock.elapsedRealtime()

        btnStartPause.text = getString(R.string.pause)

        if (!isAuto) {
            Toast.makeText(this, getString(R.string.tracking_resumed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null

        stepDetectorSensor?.let { sensorManager.unregisterListener(stepListener) }
        handler.removeCallbacks(timerRunnable)
    }

    private fun calculateCalories(): Int {
        val averageWeightKg = 70.0
        val averageHikingMET = 4.5
        val activeHours = activeTimeMillis / 3600000.0
        return (averageHikingMET * averageWeightKg * activeHours).toInt()
    }

    private fun navigateToSummary() {
        val elapsedHours = activeTimeMillis / 3600000.0
        val overallAvgSpeedKmh = if (elapsedHours > 0) totalDistanceKm / elapsedHours else 0.0
        val overallAvgPaceMinPerKm = if (totalDistanceKm > 0) (elapsedHours * 60) / totalDistanceKm else 0.0

        val endTime = System.currentTimeMillis()
        val startTime = endTime - activeTimeMillis
        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
        isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val intent = Intent(this, HikeSummaryActivity::class.java).apply {
            putExtra("duration", tvDuration.text.toString())
            putExtra("durationSeconds", (activeTimeMillis / 1000).toInt())
            putExtra("distance", totalDistanceKm)
            putExtra("steps", currentSteps)
            putExtra("calories", calculateCalories())
            putExtra("avgSpeed", overallAvgSpeedKmh)
            putExtra("maxSpeed", maxSpeedKmh)
            putExtra("avgPace", overallAvgPaceMinPerKm)
            putExtra("bestPace", bestPaceMinPerKm)
            putExtra("elevationGain", elevationGain)
            putExtra("startedAt", isoFormat.format(java.util.Date(startTime)))
            putExtra("endedAt", isoFormat.format(java.util.Date(endTime)))
        }

        startActivity(intent)
        finish()
    }

    private fun formatPace(pace: Double): String {
        if (pace <= 0.0 || pace.isInfinite() || pace.isNaN()) return "0'00\""
        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()
        return String.format(Locale.getDefault(), "%d'%02d\"", minutes, seconds)
    }

    private fun vibratePhone() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
                )
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