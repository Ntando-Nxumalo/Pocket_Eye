package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.UserDao
import com.ntando.expensetracker.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository to manage [User] data.
 * acts as an abstraction for user operations.
 */
class UserRepository(private val userDao: UserDao) {

    /**
     * Retrieve the current user from the database.
     */
    val user: Flow<User?> = userDao.getUser()

    /**
     * Insert a new user into the database.
     */
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    /**
     * Update user details.
     */
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
}
