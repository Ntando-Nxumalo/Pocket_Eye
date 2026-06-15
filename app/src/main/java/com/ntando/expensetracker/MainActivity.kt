package com.ntando.expensetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import kotlinx.coroutines.launch

/**
 * MainActivity serves as the Login screen for the Pocket Eye application.
 * Users can log in using their username or email.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Login screen")
        setContentView(R.layout.activity_main)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        /**
         * Login button click listener.
         * Validates credentials against the Room database.
         */
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                Log.d(TAG, "Attempting login for user: $username")
                lifecycleScope.launch {
                    val db = DatabaseProvider.getDatabase(this@MainActivity)
                    // Check if user exists by name or email
                    val user = db.userDao().getUserByName(username) ?: db.userDao().getUserByEmail(username)
                    
                    if (user != null) {
                        if (user.password == password) {
                            Log.i(TAG, "Login successful for user: ${user.name}")
                            // Store user ID in SharedPreferences for session persistence
                            val sharedPref = getSharedPreferences("PocketEyePrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putLong("current_user_id", user.id)
                                apply()
                            }

                            // Navigate to Dashboard
                            val intent = Intent(this@MainActivity, Dashboard::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.w(TAG, "Login failed: Incorrect password for user $username")
                            Toast.makeText(this@MainActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w(TAG, "Login failed: User $username not found")
                        Toast.makeText(this@MainActivity, "User not found. Please register.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.v(TAG, "Login attempt with empty fields")
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Navigate to the Registration screen.
         */
        btnRegister.setOnClickListener {
            Log.d(TAG, "Navigating to RegisterActivity")
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
