package com.ntando.expensetracker.data.repository

import com.ntando.expensetracker.data.dao.CurrencyDao
import com.ntando.expensetracker.data.entity.CurrencyRate

/**
 * Repository to manage [CurrencyRate] operations.
 * helps abstract currency logic from the UI.
 */
class CurrencyRepository(private val currencyDao: CurrencyDao) {

    /**
     * Fetch all available currency rates from the database.
     */
    suspend fun getAllRates(): List<CurrencyRate> {
        return currencyDao.getRates()
    }

    /**
     * Add or update a currency rate.
     */
    suspend fun insertRate(rate: CurrencyRate) {
        currencyDao.insertRate(rate)
    }
}
