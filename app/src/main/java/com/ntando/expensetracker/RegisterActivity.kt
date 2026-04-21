package com.ntando.expensetracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.User
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        registerBtn.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@RegisterActivity)
                val existingUser = db.userDao().getUserByName(username)
                
                if (existingUser != null) {
                    Toast.makeText(this@RegisterActivity, "User already exists", Toast.LENGTH_SHORT).show()
                } else {
                    // FIX: Pass the password to the User constructor
                    db.userDao().insertUser(User(name = username, email = username, password = password))
                    Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        loginLink.setOnClickListener {
            finish()
        }
    }
}