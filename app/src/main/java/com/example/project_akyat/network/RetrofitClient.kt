package com.example.project_akyat.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.57:3000/"
    @Volatile
    private var API_SERVICE: ApiService? = null

    fun create(context: Context): ApiService {
        return API_SERVICE ?: synchronized(this) {
            val tokenManager = TokenManager(context.applicationContext)

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(tokenManager))
                .build()

            val instance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)


            API_SERVICE = instance
            instance
        }
    }

    fun invalidate() {
        API_SERVICE = null
    }
}