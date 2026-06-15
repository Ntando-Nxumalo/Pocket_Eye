package com.ntando.expensetracker.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ntando.expensetracker.data.dao.GoalDao
import com.ntando.expensetracker.data.model.ChatMessage
import com.ntando.expensetracker.data.repository.ExpenseRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: ExpenseRepository = mockk()
    private val goalDao: GoalDao = mockk()
    private val userId: Long = 1L
    private lateinit var viewModel: ChatViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChatViewModel(repository, goalDao, userId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendWelcomeMessage should add message when list is empty`() = runTest {
        viewModel.messages.observeForever { }
        
        viewModel.sendWelcomeMessage()
        advanceUntilIdle()

        val lastValue = viewModel.messages.value
        assertTrue(lastValue != null && lastValue.isNotEmpty())
        assertEquals("Hello! I'm your Pocket Eye assistant. How can I help you today?", lastValue?.get(0)?.message)
    }

    @Test
    fun `sendMessage should add user message and then bot response`() = runTest {
        viewModel.messages.observeForever { }
        every { repository.getTotalSpending(userId) } returns flowOf(150.0)

        viewModel.sendMessage("balance")
        
        // Initial user message should be added immediately as it's not in a launch
        // but LiveData update might need a runCurrent
        runCurrent()
        assertEquals(1, viewModel.messages.value?.size)
        assertEquals("balance", viewModel.messages.value?.get(0)?.message)

        // Bot response is in viewModelScope.launch with delay(500)
        advanceTimeBy(501)
        runCurrent()
        
        // If it's still 1, maybe repository.getTotalSpending().first() is hanging?
        // Let's try advanceUntilIdle() instead
        advanceUntilIdle()

        val lastValue = viewModel.messages.value
        assertTrue("Expected bot response, current size: ${lastValue?.size}", (lastValue?.size ?: 0) >= 2)
        assertTrue("Bot response should contain 150: ${lastValue?.last()?.message}", lastValue?.last()?.message?.contains("150") == true)
    }

    @Test
    fun `bot response for unknown input should return default message`() = runTest {
        viewModel.messages.observeForever { }
        viewModel.sendMessage("unknown query")
        
        advanceUntilIdle()

        val lastValue = viewModel.messages.value
        assertTrue(lastValue != null && lastValue.isNotEmpty())
        assertEquals("I'm not sure about that. Try asking about your balance, goals, or monthly spending.", lastValue?.last()?.message)
    }
}
