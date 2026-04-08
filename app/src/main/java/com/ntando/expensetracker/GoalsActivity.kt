package com.ntando.expensetracker

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
        val btnBackDashboard = findViewById<Button>(R.id.btnBackDashboard)

        btnSaveGoals.setOnClickListener {
            val minGoal = etMinGoal.text.toString()
            val maxGoal = etMaxGoal.text.toString()

            if (minGoal.isEmpty() || maxGoal.isEmpty()) {
                Toast.makeText(this, "Please enter both goals", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Goals saved: Min R$minGoal, Max R$maxGoal", Toast.LENGTH_LONG).show()
            }
        }

        btnBackDashboard.setOnClickListener {
            finish() // closes this activity and returns to previous screen
        }
    }
}
