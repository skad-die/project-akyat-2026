package com.example.project_akyat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_akyat.model.RegisterRequest
import com.example.project_akyat.network.RetrofitClient

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            registerUser(name, email, password)
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        val request = RegisterRequest(name, email, password)
        RetrofitClient.api.register(request)
            .enqueue(object : retrofit2.Callback<Void> {
                override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity,
                            "Registered successfully",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@RegisterActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity,
                        "Failed: ${t.message}",
                        Toast.LENGTH_SHORT).show()
                }
            })
    }
}