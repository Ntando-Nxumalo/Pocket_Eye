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
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val totalSpending: Flow<Double?> = expenseDao.getTotalSpendingFlow()
    val categorySummary: Flow<List<CategorySummary>> = expenseDao.getCategorySummary()
    val recentExpenses: Flow<List<Expense>> = expenseDao.getRecentExpenses()

    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    fun getTotalExpensesInRange(startDate: Long, endDate: Long): Flow<Double?> {
        return expenseDao.getTotalExpensesInRange(startDate, endDate)
    }
}
