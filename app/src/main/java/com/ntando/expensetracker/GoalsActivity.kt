package com.ntando.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Goal
import kotlinx.coroutines.launch

class SetGoalsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.goals)

        val etGoalName = findViewById<EditText>(R.id.etGoalName)
        val etTargetAmount = findViewById<EditText>(R.id.etTargetAmount)
        val etCurrentAmount = findViewById<EditText>(R.id.etCurrentAmount)
        val btnSaveGoals = findViewById<Button>(R.id.btnSaveGoals)

        // Setup Navigation Bar
        setupNavigation()

        btnSaveGoals.setOnClickListener {
            val name = etGoalName.text.toString()
            val target = etTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
            val current = etCurrentAmount.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isEmpty() || target <= 0) {
                Toast.makeText(this, "Please enter a valid goal name and target", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@SetGoalsActivity)
                db.goalDao().insertGoal(Goal(
                    name = name,
                    targetAmount = target,
                    currentAmount = current
                ))
                Toast.makeText(this@SetGoalsActivity, "Goal '$name' added!", Toast.LENGTH_SHORT).show()
                
                // Clear inputs
                etGoalName.text.clear()
                etTargetAmount.text.clear()
                etCurrentAmount.text.clear()
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
