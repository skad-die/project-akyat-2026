package com.example.project_akyat.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_akyat.model.local.db.AppDatabase
import com.example.project_akyat.model.local.HikeEntity
import com.example.project_akyat.model.HikeRepository
import com.example.project_akyat.model.remote.toRequest
import com.example.project_akyat.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class HikeSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = HikeRepository(
        AppDatabase.getInstance(application).hikeDao()
    )
    private val api = RetrofitClient.create(application)

    fun saveHike(hike: HikeEntity) = viewModelScope.launch {
        repo.save(hike)
        val all = repo.allHikes.first()
        android.util.Log.d("AKYAT", "Hikes in DB: ${all.size}")

        if (isOnline()) {
            syncUnsynced()
        }
    }

    private suspend fun syncUnsynced() {
        val unsynced = repo.getUnsynced()
        unsynced.forEach { hike ->
            try {
                val response = api.createHike(hike.toRequest())
                if (response.isSuccessful) {
                    repo.update(hike.copy(synced = true))
                }
            } catch (e: Exception) {
                // no internet or server down — will retry next save
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isOnline(): Boolean {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}