package com.ntando.expensetracker.data.repository

import android.util.Log
import com.ntando.expensetracker.data.dao.CategoryDao
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.dao.ExpenseDao
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that abstracts access to multiple data sources (ExpenseDao and CategoryDao).
 * It provides a clean API for the rest of the application to interact with expense-related data.
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {
    private val TAG = "ExpenseRepository"

    // Queries returning Flows for reactive UI updates
    fun getAllExpenses(userId: Long): Flow<List<Expense>> = expenseDao.getAllExpenses(userId)
    fun getAllCategories(userId: Long): Flow<List<Category>> = categoryDao.getAllCategories(userId)
    fun getTotalSpending(userId: Long): Flow<Double?> = expenseDao.getTotalSpendingFlow(userId)
    fun getCategorySummary(userId: Long): Flow<List<CategorySummary>> = expenseDao.getCategorySummary(userId)
    fun getRecentExpenses(userId: Long): Flow<List<Expense>> = expenseDao.getRecentExpenses(userId)
    fun getExpenseCount(userId: Long): Flow<Int> = expenseDao.getExpenseCount(userId)

    /**
     * Inserts a new expense into the database.
     */
    suspend fun insertExpense(expense: Expense) {
        Log.d(TAG, "Inserting expense: ${expense.amount} for user ${expense.userId}")
        expenseDao.insertExpense(expense)
    }

    /**
     * Deletes an expense from the database.
     */
    suspend fun deleteExpense(expense: Expense) {
        Log.d(TAG, "Deleting expense ID: ${expense.id}")
        expenseDao.deleteExpense(expense)
    }

    /**
     * Adds a new category.
     */
    suspend fun insertCategory(category: Category) {
        Log.d(TAG, "Inserting category: ${category.name}")
        categoryDao.insertCategory(category)
    }

    /**
     * Retrieves total spending within a specific date range.
     */
    fun getTotalExpensesInRange(userId: Long, startDate: String, endDate: String): Flow<Double?> {
        Log.v(TAG, "Getting total expenses in range: $startDate to $endDate")
        return expenseDao.getTotalExpensesInRange(userId, startDate, endDate)
    }

    /**
     * Retrieves category-wise summaries within a specific date range.
     */
    fun getCategorySummaryInRange(userId: Long, startDate: String, endDate: String): Flow<List<CategorySummary>> {
        return expenseDao.getCategorySummaryInRange(userId, startDate, endDate)
    }
}
