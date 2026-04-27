package com.ntando.expensetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        // Using built-in slide animation
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        
        ivLogo.startAnimation(fadeIn)
        tvAppName.startAnimation(slideUp)

        // Show progress bar during delay
        pbLoading.visibility = android.view.View.VISIBLE

        // Use Coroutines for a smoother transition
        lifecycleScope.launch {
            delay(2500)
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
