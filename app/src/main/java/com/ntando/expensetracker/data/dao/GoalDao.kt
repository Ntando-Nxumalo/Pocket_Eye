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

    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals LIMIT 2")
    fun getTopGoals(): Flow<List<Goal>>
}
