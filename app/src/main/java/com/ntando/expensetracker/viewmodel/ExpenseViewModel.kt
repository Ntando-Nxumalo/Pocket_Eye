package com.ntando.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.data.repository.AchievementRepository
import com.ntando.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val userId: Long,
    private val achievementRepository: AchievementRepository? = null
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getCurrentMonthStart(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return dateFormat.format(cal.time)
    }

    private fun getCurrentMonthEnd(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return dateFormat.format(cal.time)
    }

    val expenses: StateFlow<List<Expense>> = repository.getAllExpenses(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<Category>> = repository.getAllCategories(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalSpending: StateFlow<Double> = repository.getTotalSpending(userId)
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val categorySummaries: StateFlow<List<CategorySummary>> = repository.getCategorySummary(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val expenseCount: StateFlow<Int> = repository.getExpenseCount(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Reactive Current Month Data
    val currentMonthTotalSpending: StateFlow<Double> = repository.getTotalExpensesInRange(userId, getCurrentMonthStart(), getCurrentMonthEnd())
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val currentMonthCategorySummaries: StateFlow<List<CategorySummary>> = repository.getCategorySummaryInRange(userId, getCurrentMonthStart(), getCurrentMonthEnd())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addExpense(amount: Double, description: String, date: String, categoryId: Int, startTime: String, endTime: String, photoPath: String? = null) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    userId = userId,
                    amount = amount,
                    description = description,
                    date = date,
                    categoryId = categoryId,
                    startTime = startTime,
                    endTime = endTime,
                    photoPath = photoPath
                )
            )
            achievementRepository?.checkAndUnlockAchievements(userId)
        }
    }
}
