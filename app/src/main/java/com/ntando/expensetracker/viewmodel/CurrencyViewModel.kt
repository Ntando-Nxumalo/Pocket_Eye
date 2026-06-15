package com.ntando.expensetracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Locale

class CurrencyViewModel : ViewModel() {

    // Rates relative to 1 ZAR (approximate)
    private val rates = mapOf(
        "ZAR" to 1.0,
        "USD" to 0.053,
        "EUR" to 0.049,
        "GBP" to 0.042,
        "AUD" to 0.081,
        "CNY" to 0.38
    )

    private val _convertedAmount = MutableLiveData<String>()
    val convertedAmount: LiveData<String> = _convertedAmount

    fun convert(amountStr: String, from: String, to: String) {
        if (amountStr.isBlank()) {
            _convertedAmount.value = ""
            return
        }

        val amount = amountStr.replace(",", ".").toDoubleOrNull()
        if (amount == null) {
            _convertedAmount.value = "Error"
            return
        }

        val fromRate = rates[from] ?: 1.0
        val toRate = rates[to] ?: 1.0

        // Convert to base (ZAR) then to target
        val inZar = amount / fromRate
        val result = inZar * toRate

        _convertedAmount.value = String.format(Locale.US, "%.2f", result)
    }
}
