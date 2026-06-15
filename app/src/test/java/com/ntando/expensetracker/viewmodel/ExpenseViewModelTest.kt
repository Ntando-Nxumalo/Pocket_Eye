package com.ntando.expensetracker.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.data.repository.AchievementRepository
import com.ntando.expensetracker.data.repository.ExpenseRepository
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
class ExpenseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: ExpenseRepository = mockk()
    private val achievementRepository: AchievementRepository = mockk()
    private val userId: Long = 1L
    private lateinit var viewModel: ExpenseViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks for initialization to avoid null pointer or exceptions
        every { repository.getAllExpenses(userId) } returns flowOf(emptyList())
        every { repository.getAllCategories(userId) } returns flowOf(emptyList())
        every { repository.getTotalSpending(userId) } returns flowOf(0.0)
        every { repository.getCategorySummary(userId) } returns flowOf(emptyList())
        every { repository.getExpenseCount(userId) } returns flowOf(0)
        every { repository.getTotalExpensesInRange(userId, any(), any()) } returns flowOf(0.0)
        every { repository.getCategorySummaryInRange(userId, any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `expenses flow should emit updates from repository`() = runTest {
        val expenseList = listOf(
            Expense(id = 1, userId = userId, amount = 100.0, description = "Test", date = "2023-10-01", categoryId = 1, startTime = "10:00", endTime = "11:00")
        )
        every { repository.getAllExpenses(userId) } returns flowOf(expenseList)

        viewModel = ExpenseViewModel(repository, userId, achievementRepository)

        viewModel.expenses.test {
            // First item might be initialValue (emptyList) or the mocked flow value depending on timing.
            // With StateFlow and WhileSubscribed, it usually emits initialValue first.
            val firstItem = awaitItem()
            if (firstItem.isEmpty()) {
                assertEquals(expenseList, awaitItem())
            } else {
                assertEquals(expenseList, firstItem)
            }
        }
    }

    @Test
    fun `addExpense should call repository and check achievements`() = runTest {
        viewModel = ExpenseViewModel(repository, userId, achievementRepository)
        
        coEvery { repository.insertExpense(any()) } returns Unit
        coEvery { achievementRepository.checkAndUnlockAchievements(userId) } returns Unit

        viewModel.addExpense(50.0, "Lunch", "2023-10-01", 1, "12:00", "13:00")
        
        advanceUntilIdle()

        coVerify { repository.insertExpense(any()) }
        coVerify { achievementRepository.checkAndUnlockAchievements(userId) }
    }

    @Test
    fun `totalSpending should emit correct value from repository`() = runTest {
        every { repository.getTotalSpending(userId) } returns flowOf(250.0)
        
        viewModel = ExpenseViewModel(repository, userId, achievementRepository)

        viewModel.totalSpending.test {
            val firstItem = awaitItem()
            if (firstItem == 0.0) {
                assertEquals(250.0, awaitItem(), 0.1)
            } else {
                assertEquals(250.0, firstItem, 0.1)
            }
        }
    }
}
