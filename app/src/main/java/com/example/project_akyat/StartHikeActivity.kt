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
import kotlin.math.abs

class StartHikeActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null

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
    private var lastStepTimeMillis = 0L

    private var totalDistanceKm = 0.0
    private var maxSpeedKmh = 0.0
    private var bestPaceMinPerKm = 0.0
    private var currentGpsSpeedKmh = 0.0

    private var isTracking = false
    private var currentSteps = 0
    private var lastAltitude: Double? = null
    private var elevationGain = 0.0
    private var elevationLoss = 0.0

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isTracking) return

            val now = System.currentTimeMillis()
            val delta = now - lastTickTime
            lastTickTime = now

            val isStepping = (now - lastStepTimeMillis) < 4000
            val isMovingGps = currentGpsSpeedKmh >= 0.2

            if (isStepping || isMovingGps) {
                activeTimeMillis += delta
            } else {
                tvSpeedAvg.text = getString(R.string.avg_speed_format, 0.0)
                tvPaceAvg.text = formatPace(0.0)
            }

            val seconds = (activeTimeMillis / 1000) % 60
            val minutes = (activeTimeMillis / 60000) % 60
            val hours = activeTimeMillis / 3600000

            tvDuration.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

            val calories = calculateCalories()
            tvCalories.text = getString(R.string.calorie_format, calories)

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
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            if (isTracking) return@setOnClickListener
            checkAndRequestPermissions()
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
            val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true
            } else {
                true
            }

            if (fineLocationGranted) {
                Toast.makeText(this, "Hike started!", Toast.LENGTH_SHORT).show()
                startTracking()
                if (!activityGranted) {
                    Toast.makeText(this, "Step tracking disabled", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Location permission denied. Cannot track hike.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        isTracking = true
        lastLocation = null
        lastAltitude = null
        totalDistanceKm = 0.0
        maxSpeedKmh = 0.0
        bestPaceMinPerKm = 0.0
        currentSteps = 0
        currentGpsSpeedKmh = 0.0
        elevationGain = 0.0
        elevationLoss = 0.0

        activeTimeMillis = 0L
        lastTickTime = System.currentTimeMillis()
        lastStepTimeMillis = System.currentTimeMillis()

        tvDistance.text = getString(R.string.distance_format, 0.0)
        tvSteps.text = getString(R.string.active_step_counter_default)
        tvCalories.text = getString(R.string.active_calorie_counter_default)
        tvSpeedAvg.text = getString(R.string.avg_speed_format, 0.0)
        tvPaceAvg.text = formatPace(0.0)
        tvElevation.text = getString(R.string.elevation_format, 0.0, 0.0)
        handler.post(timerRunnable)

        stepDetectorSensor?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: Toast.makeText(this, "Step counter not available on this device", Toast.LENGTH_SHORT).show()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    handleLocation(location)
                }
            }
        }

        locationCallback?.let { fusedLocationClient.requestLocationUpdates(request, it, Looper.getMainLooper()) }
    }

    private fun handleLocation(location: Location) {

        if (location.accuracy > 35f) return

        val last = lastLocation ?: run {
            lastLocation = location
            return
        }

        if (location.hasAltitude()) {
            val currentAltitude = location.altitude

            lastAltitude?.let { previousAltitude ->
                val diff = currentAltitude - previousAltitude

                if (abs(diff) > 3.0) {
                    if (diff > 0) {
                        elevationGain += diff
                    } else {
                        elevationLoss += abs(diff)
                    }

                    tvElevation.text = getString(R.string.elevation_format, elevationGain, elevationLoss)
                }
            }

            lastAltitude = currentAltitude
        }

        val distanceMeters = last.distanceTo(location)
        val timeSec = (location.time - last.time) / 1000.0

        if (timeSec < 1.0) {
            lastLocation = location
            return
        }

        val calculatedSpeedKmh = (distanceMeters / timeSec) * 3.6

        val gpsSpeedKmh = if (location.hasSpeed()) {
            location.speed * 3.6
        } else {
            calculatedSpeedKmh
        }

        if (distanceMeters < 1.5 && calculatedSpeedKmh < 1.0) {
            currentGpsSpeedKmh = 0.0
            lastLocation = location
            return
        }

        if (calculatedSpeedKmh in 0.2..25.0) {
            totalDistanceKm += (distanceMeters / 1000.0)
            currentGpsSpeedKmh = gpsSpeedKmh

            if (gpsSpeedKmh > maxSpeedKmh && gpsSpeedKmh <= 35.0) {
                maxSpeedKmh = gpsSpeedKmh
            }

            val currentPace = if (calculatedSpeedKmh > 0.1) 60.0 / calculatedSpeedKmh else 0.0

            if (currentPace > 0 && (bestPaceMinPerKm == 0.0 || currentPace < bestPaceMinPerKm)) {
                bestPaceMinPerKm = currentPace
            }

            tvDistance.text = getString(R.string.distance_format, totalDistanceKm)

            val elapsedHours = activeTimeMillis / 3600000.0

            val averageSpeed = if (elapsedHours > 0) {
                totalDistanceKm / elapsedHours
            } else {
                0.0
            }

            tvSpeedAvg.text = getString(R.string.avg_speed_format, averageSpeed)
            tvPaceAvg.text = formatPace(currentPace)

            lastLocation = location

        } else if (calculatedSpeedKmh > 25.0) {
            lastLocation = location
        }
    }

    private fun calculateCalories(): Int {
        val averageWeightKg = 70.0
        val averageHikingMET = 4.5
        val activeHours = activeTimeMillis / 3600000.0
        val caloriesBurned = averageHikingMET * averageWeightKg * activeHours

        return caloriesBurned.toInt()
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null

        if (stepDetectorSensor != null) {
            sensorManager.unregisterListener(stepListener)
        }
        handler.removeCallbacks(timerRunnable)
    }

    private fun navigateToSummary() {
        val elapsedHours = activeTimeMillis / 3600000.0
        val overallAvgSpeedKmh = if (elapsedHours > 0) totalDistanceKm / elapsedHours else 0.0
        val overallAvgPaceMinPerKm = if (totalDistanceKm > 0) (elapsedHours * 60) / totalDistanceKm else 0.0

        val intent = Intent(this, HikeSummaryActivity::class.java).apply {
            putExtra("duration", tvDuration.text.toString())
            putExtra("distance", totalDistanceKm)
            putExtra("steps", currentSteps)
            putExtra("calories", calculateCalories())
            putExtra("avgSpeed", overallAvgSpeedKmh)
            putExtra("maxSpeed", maxSpeedKmh)
            putExtra("avgPace", formatPace(overallAvgPaceMinPerKm))
            putExtra("bestPace", formatPace(bestPaceMinPerKm))
            putExtra("elevationGain", elevationGain)
            putExtra("elevationLoss", elevationLoss)
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

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                currentSteps++
                lastStepTimeMillis = System.currentTimeMillis()
                tvSteps.text = getString(R.string.active_step_counter, currentSteps)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}