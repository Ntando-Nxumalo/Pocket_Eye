package com.ntando.expensetracker.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.ntando.expensetracker.data.entity.User
import com.ntando.expensetracker.data.repository.UserRepository
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
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: UserRepository = mockk()
    private lateinit var viewModel: AuthViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getUserById(any()) } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `user flow should emit user when userId is set`() = runTest {
        val testUser = User(id = 1L, name = "John Doe", email = "john@example.com", password = "password")
        every { repository.getUserById(1L) } returns flowOf(testUser)

        viewModel = AuthViewModel(repository)
        
        viewModel.user.test {
            assertEquals(null, awaitItem()) // Initial value
            
            viewModel.setUserId(1L)
            assertEquals(testUser, awaitItem())
        }
    }

    @Test
    fun `registerUser should call repository insert`() = runTest {
        viewModel = AuthViewModel(repository)
        coEvery { repository.insertUser(any()) } returns Unit

        viewModel.registerUser("Jane", "jane@example.com", "secret")
        
        advanceUntilIdle()

        coVerify { 
            repository.insertUser(match { 
                it.name == "Jane" && it.email == "jane@example.com" 
            }) 
        }
    }
}
