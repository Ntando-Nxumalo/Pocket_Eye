package com.ntando.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val dateUnlocked: Long? = null
)
