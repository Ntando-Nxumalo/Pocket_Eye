package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.GoalDao
import com.ntando.expensetracker.data.entity.Goal
import kotlinx.coroutines.flow.Flow

/**
 * Repository to manage [Goal] operations.
 * decouples the DAO from the UI layer.
 */
class GoalRepository(private val goalDao: GoalDao) {

    /**
     * Fetch all user goals as a Flow.
     */
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

    /**
     * Fetch the top 2 goals as a Flow.
     */
    val topGoals: Flow<List<Goal>> = goalDao.getTopGoals()

    /**
     * Insert or replace a spending goal.
     */
    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal)
    }

    /**
     * Delete a goal.
     */
    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }
}
