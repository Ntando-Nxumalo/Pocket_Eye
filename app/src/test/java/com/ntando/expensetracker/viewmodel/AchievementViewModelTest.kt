package com.ntando.expensetracker.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.ntando.expensetracker.data.entity.Achievement
import com.ntando.expensetracker.data.repository.AchievementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: AchievementRepository = mockk()
    private val userId: Long = 1L
    private lateinit var viewModel: AchievementViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getAllAchievements(userId) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `achievements flow should emit values from repository`() = runTest {
        val achievementList = listOf(
            Achievement(id = 1, userId = userId, title = "Test Achievement", description = "Desc", icon = "icon_url", isUnlocked = false)
        )
        every { repository.getAllAchievements(userId) } returns flowOf(achievementList)

        viewModel = AchievementViewModel(repository, userId)

        viewModel.achievements.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(achievementList, awaitItem())
            } else {
                assertEquals(achievementList, item)
            }
        }
    }

    @Test
    fun `unlockAchievement should call repository update`() = runTest {
        viewModel = AchievementViewModel(repository, userId)
        val achievement = Achievement(id = 1, userId = userId, title = "Test", description = "Desc", icon = "icon", isUnlocked = false)
        
        coEvery { repository.updateAchievement(any()) } returns Unit

        viewModel.unlockAchievement(achievement)
        
        advanceUntilIdle()

        coVerify { 
            repository.updateAchievement(match { 
                it.id == achievement.id && it.isUnlocked 
            }) 
        }
    }
}
