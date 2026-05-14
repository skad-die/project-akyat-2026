package com.example.project_akyat.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.project_akyat.StartHikeActivity
import com.example.project_akyat.R
import com.example.project_akyat.model.HikeRepository
import com.example.project_akyat.model.local.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<FloatingActionButton>(R.id.fabAddHike).setOnClickListener {
            startActivity(Intent(requireContext(), StartHikeActivity::class.java))
        }

        val repo = HikeRepository(AppDatabase.getInstance(requireContext()).hikeDao())

        viewLifecycleOwner.lifecycleScope.launch {
            val hikes = repo.allHikes.first()
            val latest = hikes.firstOrNull()

            if (latest != null) {
                view.findViewById<TextView>(R.id.tvLatestDate).text = latest.startedAt
                view.findViewById<TextView>(R.id.tvLatestDistance).text = "%.2f".format(latest.distanceKm)
                view.findViewById<TextView>(R.id.tvLatestDuration).text = formatDuration(latest.durationSeconds)
                view.findViewById<TextView>(R.id.tvLatestCalories).text = latest.calories.toString()
                view.findViewById<TextView>(R.id.tvLatestElevation).text = "%.0f".format(latest.gainMeters)
                view.findViewById<TextView>(R.id.tvAvgSpeed).text = "%.1f".format(latest.avgKmh)
                view.findViewById<TextView>(R.id.tvMaxSpeed).text = "%.1f".format(latest.maxKmh)
                view.findViewById<TextView>(R.id.tvAvgPace).text = "%.1f".format(latest.avgMinPerKm)
                view.findViewById<TextView>(R.id.tvBestPace).text = "%.1f".format(latest.bestMinPerKm)
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return "${h}h ${m}m"
    }
}