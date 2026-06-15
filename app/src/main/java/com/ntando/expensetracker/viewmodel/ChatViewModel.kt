package com.ntando.expensetracker.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.expensetracker.data.dao.GoalDao
import com.ntando.expensetracker.data.model.ChatMessage
import com.ntando.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ExpenseRepository,
    private val goalDao: GoalDao,
    private val userId: Long
) : ViewModel() {

    private val TAG = "ChatViewModel"

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val tips = listOf(
        "Track every cent. Small expenses add up quickly!",
        "Try the 50/30/20 rule: 50% Needs, 30% Wants, 20% Savings.",
        "Wait 24 hours before making a non-essential purchase.",
        "Check for unused subscriptions and cancel them.",
        "Cook at home more often to save on food costs."
    )

    fun sendWelcomeMessage() {
        if (_messages.value?.isEmpty() == true) {
            Log.d(TAG, "Sending initial welcome message to user $userId")
            addMessage(ChatMessage("Hello! I'm your Pocket Eye assistant. How can I help you today?", false))
        }
    }

    fun sendMessage(text: String) {
        Log.i(TAG, "User sent message: $text")
        addMessage(ChatMessage(text, true))

        viewModelScope.launch {
            Log.d(TAG, "Processing bot response for input: $text")
            delay(500) // Natural delay
            val response = getBotResponse(text.lowercase())
            Log.d(TAG, "Bot response generated: $response")
            addMessage(ChatMessage(response, false))
        }
    }

    private fun addMessage(message: ChatMessage) {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(message)
        _messages.value = current
    }

    private suspend fun getBotResponse(input: String): String {
        return when {
            input.contains("hello") || input.contains("hi") -> {
                Log.v(TAG, "Greeting detected")
                "Hi there! Ask me about your balance, goals, or spending."
            }
            
            input.contains("balance") || input.contains("total") -> {
                Log.v(TAG, "Balance query detected")
                val total = repository.getTotalSpending(userId).first() ?: 0.0
                "Your total spending to date is R%,.2f.".format(total)
            }
            
            input.contains("goal") || input.contains("save") || input.contains("target") -> {
                Log.v(TAG, "Goals query detected")
                val goals = goalDao.getAllGoals(userId).first()
                if (goals.isEmpty()) {
                    "You haven't set any goals yet. Head over to the Goals section to start saving!"
                } else {
                    val goalInfo = goals.joinToString("\n") { goal ->
                        val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100).toInt() else 0
                        "- ${goal.name}: R%,.2f / R%,.2f ($progress%)".format(goal.currentAmount, goal.targetAmount)
                    }
                    "Here are your current goals:\n$goalInfo"
                }
            }
            
            input.contains("spending") || input.contains("spend") -> {
                Log.v(TAG, "Spending summary query detected")
                val summaries = repository.getCategorySummary(userId).first()
                if (summaries.isEmpty()) {
                    "You haven't logged any expenses yet."
                } else {
                    val categories = repository.getAllCategories(userId).first()
                    val total = summaries.sumOf { it.totalAmount }
                    val summaryInfo = summaries.take(3).joinToString("\n") { summary ->
                        val catName = categories.find { it.id == summary.categoryId }?.name ?: "Unknown"
                        "- $catName: R%,.2f".format(summary.totalAmount)
                    }
                    "You've spent a total of R%,.2f. Your top categories are:\n$summaryInfo".format(total)
                }
            }

            input.contains("month") -> {
                Log.v(TAG, "Monthly spending query detected")
                val calendar = java.util.Calendar.getInstance()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val start = sdf.format(calendar.time)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                val end = sdf.format(calendar.time)
                
                val monthTotal = repository.getTotalExpensesInRange(userId, start, end).first() ?: 0.0
                "You've spent R%,.2f so far this month.".format(monthTotal)
            }
            
            input.contains("tip") || input.contains("save") || input.contains("advice") -> {
                Log.v(TAG, "Financial tip query detected")
                tips.random()
            }
            
            input.contains("level") || input.contains("xp") -> {
                Log.v(TAG, "Gamification status query detected")
                val count = repository.getExpenseCount(userId).first()
                val expensesPerLevel = 5
                val xpPerLevel = 100
                val xpPerExpense = xpPerLevel / expensesPerLevel
                
                val level = (count / expensesPerLevel) + 1
                val progress = (count % expensesPerLevel) * xpPerExpense
                "You're Level $level with $progress/100 XP towards the next level. Keep logging expenses to level up!"
            }
            
            else -> {
                Log.w(TAG, "Unknown user input: $input")
                "I'm not sure about that. Try asking about your balance, goals, or monthly spending."
            }
        }
    }
}
