package com.example.project_akyat.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String
)