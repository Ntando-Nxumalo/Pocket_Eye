package com.ntando.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentExpenses: StateFlow<List<Expense>> = repository.recentExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalSpending: StateFlow<Double> = repository.totalSpending
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val categorySummaries: StateFlow<List<CategorySummary>> = repository.categorySummary.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addExpense(amount: Double, description: String, date: String, categoryId: Int) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    amount = amount,
                    description = description,
                    date = date,
                    categoryId = categoryId,
                    startTime = "",
                    endTime = "",
                    photoPath = null
                )
            )
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name))
        }
    }
}
