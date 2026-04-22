package com.ntando.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetGoalsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.goals)

        val etMinGoal = findViewById<EditText>(R.id.etMinGoal)
        val etMaxGoal = findViewById<EditText>(R.id.etMaxGoal)
        val btnSaveGoals = findViewById<Button>(R.id.btnSaveGoals)

        // Setup Navigation Bar
        setupNavigation()

        btnSaveGoals.setOnClickListener {
            val minGoal = etMinGoal.text.toString()
            val maxGoal = etMaxGoal.text.toString()

            if (minGoal.isEmpty() || maxGoal.isEmpty()) {
                Toast.makeText(this, "Please enter both goals", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Goals saved: Min R$minGoal, Max R$maxGoal", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupNavigation() {
        findViewById<android.view.View>(R.id.btnAddExpense)?.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
            finish()
        }
        findViewById<android.view.View>(R.id.btnGoals)?.setOnClickListener {
            // Already here
        }
        findViewById<android.view.View>(R.id.btnReports)?.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
            finish()
        }
    }
}
