package com.ntando.expensetracker

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private val selectedCategory = MutableStateFlow(0)
    private val selectedTimePeriod = MutableStateFlow(0)
    private var filteredExpensesList: List<Expense> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reports)

        val tvTotalExpenses = findViewById<TextView>(R.id.tvTotalExpensesAmount)
        val tvAverageMonth = findViewById<TextView>(R.id.tvAverageMonthAmount)
        val tvHighestExpense = findViewById<TextView>(R.id.tvHighestExpenseAmount)
        val tvLowestExpense = findViewById<TextView>(R.id.tvLowestExpenseAmount)
        val spCategory = findViewById<Spinner>(R.id.spCategoryReport)
        val spTimePeriod = findViewById<Spinner>(R.id.spTimePeriodReport)
        val btnExport = findViewById<Button>(R.id.btnExportReport)

        // Setup common header/footer logic
        setupHeader()
        setupNavigation()

        // Custom adapter to ensure visible black text in spinners
        val categories = listOf("All Categories", "Food", "Shopping", "Bills", "Transport", "Other")
        val categoryAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as TextView).setTextColor(AndroidColor.BLACK)
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as TextView).setTextColor(AndroidColor.BLACK)
                return v
            }
        }
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        val timePeriods = listOf("Last 3 Months", "Last 6 Months", "This Year", "All Time")
        val timeAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, timePeriods) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as TextView).setTextColor(AndroidColor.BLACK)
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as TextView).setTextColor(AndroidColor.BLACK)
                return v
            }
        }
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTimePeriod.adapter = timeAdapter

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                selectedCategory.value = position
                (view as? TextView)?.setTextColor(AndroidColor.BLACK)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spTimePeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                selectedTimePeriod.value = position
                (view as? TextView)?.setTextColor(AndroidColor.BLACK)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnExport.setOnClickListener {
            exportToPdf(filteredExpensesList)
        }

        val db = DatabaseProvider.getDatabase(this)
        val chartData = MutableStateFlow<List<Expense>>(emptyList())

        findViewById<ComposeView>(R.id.cvLineChart).setContent {
            val expenses by chartData.collectAsState()
            LineChart(expenses)
        }

        lifecycleScope.launch {
            combine(
                db.expenseDao().getAllExpenses(),
                selectedCategory,
                selectedTimePeriod
            ) { expenses, categoryIdx, timeIdx ->
                filterExpenses(expenses, categoryIdx, timeIdx)
            }.collectLatest { filtered ->
                filteredExpensesList = filtered
                chartData.value = filtered
                updateStats(filtered, tvTotalExpenses, tvAverageMonth, tvHighestExpense, tvLowestExpense)
            }
        }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Report"
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
        findViewById<View>(R.id.btnGoals)?.setOnClickListener {
            startActivity(Intent(this, SetGoalsActivity::class.java))
            finish()
        }
    }

    private fun filterExpenses(expenses: List<Expense>, categoryIdx: Int, timeIdx: Int): List<Expense> {
        val now = Calendar.getInstance()
        val filteredByTime = expenses.filter {
            val expenseDate = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)
            } catch (e: Exception) { null } ?: return@filter false
            val cal = Calendar.getInstance().apply { time = expenseDate }
            when (timeIdx) {
                0 -> cal.after(Calendar.getInstance().apply { add(Calendar.MONTH, -3) })
                1 -> cal.after(Calendar.getInstance().apply { add(Calendar.MONTH, -6) })
                2 -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                else -> true
            }
        }
        // Mapping category index from spinner to database IDs
        // 0: All, 1: Food (ID 1), 2: Shopping (ID 2), 3: Bills (ID 3), 4: Transport (ID 4), 5: Other (ID 5)
        return if (categoryIdx == 0) filteredByTime else filteredByTime.filter { it.categoryId == categoryIdx }
    }

    private fun updateStats(expenses: List<Expense>, totalTv: TextView, avgTv: TextView, highTv: TextView, lowTv: TextView) {
        val total = expenses.sumOf { it.amount }
        totalTv.text = "R%.2f".format(total)
        avgTv.text = "R%.2f".format(if (expenses.isNotEmpty()) total / 3.0 else 0.0)
        
        val highest = expenses.maxByOrNull { it.amount }?.amount ?: 0.0
        val lowest = expenses.minByOrNull { it.amount }?.amount ?: 0.0
        highTv.text = "R%.2f".format(highest)
        lowTv.text = "R%.2f".format(lowest)
    }

    private fun exportToPdf(expenses: List<Expense>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 14f
        canvas.drawText("PocketEye Expense Report", 10f, 25f, paint)
        
        paint.textSize = 10f
        var yPos = 50f
        canvas.drawText("Total Expenses: R%.2f".format(expenses.sumOf { it.amount }), 10f, yPos, paint)
        yPos += 20f

        expenses.forEach {
            if (yPos < 580f) {
                canvas.drawText("${it.date}: ${it.description} - R${it.amount}", 10f, yPos, paint)
                yPos += 15f
            }
        }

        pdfDocument.finishPage(page)

        val filePath = File(getExternalFilesDir(null), "ExpenseReport.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(filePath))
            Toast.makeText(this, "PDF Exported to: ${filePath.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        pdfDocument.close()
    }
}

@Composable
fun LineChart(expenses: List<Expense>) {
    Canvas(modifier = Modifier.fillMaxSize().padding(start = 40.dp, bottom = 40.dp, end = 20.dp, top = 20.dp)) {
        val width = size.width
        val height = size.height

        drawLine(color = Color.LightGray, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 1.dp.toPx())
        drawLine(color = Color.LightGray, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 1.dp.toPx())

        val paint = Paint().apply {
            color = AndroidColor.GRAY
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }

        if (expenses.isEmpty()) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("0", -10f, height, paint)
            }
            return@Canvas
        }
        
        val sorted = expenses.sortedBy { it.date }
        val maxAmount = sorted.maxByOrNull { it.amount }?.amount?.toFloat() ?: 1f
        
        drawIntoCanvas { canvas ->
            for (i in 0..4) {
                val value = (maxAmount * i / 4)
                val yPos = height - (value / maxAmount) * height
                canvas.nativeCanvas.drawText("R${value.toInt()}", -10f, yPos + 8f, paint)
            }
        }

        val xPaint = Paint().apply {
            color = AndroidColor.GRAY
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        
        val displayDates = if (sorted.size > 5) {
            listOf(sorted.first(), sorted[sorted.size/2], sorted.last())
        } else sorted

        drawIntoCanvas { canvas ->
            displayDates.forEach { expense ->
                val index = sorted.indexOf(expense)
                val xPos = if (sorted.size > 1) (index.toFloat() / (sorted.size - 1)) * width else width / 2
                val dateStr = expense.date.substring(5)
                canvas.nativeCanvas.drawText(dateStr, xPos, height + 30f, xPaint)
            }
        }

        val points = sorted.mapIndexed { index, expense ->
            val x = if (sorted.size > 1) (index.toFloat() / (sorted.size - 1)) * width else width / 2
            val y = height - (expense.amount.toFloat() / maxAmount) * height
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.forEach { lineTo(it.x, it.y) }
        }

        drawPath(path, Color(0xFF66BB6A), style = Stroke(width = 3.dp.toPx()))
        points.forEach { 
            drawCircle(Color(0xFF66BB6A), radius = 4.dp.toPx(), center = it)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = it)
        }
    }
}
