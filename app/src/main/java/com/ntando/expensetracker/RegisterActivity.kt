package com.ntando.expensetracker

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Achievement
import com.ntando.expensetracker.data.entity.User
import kotlinx.coroutines.launch

/**
 * RegisterActivity handles the creation of new user accounts.
 * It also initializes a set of achievements for every new user.
 */
class RegisterActivity : AppCompatActivity() {

    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Registration screen")
        setContentView(R.layout.activity_register)
        
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        /**
         * Register button click listener.
         * Performs validation and inserts the new user into the database.
         */
        registerBtn.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Basic Validation
            if (username.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "Registration failed: Empty fields")
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Log.w(TAG, "Registration failed: Passwords do not match")
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@RegisterActivity)
                
                // Check if the username is already taken
                val existingUser = db.userDao().getUserByName(username)
                
                if (existingUser != null) {
                    Log.w(TAG, "Registration failed: Username '$username' already exists")
                    Toast.makeText(this@RegisterActivity, "User already exists", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Registering new user: $username")
                    // Insert the user and get their unique ID
                    val userId = db.userDao().insertUser(User(name = username, email = username, password = password))
                    
                    Log.i(TAG, "User created with ID: $userId. Initializing achievements.")
                    
                    // Initialize achievements for the new user
                    // This sets up the gamification system for the specific user account
                    val initialAchievements = listOf(
                        Achievement(userId = userId, title = "First Step", description = "Log your first expense", icon = "star"),
                        Achievement(userId = userId, title = "Week Warrior", description = "Log expenses for 7 consecutive days", icon = "bolt"),
                        Achievement(userId = userId, title = "Budget Boss", description = "Stay within your monthly goal for the full month", icon = "account_balance_wallet"),
                        Achievement(userId = userId, title = "Consistent Tracker", description = "Log at least one expense every day for 30 days", icon = "calendar_month")
                    )
                    
                    initialAchievements.forEach {
                        db.achievementDao().insertAchievement(it)
                    }

                    Log.i(TAG, "Registration process complete for user: $username")
                    Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                    finish() // Close registration screen and return to login
                }
            }
        }

        /**
         * Navigate back to the login screen.
         */
        loginLink.setOnClickListener {
            Log.d(TAG, "User clicked login link, returning to MainActivity")
            finish()
        }
    }
}
