package com.example.project_akyat.network

import com.example.project_akyat.model.HikeRequest
import com.example.project_akyat.model.LoginRequest
import com.example.project_akyat.model.LoginResponse
import com.example.project_akyat.model.RegisterRequest
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
}