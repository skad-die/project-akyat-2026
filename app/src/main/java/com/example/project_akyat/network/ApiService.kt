package com.example.project_akyat.network

import com.example.project_akyat.model.remote.HikeRequest
import com.example.project_akyat.model.remote.LoginRequest
import com.example.project_akyat.model.remote.LoginResponse
import com.example.project_akyat.model.remote.RegisterRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("register")
    fun register(@Body request: RegisterRequest): Call<Void>

    @POST("hikes")
    suspend fun createHike(@Body hike: HikeRequest): Response<Unit>

    @GET("hikes")
    suspend fun getHikes(): Response<List<HikeRequest>>
}