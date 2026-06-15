package com.ntando.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.data.repository.AchievementRepository
import com.ntando.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class ExpenseEvent {
    data class AchievementUnlocked(val title: String) : ExpenseEvent()
    data class LevelUp(val newLevel: Int) : ExpenseEvent()
}

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val userId: Long,
    private val achievementRepository: AchievementRepository? = null
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private val _events = MutableSharedFlow<ExpenseEvent>()
    val events = _events.asSharedFlow()

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

    // Level tracking to detect level-up (Now levels up every 5 expenses)
    init {
        viewModelScope.launch {
            expenseCount
                .map { (it / 5) + 1 }
                .distinctUntilChanged()
                .drop(1) // Skip initial level
                .collect { newLevel ->
                    _events.emit(ExpenseEvent.LevelUp(newLevel))
                }
        }
    }

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
            val newlyUnlocked = achievementRepository?.checkAndUnlockAchievements(userId)
            newlyUnlocked?.forEach { title ->
                _events.emit(ExpenseEvent.AchievementUnlocked(title))
            }
        }
    }
}
