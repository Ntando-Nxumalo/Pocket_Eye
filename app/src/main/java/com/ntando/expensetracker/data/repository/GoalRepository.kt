package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.GoalDao
import com.ntando.expensetracker.data.entity.Goal

/**
 * Repository to manage [Goal] operations.
 * decouples the DAO from the UI layer.
 */
class GoalRepository(private val goalDao: GoalDao) {

    /**
     * Fetch the user's primary spending goal.
     */
    suspend fun getGoal(): Goal? {
        return goalDao.getGoal()
    }

    /**
     * Insert or replace a spending goal.
     */
    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal)
    }
}
