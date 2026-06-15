package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.AchievementDao
import com.ntando.expensetracker.data.dao.ExpenseDao
import com.ntando.expensetracker.data.dao.GoalDao
import com.ntando.expensetracker.data.entity.Achievement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val expenseDao: ExpenseDao,
    private val goalDao: GoalDao? = null
) {
    fun getAllAchievements(userId: Long): Flow<List<Achievement>> = achievementDao.getAllAchievements(userId)

    suspend fun insertAchievement(achievement: Achievement) {
        achievementDao.insertAchievement(achievement)
    }

    suspend fun updateAchievement(achievement: Achievement) {
        achievementDao.updateAchievement(achievement)
    }

    suspend fun checkAndUnlockAchievements(userId: Long) {
        val currentTime = System.currentTimeMillis()
        val allExpenses = expenseDao.getAllExpenses(userId).first()
        val count = allExpenses.size

        // 1. 'First Step' - Log your first expense
        if (count >= 1) {
            achievementDao.unlockAchievement("First Step", userId, currentTime)
        }

        // Get distinct dates for consistency checks
        val distinctDates = expenseDao.getDistinctExpenseDates(userId)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 2. 'Week Warrior' - Log expenses for 7 consecutive days
        if (hasConsecutiveDays(distinctDates, 7, sdf)) {
            achievementDao.unlockAchievement("Week Warrior", userId, currentTime)
        }

        // 3. 'Consistent Tracker' - Log at least one expense every day for 30 days
        if (hasConsecutiveDays(distinctDates, 30, sdf)) {
            achievementDao.unlockAchievement("Consistent Tracker", userId, currentTime)
        }

        // 4. 'Budget Boss' - Stay within your monthly goal for the full month
        checkBudgetBoss(userId, currentTime)
    }

    private fun hasConsecutiveDays(dates: List<String>, requiredDays: Int, sdf: SimpleDateFormat): Boolean {
        if (dates.size < requiredDays) return false
        
        var consecutiveCount = 1
        for (i in 0 until dates.size - 1) {
            val date1 = try { sdf.parse(dates[i]) } catch(_: Exception) { null }
            val date2 = try { sdf.parse(dates[i+1]) } catch(_: Exception) { null }
            
            if (date1 != null && date2 != null) {
                val diff = (date1.time - date2.time) / (1000 * 60 * 60 * 24)
                if (diff == 1L) {
                    consecutiveCount++
                    if (consecutiveCount >= requiredDays) return true
                } else {
                    consecutiveCount = 1
                }
            }
        }
        return false
    }

    private suspend fun checkBudgetBoss(userId: Long, currentTime: Long) {
        if (goalDao == null) return
        
        // Fetch all goals for the user
        val goals = goalDao.getAllGoals(userId).first()
        if (goals.isEmpty()) return

        // Find the "Total Monthly Budget" goal (no categoryId and maxTargetAmount > 0)
        val totalBudgetGoal = goals.find { it.categoryId == null && it.maxTargetAmount > 0 }
        val monthlyLimit = totalBudgetGoal?.maxTargetAmount ?: 0.0

        if (monthlyLimit <= 0) return

        val calendar = Calendar.getInstance()
        // Check last month
        calendar.add(Calendar.MONTH, -1)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calendar.set(year, month, 1)
        val startDate = sdf.format(calendar.time)
        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = sdf.format(calendar.time)

        val totalSpending = expenseDao.getTotalExpensesInRange(userId, startDate, endDate).first() ?: 0.0

        // Only unlock if they actually spent something (to make it an achievement)
        if (totalSpending > 0 && totalSpending <= monthlyLimit) {
            achievementDao.unlockAchievement("Budget Boss", userId, currentTime)
        }
    }
}
