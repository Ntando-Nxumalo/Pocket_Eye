package com.ntando.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRate(

    @PrimaryKey
    val currencyCode: String,
    val rateToZAR: Double
)
