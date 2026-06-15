package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.AchievementDao
import com.ntando.expensetracker.data.dao.ExpenseDao
import com.ntando.expensetracker.data.dao.GoalDao
import com.ntando.expensetracker.data.entity.Achievement
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.data.entity.Goal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AchievementRepositoryTest {

    private val achievementDao: AchievementDao = mockk()
    private val expenseDao: ExpenseDao = mockk()
    private val goalDao: GoalDao = mockk()
    private val userId: Long = 1L
    private lateinit var repository: AchievementRepository

    @Before
    fun setup() {
        repository = AchievementRepository(achievementDao, expenseDao, goalDao)
    }

    @Test
    fun `checkAndUnlockAchievements should unlock First Step when at least one expense exists`() = runTest {
        val expenses = listOf(
            Expense(id = 1, userId = userId, amount = 10.0, description = "Test", date = "2023-10-01", categoryId = 1, startTime = "10:00", endTime = "11:00")
        )
        every { expenseDao.getAllExpenses(userId) } returns flowOf(expenses)
        coEvery { achievementDao.unlockAchievement(any(), any(), any()) } returns Unit
        coEvery { expenseDao.getDistinctExpenseDates(userId) } returns listOf("2023-10-01")
        every { goalDao.getAllGoals(userId) } returns flowOf(emptyList())

        repository.checkAndUnlockAchievements(userId)

        coVerify { achievementDao.unlockAchievement("First Step", userId, any()) }
    }

    @Test
    fun `checkAndUnlockAchievements should unlock Week Warrior for 7 consecutive days`() = runTest {
        val dates = listOf("2023-10-07", "2023-10-06", "2023-10-05", "2023-10-04", "2023-10-03", "2023-10-02", "2023-10-01")
        val expenses = listOf(mockk<Expense>())
        
        every { expenseDao.getAllExpenses(userId) } returns flowOf(expenses)
        coEvery { expenseDao.getDistinctExpenseDates(userId) } returns dates
        coEvery { achievementDao.unlockAchievement(any(), any(), any()) } returns Unit
        every { goalDao.getAllGoals(userId) } returns flowOf(emptyList())

        repository.checkAndUnlockAchievements(userId)

        coVerify { achievementDao.unlockAchievement("Week Warrior", userId, any()) }
    }

    @Test
    fun `checkBudgetBoss should unlock achievement if spending is within limit`() = runTest {
        // Mock current time and dates for "last month" logic in checkBudgetBoss
        // The implementation uses Calendar.getInstance(), so it's a bit hard to mock exactly 
        // without wrapping Calendar.
        // However, we can mock whatever dates it asks for.
        
        val goals = listOf(
            Goal(id = 1, userId = userId, name = "Total Monthly Budget", targetAmount = 0.0, minTargetAmount = 0.0, maxTargetAmount = 1000.0, currentAmount = 0.0, categoryId = null)
        )
        every { goalDao.getAllGoals(userId) } returns flowOf(goals)
        every { expenseDao.getAllExpenses(userId) } returns flowOf(listOf(mockk()))
        coEvery { expenseDao.getDistinctExpenseDates(userId) } returns emptyList()
        coEvery { achievementDao.unlockAchievement(any(), any(), any()) } returns Unit
        
        // Mocking any range to return 500.0 (within 1000.0 limit)
        every { expenseDao.getTotalExpensesInRange(userId, any(), any()) } returns flowOf(500.0)

        repository.checkAndUnlockAchievements(userId)

        coVerify { achievementDao.unlockAchievement("Budget Boss", userId, any()) }
    }
}
