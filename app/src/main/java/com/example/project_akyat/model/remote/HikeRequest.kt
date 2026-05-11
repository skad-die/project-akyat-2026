package com.example.project_akyat.model.remote

import com.example.project_akyat.model.local.HikeEntity

data class HikeRequest(
    val durationSeconds: Int,
    val distanceKm: Double,
    val steps: Int,
    val calories: Int,
    val speed: Speed,
    val pace: Pace,
    val elevation: Elevation,
    val startedAt: String,
    val endedAt: String,
    val syncStatus: String = "synced"
) {
    data class Speed(val avgKmh: Double, val maxKmh: Double)
    data class Pace(val avgMinPerKm: Double, val bestMinPerKm: Double)
    data class Elevation(val gainMeters: Double, val minMeters: Double, val maxMeters: Double)
}

fun HikeEntity.toRequest() = HikeRequest(
    durationSeconds = durationSeconds,
    distanceKm      = distanceKm,
    steps           = steps,
    calories        = calories,
    speed           = HikeRequest.Speed(avgKmh, maxKmh),
    pace            = HikeRequest.Pace(avgMinPerKm, bestMinPerKm),
    elevation       = HikeRequest.Elevation(gainMeters, minMeters, maxMeters),
    startedAt       = startedAt,
    endedAt         = endedAt
)