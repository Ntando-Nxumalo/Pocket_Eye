package com.ntando.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "achievements",
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
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long, // Link to User
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val dateUnlocked: Long? = null,
    val isNotified: Boolean = false
)
