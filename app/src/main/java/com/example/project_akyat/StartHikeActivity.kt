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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import java.util.Locale
import kotlin.math.*

class StartHikeActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var tvDuration: TextView
    private lateinit var tvDistance: TextView

    private val locations = mutableListOf<Location>()
    private val handler = Handler(Looper.getMainLooper())

    private var startTimeMillis = 0L
    private var totalDistanceKm = 0.0
    private var isTracking = false
    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null
    private var currentSteps = 0
    private lateinit var tvSteps: TextView

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTimeMillis

            val seconds = (elapsed / 1000) % 60
            val minutes = (elapsed / 60000) % 60
            val hours = elapsed / 3600000

            tvDuration.text = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hours, minutes, seconds
            )

            handler.postDelayed(this, 1000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_hike)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvDuration = findViewById(R.id.tvDuration)
        tvDistance = findViewById(R.id.tvDistance)
        tvSteps = findViewById(R.id.tvSteps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            if (isTracking) return@setOnClickListener

            Toast.makeText(this, "Hike started!", Toast.LENGTH_SHORT).show()
            checkPermissionAndStart()
        }

        btnStop.setOnClickListener {
            if (!isTracking) return@setOnClickListener

            stopTracking()

            val intent = Intent(this, HikeSummaryActivity::class.java).apply {
                putExtra("duration", tvDuration.text.toString())
                putExtra("distance", totalDistanceKm)
                putExtra("steps", currentSteps)
            }

            startActivity(intent)
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionAndStart() {
        val permissions = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 100)
        } else {
            startTracking()
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            val locationGranted = grantResults.getOrNull(
                permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ) == PackageManager.PERMISSION_GRANTED

            if (locationGranted) {
                startTracking()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun startTracking() {
        locations.clear()
        totalDistanceKm = 0.0
        tvDistance.text = getString(R.string.distance_format, 0.0)
        tvSteps.text = getString(R.string.active_step_counter_default)
        startTimeMillis = System.currentTimeMillis()
        handler.post(timerRunnable)

        currentSteps = 0
        stepDetectorSensor?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: Toast.makeText(this, "Step counter not available on this device", Toast.LENGTH_SHORT).show()


        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    handleLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        isTracking = true
    }

    private fun handleLocation(location: Location) {
        if (locations.isNotEmpty()) {
            val last = locations.last()

            val segmentDistance = haversineDistance(
                last.latitude,
                last.longitude,
                location.latitude,
                location.longitude
            )

            // FILTER GPS NOISE (ignore < 1 meter)
            if (segmentDistance >= 0.001) {
                totalDistanceKm += segmentDistance
                tvDistance.text = getString(R.string.distance_format, totalDistanceKm)
            }
        }

        locations.add(location)
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (stepDetectorSensor != null) sensorManager.unregisterListener(stepListener)
        handler.removeCallbacks(timerRunnable)
        isTracking = false
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }

    private fun haversineDistance(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): Double {

        val earthRadiusKm = 6371.0

        val deltaLatRad = Math.toRadians(endLat - startLat)
        val deltaLonRad = Math.toRadians(endLon - startLon)

        val startLatRad = Math.toRadians(startLat)
        val endLatRad = Math.toRadians(endLat)

        val a = sin(deltaLatRad / 2).pow(2) +
                cos(startLatRad) *
                cos(endLatRad) *
                sin(deltaLonRad / 2).pow(2)

        val centralAngle = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * centralAngle
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                currentSteps++
                tvSteps.text = getString(R.string.active_step_counter, currentSteps)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}