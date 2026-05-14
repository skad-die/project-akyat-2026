package com.example.project_akyat.network

import com.example.project_akyat.model.remote.HikeRequest
import com.example.project_akyat.model.remote.LoginRequest
import com.example.project_akyat.model.remote.LoginResponse
import com.example.project_akyat.model.remote.MeResponse
import com.example.project_akyat.model.remote.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("hikes")
    suspend fun createHike(@Body hike: HikeRequest): Response<HikeRequest>

    @GET("hikes")
    suspend fun getHikes(): Response<List<HikeRequest>>

    @DELETE("hikes/{id}")
    suspend fun deleteHike(@Path("id") id: String): Response<Unit>

    @GET("me")
    suspend fun getMe(): Response<MeResponse>
}