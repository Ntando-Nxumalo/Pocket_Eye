package com.ntando.expensetracker.data.dao

import androidx.room.*
import com.ntando.expensetracker.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long

    @Query("SELECT * FROM categories WHERE userId = :userId OR userId = -1")
    fun getAllCategories(userId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE userId = :userId OR userId = -1")
    suspend fun getAllCategoriesOnce(userId: Long): List<Category>
}
