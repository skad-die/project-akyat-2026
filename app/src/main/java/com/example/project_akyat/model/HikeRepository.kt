package com.example.project_akyat.model

import com.example.project_akyat.model.local.HikeEntity
import com.example.project_akyat.model.local.dao.HikeDao
import kotlinx.coroutines.flow.Flow

class HikeRepository(private val dao: HikeDao) {

    val allHikes: Flow<List<HikeEntity>> = dao.getAllHikes()

    suspend fun save(hike: HikeEntity) = dao.insert(hike)

    suspend fun update(hike: HikeEntity) = dao.update(hike)

    suspend fun delete(hike: HikeEntity) = dao.delete(hike)

    suspend fun getUnsynced(): List<HikeEntity> = dao.getUnsynced()

    suspend fun getById(id: Int): HikeEntity? = dao.getById(id)
}