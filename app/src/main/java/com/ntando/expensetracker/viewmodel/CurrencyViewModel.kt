package com.ntando.expensetracker.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Locale

/**
 * CurrencyViewModel handles the logic for real-time currency conversion.
 * It uses a fixed set of exchange rates for immediate calculation on the dashboard.
 */
class CurrencyViewModel : ViewModel() {

    private val TAG = "CurrencyViewModel"

    // Mock exchange rates relative to 1 ZAR (Base Currency)
    // These provide approximate values for demonstration purposes.
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

    /**
     * Converts an amount from one currency to another using ZAR as the base.
     * @param amountStr The raw string input from the EditText.
     * @param from The source currency code (e.g., "USD").
     * @param to The target currency code (e.g., "ZAR").
     */
    fun convert(amountStr: String, from: String, to: String) {
        if (amountStr.isBlank()) {
            _convertedAmount.value = ""
            return
        }

        // Clean the string for parsing (handling different decimal separators)
        val amount = amountStr.replace(",", ".").toDoubleOrNull()
        if (amount == null) {
            Log.w(TAG, "Failed to parse conversion amount: $amountStr")
            _convertedAmount.value = "Error"
            return
        }

        val fromRate = rates[from] ?: 1.0
        val toRate = rates[to] ?: 1.0

        // Step 1: Convert source amount to ZAR (base)
        // Step 2: Convert ZAR amount to target currency
        val inZar = amount / fromRate
        val result = inZar * toRate

        Log.v(TAG, "Converted $amount $from to ${String.format("%.2f", result)} $to")
        
        // Post the result to the LiveData for the UI to observe
        _convertedAmount.value = String.format(Locale.US, "%.2f", result)
    }
}
