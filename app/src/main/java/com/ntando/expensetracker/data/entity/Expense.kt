package com.ntando.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryId: Int,
    val amount: Double,
    val description: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val photoPath: String?
)
