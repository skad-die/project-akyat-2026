package com.example.project_akyat.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hikes")
data class HikeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val durationSeconds: Int,
    val distanceKm: Double,
    val steps: Int = 0,
    val calories: Int = 0,
    val avgKmh: Double = 0.0,
    val maxKmh: Double = 0.0,
    val avgMinPerKm: Double = 0.0,
    val bestMinPerKm: Double = 0.0,
    val gainMeters: Double = 0.0,
    val minMeters: Double = 0.0,
    val maxMeters: Double = 0.0,
    val startedAt: String,
    val endedAt: String,
    val synced: Boolean = false
)