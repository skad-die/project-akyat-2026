package com.example.project_akyat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HikeSummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hike_summary)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvSteps = findViewById<TextView>(R.id.tvSteps)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDiscard = findViewById<Button>(R.id.btnDiscard)

        val duration = intent.getStringExtra("duration") ?: "00:00:00"
        val distance = intent.getDoubleExtra("distance", 0.0)
        val steps = intent.getIntExtra("steps", 0)

        tvDuration.text = getString(R.string.duration_format, duration)
        tvDistance.text = getString(R.string.distance_format, distance)
        tvSteps.text = getString(R.string.active_step_counter, steps)

        btnDiscard.setOnClickListener {
            startActivity(Intent(this, StartHikeActivity::class.java))
            finish()
        }

        btnSave.setOnClickListener {
            // TODO: POST to backend
            Toast.makeText(this, "TODO: Save Hike Data!", Toast.LENGTH_SHORT).show()
        }
    }


}