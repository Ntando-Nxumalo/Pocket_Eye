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
                        val existing = categoryDao.getAllCategoriesOnce()
                        if (existing.isEmpty()) {
                            categoryDao.insertCategory(Category(id = 1, name = "Food"))
                            categoryDao.insertCategory(Category(id = 2, name = "Shopping"))
                            categoryDao.insertCategory(Category(id = 3, name = "Bills"))
                            categoryDao.insertCategory(Category(id = 4, name = "Transport"))
                            categoryDao.insertCategory(Category(id = 5, name = "Other"))
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
