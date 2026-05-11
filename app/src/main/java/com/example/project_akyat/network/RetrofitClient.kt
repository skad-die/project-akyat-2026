package com.example.project_akyat.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.57:3000/"

    @Volatile
    private var API_SERVICE: ApiService? = null

    fun create(context: Context): ApiService {
        return API_SERVICE ?: synchronized(this) {
            val instance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder()
                    // Use applicationContext to prevent leaking the Fragment/Activity context
                    .addInterceptor(AuthInterceptor(context.applicationContext))
                    .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)

            API_SERVICE = instance
            instance
        }
    }
}