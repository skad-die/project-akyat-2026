package com.example.project_akyat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.project_akyat.fragments.DashboardFragment
import com.example.project_akyat.fragments.HistoryFragment
import com.example.project_akyat.fragments.ProgressFragment
import com.example.project_akyat.network.RetrofitClient
import com.example.project_akyat.network.TokenManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tokenManager = TokenManager(this)

        setupInsets()
        initViews()
        setupToolbar()
        setupDrawer()
        setupDrawerHeader()
        setupBottomNavigation()
        setupBackPressed()

        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment(), getString(R.string.dashboard))
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        bottomNav = findViewById(R.id.bottom_navigation)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupDrawerHeader() {
        val header = navView.getHeaderView(0)
        val tvNavName = header.findViewById<TextView>(R.id.tvNavName)
        val tvNavEmail = header.findViewById<TextView>(R.id.tvNavEmail)

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.create(this@MainActivity)
                val response = api.getMe()
                if (response.isSuccessful) {
                    val body = response.body()
                    tvNavName.text = body?.name
                    tvNavEmail.text = body?.email
                }
            } catch (e: Exception) {
                // silently fail — header just stays empty
            }
        }
    }

    private fun setupDrawer() {
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> logout()
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { replaceFragment(DashboardFragment(), getString(R.string.dashboard)); true }
                R.id.nav_history   -> { replaceFragment(HistoryFragment(), getString(R.string.history)); true }
                R.id.nav_progress  -> { replaceFragment(ProgressFragment(), getString(R.string.progress)); true }
                else -> false
            }
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    finish()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_menu -> { drawerLayout.openDrawer(GravityCompat.END); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        supportActionBar?.title = title
    }

    private fun logout() {
        tokenManager.clearToken()
        RetrofitClient.invalidate()
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}