package com.ntando.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import kotlinx.coroutines.launch

class Dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val progressLevel = findViewById<ProgressBar>(R.id.progressLevel)
        val tvXP = findViewById<TextView>(R.id.tvXP)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Category views
        val tvFood = findViewById<TextView>(R.id.tvFood)
        val tvShopping = findViewById<TextView>(R.id.tvShopping)
        val tvBills = findViewById<TextView>(R.id.tvBills)
        val tvTransport = findViewById<TextView>(R.id.tvTransport)

        btnLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Load data from database using Flows for real-time updates
        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@Dashboard)
            
            // Observe Total Spending
            launch {
                db.expenseDao().getTotalSpending().collect { total ->
                    tvBalance.text = "R%.2f".format(total ?: 0.0)
                }
            }

            // Observe XP (based on expense count)
            launch {
                db.expenseDao().getExpenseCount().collect { count ->
                    val userXP = count * 10
                    progressLevel.progress = userXP % 100
                    tvXP.text = "$userXP XP"
                }
            }

            // Observe Category Summaries
            launch {
                db.expenseDao().getCategorySummary().collect { summary ->
                    summary.forEach { 
                        when(it.categoryId) {
                            1 -> tvFood.text = "Food: R${it.totalAmount}"
                            2 -> tvShopping.text = "Shopping: R${it.totalAmount}"
                            3 -> tvBills.text = "Bills: R${it.totalAmount}"
                            4 -> tvTransport.text = "Transport: R${it.totalAmount}"
                        }
                    }
                }
            }
        }
    }
}