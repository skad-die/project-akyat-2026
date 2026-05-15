package com.example.project_akyat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_akyat.model.remote.RegisterRequest
import com.example.project_akyat.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var tvRegError: TextView
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var rootLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnGoToLogin = findViewById<Button>(R.id.btnGoToLogin)

        tvRegError = findViewById(R.id.tvRegError)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        rootLayout = findViewById(android.R.id.content)

        btnRegister.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            tvRegError.text = ""

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                tvRegError.text = getString(R.string.all_fields_required)
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvRegError.text = getString(R.string.invalid_email_or_password)
                return@setOnClickListener
            }

            if (password.length < 8) {
                tvRegError.text = getString(R.string.password_must_be_at_least_8_characters)
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                tvRegError.text = getString(R.string.passwords_do_not_match)
                return@setOnClickListener
            }

            registerUser(name, email, password)
        }

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        val request = RegisterRequest(name, email, password)
        val api = RetrofitClient.create(this)

        lifecycleScope.launch {
            btnRegister.isEnabled = false
            progressBar.visibility = View.VISIBLE
            rootLayout.animate().alpha(0.4f).setDuration(200).start()

            try {
                val response = api.register(request)

                if (response.isSuccessful) {
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    tvRegError.text = when (response.code()) {
                        409 -> "An account with this email already exists."
                        else -> "Registration failed. Please try again."
                    }
                }

            } catch (e: Exception) {
                tvRegError.text = getString(R.string.network_error_try_again)
            } finally {
                btnRegister.isEnabled = true
                progressBar.visibility = View.GONE
                rootLayout.animate().alpha(1f).setDuration(200).start()
            }
        }
    }
}