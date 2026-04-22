package com.ntando.expensetracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ntando.expensetracker.data.dao.*
import com.ntando.expensetracker.data.entity.*

@Database(
    entities = [
        User::class,
        Category::class,
        Expense::class,
        Goal::class,
        Achievement::class,
        CurrencyRate::class
    ],
    version = 3 // Increased version to 3 to reflect User entity change
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun achievementDao(): AchievementDao
    abstract fun currencyDao(): CurrencyDao
}
