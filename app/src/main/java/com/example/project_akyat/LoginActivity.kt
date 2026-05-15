package com.example.project_akyat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_akyat.model.remote.LoginRequest
import com.example.project_akyat.network.RetrofitClient
import com.example.project_akyat.network.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var tvLoginError: TextView
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var rootLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        val btnGoToRegister = findViewById<Button>(R.id.btnGoToRegister)
        tvLoginError = findViewById(R.id.tvLoginError)
        progressBar = findViewById(R.id.progressBar)
        rootLayout = findViewById(R.id.main)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            tvLoginError.text = ""

            if (email.isEmpty() || password.isEmpty()) {
                tvLoginError.text = getString(R.string.all_fields_required)
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvLoginError.text = getString(R.string.invalid_email_or_password)
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // GO TO REGISTER
        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }


    private fun loginUser(email: String, password: String) {
        val request = LoginRequest(email, password)
        val api = RetrofitClient.create(this)
        val tokenManager = TokenManager(this)

        lifecycleScope.launch {
            btnLogin.isEnabled = false
            progressBar.visibility = View.VISIBLE
            rootLayout.animate().alpha(0.4f).setDuration(200).start()
            try {
                val response = api.login(request)

                if (response.isSuccessful) {
                    val token = response.body()?.token

                    if (token != null) {
                        tokenManager.saveToken(token)

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        tvLoginError.text = getString(R.string.invalid_response_from_server)
                    }
                } else {
                    tvLoginError.text = getString(R.string.login_failed_invalid_credentials)
                }

            } catch (e: Exception) {
                tvLoginError.text = getString(R.string.network_error_try_again)
            } finally {
                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE
                rootLayout.animate().alpha(1f).setDuration(200).start()
            }
        }
    }
}