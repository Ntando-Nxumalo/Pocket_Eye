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

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC LIMIT 10")
    fun getRecentExpenses(userId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    fun getAllExpenses(userId: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId")
    fun getTotalSpendingFlow(userId: Long): Flow<Double?>

    @Query("SELECT categoryId, SUM(amount) as totalAmount FROM expenses WHERE userId = :userId GROUP BY categoryId ORDER BY totalAmount DESC, categoryId ASC")
    fun getCategorySummary(userId: Long): Flow<List<CategorySummary>>

    @Query("SELECT categoryId, SUM(amount) as totalAmount FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate GROUP BY categoryId")
    fun getCategorySummaryInRange(userId: Long, startDate: String, endDate: String): Flow<List<CategorySummary>>

    @Query("SELECT COUNT(*) FROM expenses WHERE userId = :userId")
    fun getExpenseCount(userId: Long): Flow<Int>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpensesInRange(userId: Long, startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT DISTINCT date FROM expenses WHERE userId = :userId ORDER BY date DESC")
    suspend fun getDistinctExpenseDates(userId: Long): List<String>
}
