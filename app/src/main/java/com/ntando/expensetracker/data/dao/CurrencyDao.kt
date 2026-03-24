package com.ntando.expensetracker.data.dao

import androidx.room.*
import com.ntando.expensetracker.data.entity.CurrencyRate

@Dao
interface CurrencyDao {

    @Insert
    suspend fun insertRate(rate: CurrencyRate)

    @Query("SELECT * FROM currency_rates")
    suspend fun getRates(): List<CurrencyRate>
}
