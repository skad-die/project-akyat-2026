package com.example.project_akyat.network

import com.example.project_akyat.model.LoginRequest
import com.example.project_akyat.model.LoginResponse
import com.example.project_akyat.model.RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<Void>
}