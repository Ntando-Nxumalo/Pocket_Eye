package com.ntando.expensetracker.data.dao

import androidx.room.*
import com.ntando.expensetracker.data.entity.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: Achievement)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAllAchievements(userId: Long): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getAllAchievementsOnce(userId: Long): List<Achievement>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND isUnlocked = 1 AND isNotified = 0")
    fun getUnnotifiedAchievements(userId: Long): Flow<List<Achievement>>

    @Query("UPDATE achievements SET isNotified = 1 WHERE id = :id")
    suspend fun markAsNotified(id: Long)

    @Query("UPDATE achievements SET isUnlocked = 1, dateUnlocked = :date WHERE title = :title AND userId = :userId AND isUnlocked = 0")
    suspend fun unlockAchievement(title: String, userId: Long, date: Long): Int
}
