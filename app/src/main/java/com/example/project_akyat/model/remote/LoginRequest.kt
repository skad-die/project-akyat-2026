package com.example.project_akyat.model.remote

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String
)