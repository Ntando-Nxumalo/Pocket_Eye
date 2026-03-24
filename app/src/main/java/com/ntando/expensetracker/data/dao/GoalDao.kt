package com.ntando.expensetracker.data.dao

import androidx.room.*
import com.ntando.expensetracker.data.entity.Goal

@Dao
interface GoalDao {

    @Insert
    suspend fun insertGoal(goal: Goal)

    @Query("SELECT * FROM goals LIMIT 1")
    suspend fun getGoal(): Goal?
}
