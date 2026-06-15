package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.CategoryDao
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.dao.ExpenseDao
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {
    fun getAllExpenses(userId: Long): Flow<List<Expense>> = expenseDao.getAllExpenses(userId)
    fun getAllCategories(userId: Long): Flow<List<Category>> = categoryDao.getAllCategories(userId)
    fun getTotalSpending(userId: Long): Flow<Double?> = expenseDao.getTotalSpendingFlow(userId)
    fun getCategorySummary(userId: Long): Flow<List<CategorySummary>> = expenseDao.getCategorySummary(userId)
    fun getRecentExpenses(userId: Long): Flow<List<Expense>> = expenseDao.getRecentExpenses(userId)
    fun getExpenseCount(userId: Long): Flow<Int> = expenseDao.getExpenseCount(userId)

    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    fun getTotalExpensesInRange(userId: Long, startDate: String, endDate: String): Flow<Double?> {
        return expenseDao.getTotalExpensesInRange(userId, startDate, endDate)
    }

    fun getCategorySummaryInRange(userId: Long, startDate: String, endDate: String): Flow<List<CategorySummary>> {
        return expenseDao.getCategorySummaryInRange(userId, startDate, endDate)
    }
}
