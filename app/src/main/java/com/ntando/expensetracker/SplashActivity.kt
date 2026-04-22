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

        // Load modern animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, androidx.appcompat.R.anim.abc_slide_in_bottom)
        
        ivLogo.startAnimation(fadeIn)
        tvAppName.startAnimation(slideUp)

        // Use Coroutines for a smoother transition
        lifecycleScope.launch {
            delay(2500) // Slightly longer for a better feel
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
