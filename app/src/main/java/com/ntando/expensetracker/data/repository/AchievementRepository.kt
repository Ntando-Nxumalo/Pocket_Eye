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

    suspend fun checkAndUnlockAchievements(userId: Long): List<String> {
        val newlyUnlocked = mutableListOf<String>()
        val currentTime = System.currentTimeMillis()
        val allExpenses = expenseDao.getAllExpenses(userId).first()
        val count = allExpenses.size

        // 1. 'First Step'
        if (count >= 1) {
            if (achievementDao.unlockAchievement("First Step", userId, currentTime) > 0) {
                newlyUnlocked.add("First Step")
            }
        }

        val distinctDates = expenseDao.getDistinctExpenseDates(userId)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 2. 'Week Warrior'
        if (hasConsecutiveDays(distinctDates, 7, sdf)) {
            if (achievementDao.unlockAchievement("Week Warrior", userId, currentTime) > 0) {
                newlyUnlocked.add("Week Warrior")
            }
        }

        // 3. 'Consistent Tracker'
        if (hasConsecutiveDays(distinctDates, 30, sdf)) {
            if (achievementDao.unlockAchievement("Consistent Tracker", userId, currentTime) > 0) {
                newlyUnlocked.add("Consistent Tracker")
            }
        }

        // 4. 'Budget Boss'
        if (checkBudgetBossInternal(userId, currentTime)) {
            if (achievementDao.unlockAchievement("Budget Boss", userId, currentTime) > 0) {
                newlyUnlocked.add("Budget Boss")
            }
        }

        return newlyUnlocked
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
                } else { consecutiveCount = 1 }
            }
        }
        return false
    }

    private suspend fun checkBudgetBossInternal(userId: Long, currentTime: Long): Boolean {
        if (goalDao == null) return false
        val goals = goalDao.getAllGoals(userId).first()
        val totalBudgetGoal = goals.find { it.categoryId == null && it.maxTargetAmount > 0 }
        val monthlyLimit = totalBudgetGoal?.maxTargetAmount ?: 0.0
        if (monthlyLimit <= 0) return false

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val start = sdf.format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = sdf.format(calendar.time)

        val spent = expenseDao.getTotalExpensesInRange(userId, start, end).first() ?: 0.0
        return spent > 0 && spent <= monthlyLimit
    }

    suspend fun updateAchievement(achievement: Achievement) {
        achievementDao.updateAchievement(achievement)
    }
}
