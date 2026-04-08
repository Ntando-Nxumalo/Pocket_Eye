package com.ntando.expensetracker


import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

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
        val categories = listOf("Food", "Transport", "Entertainment", "Bills", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter

        btnSave.setOnClickListener {
            val amount = etAmount.text.toString()
            val category = spCategory.selectedItem.toString()
            val note = etNote.text.toString()

            Toast.makeText(this, "Saved: R$amount - $category ($note)", Toast.LENGTH_LONG).show()
        }

        btnCancel.setOnClickListener {
            etAmount.text.clear()
            etNote.text.clear()
            spCategory.setSelection(0)
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }
}
