package com.ntando.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.DatabaseProvider
import kotlinx.coroutines.launch

class ReportsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reports)

        val tvTotalExpenses = findViewById<TextView>(R.id.tvTotalExpensesAmount)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        // Setup Navigation Bar
        setupNavigation()

        // Setup Chart
        findViewById<ComposeView>(R.id.cvLineChart).setContent {
            LineChart()
        }

        // Load data for reports
        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@ReportsActivity)
            db.expenseDao().getTotalSpendingFlow().collect { total ->
                tvTotalExpenses.text = "R%.2f".format(total ?: 0.0)
            }
        }
    }

    private fun setupNavigation() {
        findViewById<android.view.View>(R.id.btnAddExpense)?.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
            finish()
        }
        findViewById<android.view.View>(R.id.btnGoals)?.setOnClickListener {
            startActivity(Intent(this, SetGoalsActivity::class.java))
            finish()
        }
        findViewById<android.view.View>(R.id.btnReports)?.setOnClickListener {
            // Already here
        }
    }
}

@Composable
fun LineChart() {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height
        
        // Mock data points
        val points = listOf(
            Offset(0f, height * 0.2f),
            Offset(width * 0.25f, height * 0.4f),
            Offset(width * 0.5f, height * 0.3f),
            Offset(width * 0.75f, height * 0.5f),
            Offset(width, height * 0.25f)
        )

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                // Simplified cubic curve for smoothness
                val prev = points[i - 1]
                val curr = points[i]
                cubicTo(
                    prev.x + (curr.x - prev.x) / 2, prev.y,
                    prev.x + (curr.x - prev.x) / 2, curr.y,
                    curr.x, curr.y
                )
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF42A5F5),
            style = Stroke(width = 4.dp.toPx())
        )
        
        // Draw points
        points.forEach { point ->
            drawCircle(color = Color(0xFF42A5F5), radius = 6.dp.toPx(), center = point)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = point)
        }
    }
}
