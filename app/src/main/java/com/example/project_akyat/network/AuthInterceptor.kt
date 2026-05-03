package com.example.project_akyat.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: android.content.Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val token = TokenManager.getToken(context)

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}