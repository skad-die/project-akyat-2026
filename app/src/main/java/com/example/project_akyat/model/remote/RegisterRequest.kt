package com.example.project_akyat.model.remote

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)