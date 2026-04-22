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
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense_database"
            )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Pre-populate default categories to avoid Foreign Key constraint crashes
                    val scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        val database = getDatabase(context)
                        database.categoryDao().insertCategory(Category(id = 1, name = "Food"))
                        database.categoryDao().insertCategory(Category(id = 2, name = "Shopping"))
                        database.categoryDao().insertCategory(Category(id = 3, name = "Bills"))
                        database.categoryDao().insertCategory(Category(id = 4, name = "Transport"))
                        database.categoryDao().insertCategory(Category(id = 5, name = "Other"))
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
        }
        return instance!!
    }
}
