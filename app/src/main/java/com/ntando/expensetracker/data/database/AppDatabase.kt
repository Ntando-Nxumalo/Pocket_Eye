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
    version = 2 // Increased version number to fix schema mismatch crash
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun achievementDao(): AchievementDao
    abstract fun currencyDao(): CurrencyDao
}
