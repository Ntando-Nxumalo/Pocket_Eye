package com.ntando.expensetracker

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Goal
import com.ntando.expensetracker.data.repository.ExpenseRepository
import com.ntando.expensetracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Dashboard : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize ViewModel
        val db = DatabaseProvider.getDatabase(this)
        val repository = ExpenseRepository(db.expenseDao(), db.categoryDao())
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory).get(ExpenseViewModel::class.java)

        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val progressLevel = findViewById<ProgressBar>(R.id.progressLevel)
        val tvXP = findViewById<TextView>(R.id.tvXP)
        val btnLogout = findViewById<LinearLayout>(R.id.btnLogout)
        val llRecentTransactions = findViewById<LinearLayout>(R.id.llRecentTransactions)
        
        // Navigation
        val btnAddExpense = findViewById<View>(R.id.btnAddExpense)
        val btnGoals = findViewById<View>(R.id.btnGoals)
        val btnReports = findViewById<View>(R.id.btnReports)

        btnLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        btnGoals.setOnClickListener {
            startActivity(Intent(this, SetGoalsActivity::class.java))
        }
        btnReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }

        // Setup Charts
        setupCharts(db)

        // Observe ViewModel
        observeViewModel(tvBalance, progressLevel, tvXP, llRecentTransactions)
    }

    private fun setupCharts(db: com.ntando.expensetracker.data.database.AppDatabase) {
        findViewById<ComposeView>(R.id.cvGoalsCharts).setContent {
            val goals by db.goalDao().getTopGoals().collectAsState(initial = emptyList())
            
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (goals.isEmpty()) {
                    GoalChart(percentage = 0.6f, label = "Vacation", color = androidx.compose.ui.graphics.Color(0xFF66BB6A))
                    GoalChart(percentage = 0.35f, label = "New Car", color = androidx.compose.ui.graphics.Color(0xFFFFD54F))
                } else {
                    goals.forEachIndexed { index, goal ->
                        val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                        val color = if (index == 0) androidx.compose.ui.graphics.Color(0xFF66BB6A) else androidx.compose.ui.graphics.Color(0xFFFFD54F)
                        GoalChart(percentage = progress, label = goal.name, color = color)
                    }
                }
            }
        }

        findViewById<ComposeView>(R.id.cvSpendingChart).setContent {
            val summaries by viewModel.categorySummaries.collectAsState()
            val total by viewModel.totalSpending.collectAsState()
            SpendingBreakdownChart(summaries, total)
        }
    }

    private fun observeViewModel(
        tvBalance: TextView,
        progressLevel: ProgressBar,
        tvXP: TextView,
        llRecentTransactions: LinearLayout
    ) {
        lifecycleScope.launch {
            viewModel.totalSpending.collect { total ->
                tvBalance.text = "R%.2f".format(total)
            }
        }

        lifecycleScope.launch {
            viewModel.recentExpenses.collect { expenses ->
                val count = expenses.size
                val userXP = count * 10
                progressLevel.progress = userXP % 100
                tvXP.text = "$userXP XP"
                findViewById<TextView>(R.id.tvLevelLabel).text = "Level ${ (userXP / 100) + 1 }"
            }
        }

        lifecycleScope.launch {
            viewModel.recentExpenses.collectLatest { expenses ->
                llRecentTransactions.removeAllViews()
                expenses.forEach { expense ->
                    llRecentTransactions.addView(createTransactionView(expense))
                }
            }
        }
    }

    private fun createTransactionView(expense: com.ntando.expensetracker.data.entity.Expense): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 16) }
            gravity = Gravity.CENTER_VERTICAL

            val categoryName = when(expense.categoryId) {
                1 -> "Food"
                2 -> "Shopping"
                3 -> "Bills"
                4 -> "Transport"
                else -> "Other"
            }

            val icon = View(this@Dashboard).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply { marginEnd = 32 }
                background = AppCompatResources.getDrawable(this@Dashboard, android.R.drawable.presence_online)
            }

            val textContainer = LinearLayout(this@Dashboard).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            textContainer.addView(TextView(this@Dashboard).apply {
                text = categoryName
                textSize = 14f
                setTextColor(Color.parseColor("#0D2B45"))
                typeface = Typeface.DEFAULT_BOLD
            })

            textContainer.addView(TextView(this@Dashboard).apply {
                text = expense.description
                textSize = 12f
                setTextColor(Color.GRAY)
            })

            val amountTv = TextView(this@Dashboard).apply {
                text = "-R%.2f".format(expense.amount)
                setTextColor(Color.parseColor("#EF5350"))
                typeface = Typeface.DEFAULT_BOLD
            }

            addView(icon)
            addView(textContainer)
            addView(amountTv)
        }
    }
}

@Composable
fun GoalChart(percentage: Float, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            Canvas(modifier = Modifier.size(80.dp)) {
                drawArc(
                    color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 8.dp.toPx())
                )
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360 * percentage, useCenter = false,
                    style = Stroke(width = 8.dp.toPx())
                )
            }
            Text(text = "${(percentage * 100).toInt()}%", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        Text(text = label, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun SpendingBreakdownChart(summaries: List<CategorySummary>, total: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.size(140.dp).padding(16.dp), contentAlignment = Alignment.Center) {
             Canvas(modifier = Modifier.size(140.dp)) {
                 var startAngle = -90f
                 if (total == 0.0) {
                     drawArc(androidx.compose.ui.graphics.Color.LightGray, 0f, 360f, false, style = Stroke(40f))
                 } else {
                     summaries.forEach { item ->
                         val sweepAngle = (item.totalAmount / total).toFloat() * 360f
                         drawArc(getCategoryColor(item.categoryId), startAngle, sweepAngle, false, style = Stroke(40f))
                         startAngle += sweepAngle
                     }
                 }
             }
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            LegendItem(androidx.compose.ui.graphics.Color(0xFF66BB6A), "Food")
            LegendItem(androidx.compose.ui.graphics.Color(0xFFFFD54F), "Shopping")
            LegendItem(androidx.compose.ui.graphics.Color(0xFF42A5F5), "Bills")
            LegendItem(androidx.compose.ui.graphics.Color(0xFFEF5350), "Transport")
        }
    }
}

fun getCategoryColor(categoryId: Int): androidx.compose.ui.graphics.Color {
    return when(categoryId) {
        1 -> androidx.compose.ui.graphics.Color(0xFF66BB6A)
        2 -> androidx.compose.ui.graphics.Color(0xFFFFD54F)
        3 -> androidx.compose.ui.graphics.Color(0xFF42A5F5)
        4 -> androidx.compose.ui.graphics.Color(0xFFEF5350)
        else -> androidx.compose.ui.graphics.Color.Gray
    }
}

@Composable
fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Canvas(modifier = Modifier.size(12.dp)) { drawCircle(color) }
        Text(text = label, modifier = Modifier.padding(start = 8.dp), fontSize = 12.sp)
    }
}
