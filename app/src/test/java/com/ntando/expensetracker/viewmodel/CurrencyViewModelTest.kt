package com.ntando.expensetracker.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CurrencyViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CurrencyViewModel

    @Before
    fun setup() {
        viewModel = CurrencyViewModel()
    }

    @Test
    fun `convert from ZAR to USD should return correct value`() {
        // ZAR to USD: 100 * 0.053 = 5.30
        viewModel.convert("100", "ZAR", "USD")
        assertEquals("5.30", viewModel.convertedAmount.value)
    }

    @Test
    fun `convert from USD to ZAR should return correct value`() {
        // USD to ZAR: 5.30 / 0.053 = 100.00
        viewModel.convert("5.30", "USD", "ZAR")
        assertEquals("100.00", viewModel.convertedAmount.value)
    }

    @Test
    fun `convert with invalid amount should return Error`() {
        viewModel.convert("abc", "ZAR", "USD")
        assertEquals("Error", viewModel.convertedAmount.value)
    }

    @Test
    fun `convert with empty amount should return empty string`() {
        viewModel.convert("", "ZAR", "USD")
        assertEquals("", viewModel.convertedAmount.value)
    }
}
