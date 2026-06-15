package com.ntando.expensetracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.repository.ExpenseRepository
import com.ntando.expensetracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManageCategoriesActivity : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private lateinit var adapter: CategoryAdapter
    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_categories)

        val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val db = DatabaseProvider.getDatabase(this)
        val repository = ExpenseRepository(db.expenseDao(), db.categoryDao())
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository, currentUserId) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[ExpenseViewModel::class.java]

        setupHeader()
        setupUI()
        observeViewModel()
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Categories"
        findViewById<View>(R.id.btnHeaderHome)?.setOnClickListener {
            finish()
        }
        findViewById<View>(R.id.btnHeaderRightAction)?.setOnClickListener {
            val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
            sharedPref.edit { remove("current_user_id") }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupUI() {
        val etNewCategory = findViewById<EditText>(R.id.etNewCategory)
        val btnAddCategory = findViewById<ImageButton>(R.id.btnAddCategory)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)

        adapter = CategoryAdapter()
        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = adapter

        btnAddCategory.setOnClickListener {
            val name = etNewCategory.text.toString().trim()
            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    val db = DatabaseProvider.getDatabase(this@ManageCategoriesActivity)
                    db.categoryDao().insertCategory(Category(name = name, userId = currentUserId))
                    etNewCategory.setText("")
                    Toast.makeText(this@ManageCategoriesActivity, "Category added", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                adapter.submitList(categories)
            }
        }
    }

    inner class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        private var categories = emptyList<Category>()

        fun submitList(newList: List<Category>) {
            categories = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            (holder.itemView as TextView).apply {
                text = category.name
                setTextColor(Color.BLACK)
            }
        }

        override fun getItemCount() = categories.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
