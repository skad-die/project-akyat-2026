package com.example.project_akyat.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HikeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hike: HikeEntity)

    @Update
    suspend fun update(hike: HikeEntity)

    @Delete
    suspend fun delete(hike: HikeEntity)

    @Query("SELECT * FROM hikes ORDER BY id DESC")
    fun getAllHikes(): Flow<List<HikeEntity>>

    @Query("SELECT * FROM hikes WHERE synced = 0")
    suspend fun getUnsynced(): List<HikeEntity>

    @Query("SELECT * FROM hikes WHERE id = :id")
    suspend fun getById(id: Int): HikeEntity?
}