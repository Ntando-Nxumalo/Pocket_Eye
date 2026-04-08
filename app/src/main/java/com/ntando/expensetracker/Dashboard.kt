package com.ntando.expensetracker

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        // ✅ ADD YOUR CODE RIGHT HERE ↓↓↓

        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val progressLevel = findViewById<ProgressBar>(R.id.progressLevel)
        val tvXP = findViewById<TextView>(R.id.tvXP)

        // Example backend data (replace later with real data)
        val totalBalance = 1250.75
        val userXP = 60

        tvBalance.text = "R%.2f".format(totalBalance)
        progressLevel.progress = userXP
        tvXP.text = "$userXP XP"

        // ✅ KEEP YOUR EXISTING CODE BELOW

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}