package com.ntando.expensetracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ntando.expensetracker.data.entity.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val newInstance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense_database"
            )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = getDatabase(context)
                        val categoryDao = database.categoryDao()
                        // Ensure global categories exist (userId = -1)
                        val existingGlobal = categoryDao.getAllCategoriesOnce(-1)
                        if (existingGlobal.isEmpty()) {
                            categoryDao.insertCategory(Category(name = "Food", userId = -1))
                            categoryDao.insertCategory(Category(name = "Shopping", userId = -1))
                            categoryDao.insertCategory(Category(name = "Bills", userId = -1))
                            categoryDao.insertCategory(Category(name = "Transport", userId = -1))
                            categoryDao.insertCategory(Category(name = "Other", userId = -1))
                        }
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
            instance = newInstance
            newInstance
        }
    }
}
