package com.ntando.expensetracker.viewmodel

import android.util.Log
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

/**
 * Events that can be triggered from the ViewModel to be handled by the UI.
 */
sealed class ExpenseEvent {
    data class AchievementUnlocked(val title: String) : ExpenseEvent()
    data class LevelUp(val newLevel: Int) : ExpenseEvent()
}

/**
 * ViewModel responsible for managing expense data, gamification logic, and budget summaries.
 * Communicates with the ExpenseRepository to perform CRUD operations.
 */
class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val userId: Long,
    private val achievementRepository: AchievementRepository? = null
) : ViewModel() {

    private val TAG = "ExpenseViewModel"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // SharedFlow to emit one-time events like Level Up alerts
    private val _events = MutableSharedFlow<ExpenseEvent>()
    val events = _events.asSharedFlow()

    // Helper functions for date range calculation (Current Month)
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

    /**
     * Flow of all expenses for the current user.
     * Uses stateIn to convert the cold flow from the repository into a hot StateFlow
     * that survives configuration changes.
     */
    val expenses: StateFlow<List<Expense>> = repository.getAllExpenses(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Flow of all categories created by the current user.
     */
    val categories: StateFlow<List<Category>> = repository.getAllCategories(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Flow calculating total spending across all time.
     * Maps the Double? from the database to a Double (defaulting to 0.0).
     */
    val totalSpending: StateFlow<Double> = repository.getTotalSpending(userId)
        .map { it ?: 0.0 }
        .onEach { Log.v(TAG, "New total spending calculated: R$it") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Flow providing summaries of spending grouped by category.
     */
    val categorySummaries: StateFlow<List<CategorySummary>> = repository.getCategorySummary(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Flow tracking the total number of logged expenses for gamification.
     */
    val expenseCount: StateFlow<Int> = repository.getExpenseCount(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        Log.i(TAG, "Initializing ExpenseViewModel for User ID: $userId")
        
        // Level tracking logic: Levels up every 5 expenses
        viewModelScope.launch {
            expenseCount
                .map { (it / 5) + 1 }
                .distinctUntilChanged()
                .drop(1) // Skip initial emission of level 1
                .collect { newLevel ->
                    Log.i(TAG, "Gamification: User leveled up to $newLevel")
                    _events.emit(ExpenseEvent.LevelUp(newLevel))
                }
        }
    }

    /**
     * Reactive flow for total spending within the current calendar month.
     */
    val currentMonthTotalSpending: StateFlow<Double> = repository.getTotalExpensesInRange(userId, getCurrentMonthStart(), getCurrentMonthEnd())
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Reactive flow for category-wise spending within the current calendar month.
     */
    val currentMonthCategorySummaries: StateFlow<List<CategorySummary>> = repository.getCategorySummaryInRange(userId, getCurrentMonthStart(), getCurrentMonthEnd())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Inserts a new expense into the database and checks for achievement unlocks.
     */
    fun addExpense(amount: Double, description: String, date: String, categoryId: Int, startTime: String, endTime: String, photoPath: String? = null) {
        viewModelScope.launch {
            Log.d(TAG, "Adding expense of R$amount for user $userId. Category ID: $categoryId")
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
            
            // Check for newly unlocked achievements after adding an expense
            Log.d(TAG, "Checking for achievement progress...")
            val newlyUnlocked = achievementRepository?.checkAndUnlockAchievements(userId)
            newlyUnlocked?.forEach { title ->
                Log.i(TAG, "CONGRATS: Achievement unlocked: $title")
                _events.emit(ExpenseEvent.AchievementUnlocked(title))
            }
        }
    }
}
