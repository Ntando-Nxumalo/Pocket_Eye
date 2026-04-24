package com.ntando.expensetracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.data.entity.Category
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private var selectedDate = Calendar.getInstance()
    private var startTime = Calendar.getInstance()
    private var endTime = Calendar.getInstance()
    private var photoUri: Uri? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            photoUri = it
            val ivPhoto = findViewById<ImageView>(R.id.ivExpensePhoto)
            ivPhoto.visibility = View.VISIBLE
            ivPhoto.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_expenses_activity)

        val etDate = findViewById<EditText>(R.id.etDate)
        val etStartTime = findViewById<EditText>(R.id.etStartTime)
        val etEndTime = findViewById<EditText>(R.id.etEndTime)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val spCategory = findViewById<Spinner>(R.id.spCategory)
        val etCustomCategory = findViewById<EditText>(R.id.etCustomCategory)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnAddPhoto = findViewById<Button>(R.id.btnAddPhoto)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        setupHeader()
        setupNavigation()

        // Date Picker
        etDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day)
                etDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time))
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Time Pickers
        etStartTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                startTime.set(Calendar.HOUR_OF_DAY, hour)
                startTime.set(Calendar.MINUTE, minute)
                etStartTime.setText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(startTime.time))
            }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true).show()
        }

        etEndTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                endTime.set(Calendar.HOUR_OF_DAY, hour)
                endTime.set(Calendar.MINUTE, minute)
                etEndTime.setText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(endTime.time))
            }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true).show()
        }

        btnAddPhoto.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        val categories = mutableListOf("Food", "Shopping", "Bills", "Transport", "Other")
        
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as TextView).setTextColor(Color.BLACK)
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as TextView).setTextColor(Color.BLACK)
                return v
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (categories[position] == "Other") {
                    etCustomCategory.visibility = View.VISIBLE
                } else {
                    etCustomCategory.visibility = View.GONE
                }
                (view as? TextView)?.setTextColor(Color.BLACK)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener {
            val amountStr = etAmount.text.toString()
            val dateStr = etDate.text.toString()
            val startStr = etStartTime.text.toString()
            val endStr = etEndTime.text.toString()
            val note = etNote.text.toString()
            val selectedCategoryName = spCategory.selectedItem.toString()
            val customCategoryName = etCustomCategory.text.toString().trim()

            if (amountStr.isEmpty() || dateStr.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCategoryName == "Other" && customCategoryName.isEmpty()) {
                Toast.makeText(this, "Please enter a custom category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@AddExpenseActivity)
                
                // Map to ID 5 if "Other" is selected, regardless of if it's a custom name
                val categoryId = if (selectedCategoryName == "Other") 5 else spCategory.selectedItemPosition + 1
                
                db.expenseDao().insertExpense(
                    Expense(
                        categoryId = categoryId,
                        amount = amountStr.toDoubleOrNull() ?: 0.0,
                        description = if (selectedCategoryName == "Other") customCategoryName else note,
                        date = dateStr,
                        startTime = startStr,
                        endTime = endStr,
                        photoPath = photoUri?.toString()
                    )
                )
                Toast.makeText(this@AddExpenseActivity, "Expense saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnCancel.setOnClickListener { finish() }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Add Expense"
        findViewById<View>(R.id.btnHeaderHome)?.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        val btnAction = findViewById<ImageView>(R.id.btnHeaderRightAction)
        btnAction?.setImageResource(android.R.drawable.ic_menu_today) // Dashboard icon
        btnAction?.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.btnDashboardNav)?.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        findViewById<View>(R.id.btnGoals)?.setOnClickListener {
            startActivity(Intent(this, SetGoalsActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.btnReports)?.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
            finish()
        }
    }
}
