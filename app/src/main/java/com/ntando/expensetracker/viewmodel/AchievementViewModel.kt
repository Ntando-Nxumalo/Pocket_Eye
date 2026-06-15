package com.ntando.expensetracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.expensetracker.data.entity.Achievement
import com.ntando.expensetracker.data.repository.AchievementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AchievementViewModel handles the business logic for the achievements system.
 * It provides a reactive stream of achievements for the UI to observe.
 */
class AchievementViewModel(
    private val repository: AchievementRepository,
    private val userId: Long
) : ViewModel() {

    private val TAG = "AchievementViewModel"

    /**
     * StateFlow representing the list of achievements for the current user.
     * Uses stateIn to convert a Flow into a StateFlow that survives configuration changes.
     */
    val achievements: StateFlow<List<Achievement>> = repository.getAllAchievements(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        Log.d(TAG, "Initializing AchievementViewModel for user: $userId")
    }

    /**
     * Manually unlocks an achievement. 
     * Note: Most achievements are unlocked automatically via ExpenseViewModel's interaction with the repository.
     */
    fun unlockAchievement(achievement: Achievement) {
        viewModelScope.launch {
            Log.i(TAG, "Manually unlocking achievement: ${achievement.title}")
            repository.updateAchievement(achievement.copy(
                isUnlocked = true, 
                dateUnlocked = System.currentTimeMillis()
            ))
        }
    }
}
