package com.ntando.expensetracker.data.dao

import androidx.room.*
import com.ntando.expensetracker.data.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("SELECT * FROM goals WHERE userId = :userId")
    fun getAllGoals(userId: Long): Flow<List<Goal>>

    /**
     * Fetch all savings goals (no category link and has a target amount).
     */
    @Query("SELECT * FROM goals WHERE userId = :userId AND categoryId IS NULL AND targetAmount > 0")
    fun getSavingsGoals(userId: Long): Flow<List<Goal>>

    /**
     * Fetch goals that act as budgets (have category link OR have a max spending limit).
     */
    @Query("SELECT * FROM goals WHERE userId = :userId AND (categoryId IS NOT NULL OR maxTargetAmount > 0)")
    fun getBudgetGoals(userId: Long): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id AND userId = :userId")
    suspend fun getGoalById(id: Int, userId: Long): Goal?
}
