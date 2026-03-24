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
                // Basic entry point
                DashboardScreen(modifier = Modifier.padding(innerPadding))
            }
        }

        // Initialize database and insert initial achievements
        val db = DatabaseProvider.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            // Setup initial achievements if needed
            setupInitialAchievements(db)

            // STEP 4 - Test Expense + Badge
            // insert test category
            db.categoryDao().insertCategory(Category(name = "Food"))

            // insert test expense
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

            // check badges
            checkAchievements(db)
        }
    }

    private suspend fun setupInitialAchievements(db: AppDatabase) {
        val existing = db.achievementDao().getAllAchievements().first()
        if (existing.isEmpty()) {
            db.achievementDao().insertAchievement(
                Achievement(title = "Beginner Saver", description = "Add first expense", icon = "star", isUnlocked = false)
            )
            db.achievementDao().insertAchievement(
                Achievement(title = "Bronze Tracker", description = "Add 5 expenses", icon = "trending_up", isUnlocked = false)
            )
            db.achievementDao().insertAchievement(
                Achievement(title = "Silver Tracker", description = "Add 20 expenses", icon = "payments", isUnlocked = false)
            )
            db.achievementDao().insertAchievement(
                Achievement(title = "Smart Spender", description = "Stay under budget", icon = "savings", isUnlocked = false)
            )
        }
    }

    private suspend fun checkAchievements(db: AppDatabase) {
        val count = db.expenseDao().getExpenseCount()

        if (count >= 1) {
            db.achievementDao().unlockAchievement("Beginner Saver")
        }

        if (count >= 5) {
            db.achievementDao().unlockAchievement("Bronze Tracker")
        }

        if (count >= 20) {
            db.achievementDao().unlockAchievement("Silver Tracker")
        }
    }

    // STEP 5 - Add Currency Function
    fun convert(amount: Double, rate: Double): Double {
        return amount / rate
    }
}
