package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.UserDao
import com.ntando.expensetracker.data.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val user: Flow<User?> = userDao.getUser()

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
}
