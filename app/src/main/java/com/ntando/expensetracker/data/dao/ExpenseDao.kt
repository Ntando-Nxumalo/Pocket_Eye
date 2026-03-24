package com.ntando.expensetracker.data.dao

import androidx.room.*
import com.ntando.expensetracker.data.entity.Expense
import kotlinx.coroutines.flow.Flow

data class CategorySummary(
    val categoryId: Int,
    val totalAmount: Double
)

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalSpending(): Double

    @Query("SELECT categoryId, SUM(amount) as totalAmount FROM expenses GROUP BY categoryId")
    suspend fun getCategorySummary(): List<CategorySummary>

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalExpensesInRange(startDate: Long, endDate: Long): Flow<Double?>
}
