package com.ntando.expensetracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Goal
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.data.repository.ExpenseRepository
import com.ntando.expensetracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
        val llRecentTransactions = findViewById<LinearLayout>(R.id.llRecentTransactions)
        
        // Dynamic Header Setup
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val btnAction = findViewById<ImageView>(R.id.btnHeaderRightAction)
        
        tvHeaderTitle.text = "Pocket Eye"
        btnAction.setImageResource(android.R.drawable.ic_lock_power_off)
        btnAction.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Navigation
        setupNavigation()

        // Setup Charts
        setupCharts(db)

        // Observe ViewModel
        observeViewModel(tvBalance, progressLevel, tvXP, llRecentTransactions)
    }

    private fun setupCharts(db: com.ntando.expensetracker.data.database.AppDatabase) {
        findViewById<ComposeView>(R.id.cvGoalsCharts).setContent {
            val goals by db.goalDao().getTopGoals().collectAsState(initial = emptyList())
            var editingGoal by remember { mutableStateOf<Goal?>(null) }
            val scope = rememberCoroutineScope()
            
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (goals.isEmpty()) {
                    DummyGoalChart(percentage = 0.6f, label = "Vacation", color = androidx.compose.ui.graphics.Color(0xFF66BB6A))
                    DummyGoalChart(percentage = 0.35f, label = "New Car", color = androidx.compose.ui.graphics.Color(0xFFFFD54F))
                } else {
                    goals.forEachIndexed { index, goal ->
                        val color = if (index == 0) androidx.compose.ui.graphics.Color(0xFF66BB6A) else androidx.compose.ui.graphics.Color(0xFFFFD54F)
                        GoalChart(goal = goal, color = color, onClick = { editingGoal = goal })
                    }
                }
            }

            if (editingGoal != null) {
                var amountText by remember { mutableStateOf(editingGoal!!.currentAmount.toString()) }
                AlertDialog(
                    onDismissRequest = { editingGoal = null },
                    title = { Text("Update Saved Amount", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    text = {
                        Column {
                            Text("Goal: ${editingGoal!!.name}", modifier = Modifier.padding(bottom = 12.dp), fontSize = 16.sp)
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = { Text("Amount Already Saved", fontSize = 14.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val newAmount = amountText.toDoubleOrNull() ?: editingGoal!!.currentAmount
                                val updatedGoal = editingGoal!!.copy(currentAmount = newAmount)
                                scope.launch {
                                    db.goalDao().updateGoal(updatedGoal)
                                    editingGoal = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF66BB6A)) // Green
                        ) {
                            Text("Update", fontSize = 14.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingGoal = null }) {
                            Text("Cancel", fontSize = 14.sp)
                        }
                    }
                )
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

    private fun setupNavigation() {
        findViewById<View>(R.id.btnDashboardNav)?.setOnClickListener {
            // Already on Dashboard
        }
        findViewById<View>(R.id.btnAddExpense)?.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        findViewById<View>(R.id.btnGoals)?.setOnClickListener {
            startActivity(Intent(this, SetGoalsActivity::class.java))
        }
        findViewById<View>(R.id.btnReports)?.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
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

            val db = DatabaseProvider.getDatabase(this@Dashboard)
            val nameContainer = LinearLayout(this@Dashboard).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val categoryTv = TextView(this@Dashboard).apply {
                text = "Other"
                textSize = 14f
                setTextColor(Color.parseColor("#0D2B45"))
                typeface = Typeface.DEFAULT_BOLD
            }
            
            lifecycleScope.launch {
                val categories = db.categoryDao().getAllCategoriesOnce()
                val cat = categories.find { it.id == expense.categoryId }
                categoryTv.text = cat?.name ?: "Other"
            }

            val icon = View(this@Dashboard).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply { marginEnd = 32 }
                background = AppCompatResources.getDrawable(this@Dashboard, android.R.drawable.presence_online)
            }

            nameContainer.addView(categoryTv)
            nameContainer.addView(TextView(this@Dashboard).apply {
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
            addView(nameContainer)
            addView(amountTv)
        }
    }
}

@Composable
fun GoalChart(goal: Goal, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            Canvas(modifier = Modifier.size(80.dp)) {
                drawArc(
                    color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 9.dp.toPx())
                )
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360 * progress, useCenter = false,
                    style = Stroke(width = 9.dp.toPx())
                )
            }
            Text(text = "${(progress * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = goal.name, modifier = Modifier.padding(top = 4.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(text = "R${goal.currentAmount.toInt()} / R${goal.targetAmount.toInt()}", fontSize = 14.sp, color = androidx.compose.ui.graphics.Color.Gray, fontWeight = FontWeight.Medium)
        if (goal.minTargetAmount > 0 || goal.maxTargetAmount > 0) {
            Text(
                text = "Min:R${goal.minTargetAmount.toInt()} Max:R${goal.maxTargetAmount.toInt()}",
                fontSize = 10.sp,
                color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.8f)
            )
        }
        
        // Explicit Edit Button
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = androidx.compose.ui.graphics.Color(0xFF66BB6A), // Green theme color
            modifier = Modifier.padding(top = 10.dp).height(32.dp).clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("Edit Amount", color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DummyGoalChart(percentage: Float, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            Canvas(modifier = Modifier.size(80.dp)) {
                drawArc(
                    color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 9.dp.toPx())
                )
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360 * percentage, useCenter = false,
                    style = Stroke(width = 9.dp.toPx())
                )
            }
            Text(text = "${(percentage * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = label, modifier = Modifier.padding(top = 4.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun SpendingBreakdownChart(summaries: List<CategorySummary>, total: Double) {
    val db = DatabaseProvider.getDatabase(LocalContext.current)
    var categories by remember { mutableStateOf(emptyList<com.ntando.expensetracker.data.entity.Category>()) }
    
    LaunchedEffect(summaries) {
        categories = db.categoryDao().getAllCategoriesOnce()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        Box(
            modifier = Modifier.size(130.dp), 
            contentAlignment = Alignment.Center
        ) {
             Canvas(modifier = Modifier.size(110.dp)) {
                 var startAngle = -90f
                 if (total == 0.0) {
                     drawArc(androidx.compose.ui.graphics.Color.LightGray, 0f, 360f, false, style = Stroke(30f))
                 } else {
                     summaries.forEach { item ->
                         val sweepAngle = (item.totalAmount / total).toFloat() * 360f
                         drawArc(getCategoryColor(item.categoryId), startAngle, sweepAngle, false, style = Stroke(30f))
                         startAngle += sweepAngle
                     }
                 }
             }
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 Text("Total", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
                 Text("R${total.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF0D2B45))
             }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            summaries.forEach { item ->
                val category = categories.find { it.id == item.categoryId }
                val name = category?.name ?: "Other"
                LegendItem(getCategoryColor(item.categoryId), name, item.totalAmount)
            }
        }
    }
}

fun getCategoryColor(categoryId: Int): androidx.compose.ui.graphics.Color {
    return when(categoryId) {
        1 -> androidx.compose.ui.graphics.Color(0xFF66BB6A)
        2 -> androidx.compose.ui.graphics.Color(0xFFFFD54F)
        3 -> androidx.compose.ui.graphics.Color(0xFF42A5F5)
        4 -> androidx.compose.ui.graphics.Color(0xFFEF5350)
        else -> {
            val colors = listOf(
                androidx.compose.ui.graphics.Color(0xFFBA68C8),
                androidx.compose.ui.graphics.Color(0xFFFF8A65),
                androidx.compose.ui.graphics.Color(0xFF4DB6AC),
                androidx.compose.ui.graphics.Color(0xFFAED581)
            )
            colors[categoryId % colors.size]
        }
    }
}

@Composable
fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String, amount: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label, 
                modifier = Modifier.padding(start = 8.dp), 
                fontSize = 13.sp,
                color = androidx.compose.ui.graphics.Color(0xFF0D2B45)
            )
        }
        Text(
            text = "R${amount.toInt()}", 
            fontSize = 13.sp, 
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color(0xFF0D2B45)
        )
    }
}
