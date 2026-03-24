package com.ntando.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.ntando.expensetracker.data.database.AppDatabase
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Achievement
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.ui.dashboard.DashboardScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                DashboardScreen(modifier = Modifier.padding(innerPadding))
            }
        }

        val db = DatabaseProvider.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            setupInitialAchievements(db)

            // Test logic: Insert test data and check achievements
            db.categoryDao().insertCategory(Category(name = "Food"))
            db.expenseDao().insertExpense(
                Expense(
                    categoryId = 1,
                    amount = 100.0,
                    description = "Lunch",
                    date = "2026-01-01",
                    startTime = "12:00",
                    endTime = "12:30",
                    photoPath = null
                )
            )
            checkAchievements(db)
        }
    }

    private suspend fun setupInitialAchievements(db: AppDatabase) {
        val existing = db.achievementDao().getAllAchievements().first()
        if (existing.isEmpty()) {
            val defaultAchievements = listOf(
                Achievement(title = "Beginner Saver", description = "Add first expense", icon = "star"),
                Achievement(title = "Bronze Tracker", description = "Add 5 expenses", icon = "trending_up"),
                Achievement(title = "Silver Tracker", description = "Add 20 expenses", icon = "payments"),
                Achievement(title = "Smart Spender", description = "Stay under budget", icon = "savings")
            )
            defaultAchievements.forEach { db.achievementDao().insertAchievement(it) }
        }
    }

    private suspend fun checkAchievements(db: AppDatabase) {
        val count = db.expenseDao().getExpenseCount()
        if (count >= 1) db.achievementDao().unlockAchievement("Beginner Saver")
        if (count >= 5) db.achievementDao().unlockAchievement("Bronze Tracker")
        if (count >= 20) db.achievementDao().unlockAchievement("Silver Tracker")
    }

    fun convert(amount: Double, rate: Double): Double = amount / rate
}
