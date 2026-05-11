package com.example.project_akyat.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_akyat.R
import com.example.project_akyat.adapters.HikeAdapter
import com.example.project_akyat.model.HikeRepository
import com.example.project_akyat.model.local.HikeEntity
import com.example.project_akyat.model.local.db.AppDatabase
import com.example.project_akyat.model.remote.toRequest
import com.example.project_akyat.network.RetrofitClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private lateinit var rvHikeHistory: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var repo: HikeRepository
    private lateinit var hikeAdapter: HikeAdapter
    private lateinit var tvHikeCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHikeHistory = view.findViewById(R.id.rvHikeHistory)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvHikeCount = view.findViewById(R.id.tvHikeCount)

        val dao = AppDatabase.getInstance(requireContext()).hikeDao()
        repo = HikeRepository(dao)

        setupRecyclerView()

        observeHikes()
    }

    private fun setupRecyclerView() {
        hikeAdapter = HikeAdapter(
            onUploadClick = { hike ->
                if (isOnline()) {
                    uploadHike(hike)
                } else {
                    Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
                }
            }
        )

        rvHikeHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hikeAdapter
        }
    }

    private fun observeHikes() {
        viewLifecycleOwner.lifecycleScope.launch {
            repo.allHikes.collectLatest { hikes ->
                tvHikeCount.text = "${hikes.size} hikes"

                if (hikes.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvHikeHistory.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvHikeHistory.visibility = View.VISIBLE

                    hikeAdapter.submitList(hikes)
                }
            }
        }
    }

    private fun uploadHike(hike: HikeEntity) {
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