package com.ntando.expensetracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Expense
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_expenses_activity)

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val spCategory = findViewById<Spinner>(R.id.spCategory)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        // Categories for dropdown
        val categories = listOf("Food", "Shopping", "Bills", "Transport", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter

        btnSave.setOnClickListener {
            val amountStr = etAmount.text.toString()
            val category = spCategory.selectedItem.toString()
            val note = etNote.text.toString()

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val categoryId = when(category) {
                "Food" -> 1
                "Shopping" -> 2
                "Bills" -> 3
                "Transport" -> 4
                else -> 5
            }

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@AddExpenseActivity)
                db.expenseDao().insertExpense(
                    Expense(
                        categoryId = categoryId,
                        amount = amount,
                        description = note,
                        date = currentDate,
                        startTime = "",
                        endTime = "",
                        photoPath = null
                    )
                )
                Toast.makeText(this@AddExpenseActivity, "Expense saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }
}
