package com.example.project_akyat.network

import com.example.project_akyat.model.RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<Void>
}