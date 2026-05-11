package com.example.project_akyat.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.project_akyat.R
import com.example.project_akyat.model.local.db.AppDatabase
import com.example.project_akyat.model.local.HikeEntity
import com.example.project_akyat.model.HikeRepository
import com.example.project_akyat.model.remote.toRequest
import com.example.project_akyat.network.RetrofitClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private lateinit var hikeListContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var repo: HikeRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hikeListContainer = view.findViewById(R.id.hikeListContainer)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        val dao = AppDatabase.getInstance(requireContext()).hikeDao()
        repo = HikeRepository(dao)

        observeHikes()
    }

    private fun observeHikes() {
        viewLifecycleOwner.lifecycleScope.launch {
            repo.allHikes.collectLatest { hikes ->
                hikeListContainer.removeAllViews()

                if (hikes.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    return@collectLatest
                }

                tvEmptyState.visibility = View.GONE
                hikes.forEach { hike -> addHikeItem(hike) }
            }
        }
    }

    private fun addHikeItem(hike: HikeEntity) {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_hike, hikeListContainer, false)

        itemView.findViewById<TextView>(R.id.tvHikeDate).text = hike.startedAt
        itemView.findViewById<TextView>(R.id.tvHikeDistance).text =
            getString(R.string.distance_format, hike.distanceKm)
        itemView.findViewById<TextView>(R.id.tvHikeDuration).text =
            "Duration: ${hike.durationSeconds}s"

        val btnUpload = itemView.findViewById<Button>(R.id.btnUpload)
        if (!hike.synced) {
            btnUpload.visibility = View.VISIBLE
            btnUpload.setOnClickListener {
                if (isOnline()) {
                    uploadHike(hike, btnUpload)
                } else {
                    Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
                }
            }
        }

        hikeListContainer.addView(itemView)
    }

    private fun uploadHike(hike: HikeEntity, btn: Button) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.create(requireContext())
                val response = api.createHike(hike.toRequest())
                if (response.isSuccessful) {
                    repo.update(hike.copy(synced = true))
                    Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}