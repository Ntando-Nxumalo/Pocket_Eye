package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.AchievementDao
import com.ntando.expensetracker.data.database.AppDatabase
import com.ntando.expensetracker.data.entity.Achievement
import kotlinx.coroutines.flow.Flow

class AchievementRepository(private val achievementDao: AchievementDao) {
    val allAchievements: Flow<List<Achievement>> = achievementDao.getAllAchievements()

    suspend fun insertAchievement(achievement: Achievement) {
        achievementDao.insertAchievement(achievement)
    }

    suspend fun updateAchievement(achievement: Achievement) {
        achievementDao.updateAchievement(achievement)
    }

    /**
     * Logic to check and unlock achievements based on expense count.
     */
    suspend fun checkAchievements(db: AppDatabase) {
        val count = db.expenseDao().getExpenseCount()

        if (count >= 1) db.achievementDao().unlockAchievement("Beginner Saver")
        if (count >= 5) db.achievementDao().unlockAchievement("Bronze Tracker")
        if (count >= 20) db.achievementDao().unlockAchievement("Silver Tracker")
    }
}
