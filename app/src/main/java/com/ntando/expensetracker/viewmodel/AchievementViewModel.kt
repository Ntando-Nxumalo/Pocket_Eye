package com.ntando.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.expensetracker.data.entity.Achievement
import com.ntando.expensetracker.data.repository.AchievementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AchievementViewModel(private val repository: AchievementRepository) : ViewModel() {

    val achievements: StateFlow<List<Achievement>> = repository.allAchievements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun unlockAchievement(achievement: Achievement) {
        viewModelScope.launch {
            repository.updateAchievement(achievement.copy(isUnlocked = true, dateUnlocked = System.currentTimeMillis()))
        }
    }
}
