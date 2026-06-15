package com.ntando.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Long, // Link to User
    val name: String,
    val targetAmount: Double,
    val minTargetAmount: Double = 0.0,
    val maxTargetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val categoryId: Int? = null // Link to Category for budget tracking
)
