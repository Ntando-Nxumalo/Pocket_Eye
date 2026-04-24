package com.ntando.expensetracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
        val etMinTargetAmount = findViewById<EditText>(R.id.etMinTargetAmount)
        val etMaxTargetAmount = findViewById<EditText>(R.id.etMaxTargetAmount)
        val etCurrentAmount = findViewById<EditText>(R.id.etCurrentAmount)
        val btnSaveGoals = findViewById<Button>(R.id.btnSaveGoals)

        setupHeader()
        setupNavigation()

        btnSaveGoals.setOnClickListener {
            val name = etGoalName.text.toString()
            val target = etTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
            val minTarget = etMinTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
            val maxTarget = etMaxTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
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
                    minTargetAmount = minTarget,
                    maxTargetAmount = maxTarget,
                    currentAmount = current
                ))
                Toast.makeText(this@SetGoalsActivity, "Goal '$name' added!", Toast.LENGTH_SHORT).show()
                
                etGoalName.text.clear()
                etTargetAmount.text.clear()
                etMinTargetAmount.text.clear()
                etMaxTargetAmount.text.clear()
                etCurrentAmount.text.clear()
            }
        }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Goals"
        findViewById<View>(R.id.btnHeaderHome)?.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        findViewById<View>(R.id.btnHeaderRightAction)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.btnDashboardNav)?.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        findViewById<View>(R.id.btnAddExpense)?.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.btnReports)?.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
            finish()
        }
    }
}
