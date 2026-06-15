package com.ntando.expensetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashActivity provides a smooth entry point for the application.
 * It handles branding animation and session-based navigation logic.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting Splash screen")
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)

        // Load branding animations from XML resources
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        
        Log.v(TAG, "Starting entrance animations")
        ivLogo.startAnimation(fadeIn)
        tvAppName.startAnimation(slideUp)

        // Show progress bar to indicate background initialization
        pbLoading.visibility = android.view.View.VISIBLE

        /**
         * Use Coroutines to handle the delay and navigation logic without blocking the UI thread.
         * The app stays on the splash screen for 2.5 seconds to establish branding.
         */
        lifecycleScope.launch {
            delay(2500)
            
            // Check session status: If user ID exists in SharedPreferences, skip login
            val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
            val currentUserId = sharedPref.getLong("current_user_id", -1L)
            
            val nextActivity = if (currentUserId != -1L) {
                Log.i(TAG, "Active session found for user ID $currentUserId. Redirecting to Dashboard.")
                Dashboard::class.java
            } else {
                Log.i(TAG, "No active session found. Redirecting to Login.")
                MainActivity::class.java
            }
            
            val intent = Intent(this@SplashActivity, nextActivity)
            startActivity(intent)
            
            // Standard fade transition between activities
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            Log.d(TAG, "Transitioning to ${nextActivity.simpleName}")
            finish() // Ensure the splash screen is removed from the backstack
        }
    }
}
