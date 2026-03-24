package com.ntando.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val minGoal: Double,
    val maxGoal: Double
)
