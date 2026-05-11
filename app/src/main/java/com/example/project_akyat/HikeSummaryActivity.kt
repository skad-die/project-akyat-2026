package com.example.project_akyat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.project_akyat.model.local.HikeEntity
import com.example.project_akyat.viewmodel.HikeSummaryViewModel

class HikeSummaryActivity : AppCompatActivity() {
    private lateinit var tvDuration: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvSpeedAvg: TextView
    private lateinit var tvSpeedMax: TextView
    private lateinit var tvPaceAvg: TextView
    private lateinit var tvPaceMax: TextView
    private lateinit var tvElevation: TextView

    private val viewModel: HikeSummaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hike_summary)

        findViewById<View>(R.id.main)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        bindViews()
        displaySummary()
        setupButtons()
    }

    private fun bindViews() {
        tvDuration = findViewById(R.id.tvDuration)
        tvDistance = findViewById(R.id.tvDistance)
        tvSteps = findViewById(R.id.tvSteps)
        tvCalories = findViewById(R.id.tvCalories)
        tvSpeedAvg = findViewById(R.id.tvSpeedAvg)
        tvSpeedMax = findViewById(R.id.tvSpeedMax)
        tvPaceAvg = findViewById(R.id.tvPaceAvg)
        tvPaceMax = findViewById(R.id.tvPaceMax)
        tvElevation = findViewById(R.id.tvElevation)
    }

    private fun displaySummary() {
        val duration = intent.getStringExtra("duration") ?: "00:00:00"
        val distance = intent.getDoubleExtra("distance", 0.0)
        val steps = intent.getIntExtra("steps", 0)
        val calories = intent.getIntExtra("calories", 0)
        val avgSpeed = intent.getDoubleExtra("avgSpeed", 0.0)
        val maxSpeed = intent.getDoubleExtra("maxSpeed", 0.0)
        val avgPace = intent.getStringExtra("avgPace") ?: "0'00\""
        val bestPace = intent.getStringExtra("bestPace") ?: "0'00\""
        val elevationGain = intent.getDoubleExtra("elevationGain", 0.0)

        tvDuration.text = getString(R.string.duration_format, duration)
        tvDistance.text = getString(R.string.distance_format, distance)
        tvSteps.text = getString(R.string.active_step_counter, steps)
        tvCalories.text = getString(R.string.calorie_format, calories)
        tvSpeedAvg.text = getString(R.string.summary_speed_avg, avgSpeed)
        tvSpeedMax.text = getString(R.string.summary_speed_max, maxSpeed)
        tvPaceAvg.text = getString(R.string.summary_pace_avg, avgPace)
        tvPaceMax.text = getString(R.string.summary_pace_max, bestPace)
        tvElevation.text = getString(R.string.elevation_format, elevationGain)
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnDiscard).setOnClickListener {
            startActivity(Intent(this, StartHikeActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val hike = buildHikeFromIntent()
            viewModel.saveHike(hike)
            Toast.makeText(this, "Hike saved!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StartHikeActivity::class.java))
            finish()
        }
    }

    private fun buildHikeFromIntent(): HikeEntity {
        return HikeEntity(
            durationSeconds = intent.getIntExtra("durationSeconds", 0),
            distanceKm      = intent.getDoubleExtra("distance", 0.0),
            steps           = intent.getIntExtra("steps", 0),
            calories        = intent.getIntExtra("calories", 0),
            avgKmh          = intent.getDoubleExtra("avgSpeed", 0.0),
            maxKmh          = intent.getDoubleExtra("maxSpeed", 0.0),
            avgMinPerKm     = intent.getDoubleExtra("avgPace", 0.0),
            bestMinPerKm    = intent.getDoubleExtra("bestPace", 0.0),
            gainMeters      = intent.getDoubleExtra("elevationGain", 0.0),
            minMeters       = intent.getDoubleExtra("minElevation", 0.0),
            maxMeters       = intent.getDoubleExtra("maxElevation", 0.0),
            startedAt       = intent.getStringExtra("startedAt") ?: "",
            endedAt         = intent.getStringExtra("endedAt") ?: "",
            synced          = false
        )
    }
}