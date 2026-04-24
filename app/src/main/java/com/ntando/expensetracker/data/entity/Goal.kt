package com.ntando.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val minTargetAmount: Double = 0.0,
    val maxTargetAmount: Double = 0.0,
    val currentAmount: Double = 0.0
)
