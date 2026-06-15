package com.ntando.expensetracker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Goal
import com.ntando.expensetracker.ui.chat.ChatBottomSheetFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SetGoalsActivity : AppCompatActivity() {

    private var editingGoalId: Int? = null
    private var isFabExpanded = false
    private var categoriesList: List<Category> = emptyList()
    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.goals)

        val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val etGoalName = findViewById<EditText>(R.id.etGoalName)
        val spCategory = findViewById<Spinner>(R.id.spGoalCategory)
        val etTargetAmount = findViewById<EditText>(R.id.etTargetAmount)
        val etMinTargetAmount = findViewById<EditText>(R.id.etMinTargetAmount)
        val etMaxTargetAmount = findViewById<EditText>(R.id.etMaxTargetAmount)
        val etCurrentAmount = findViewById<EditText>(R.id.etCurrentAmount)
        val btnSaveGoals = findViewById<Button>(R.id.btnSaveGoals)

        setupHeader()
        setupNavigation()

        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@SetGoalsActivity)
            // Fix: Load only user's categories
            categoriesList = db.categoryDao().getAllCategoriesOnce(currentUserId)
            
            val categoryNames = mutableListOf("No Category (Savings/Total Budget)")
            categoryNames.addAll(categoriesList.map { it.name })
            
            val adapter = object : ArrayAdapter<String>(this@SetGoalsActivity, android.R.layout.simple_spinner_item, categoryNames) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    (v as? TextView)?.setTextColor(android.graphics.Color.BLACK)
                    return v
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent)
                    (v as? TextView)?.setTextColor(android.graphics.Color.BLACK)
                    return v
                }
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCategory.adapter = adapter

            // Check for edit mode after categories are loaded
            val goalId = intent.getIntExtra("EXTRA_GOAL_ID", -1)
            if (goalId != -1) {
                val goal = db.goalDao().getGoalById(goalId, currentUserId)
                goal?.let {
                    editingGoalId = it.id
                    etGoalName.setText(it.name)
                    etTargetAmount.setText(it.targetAmount.toString())
                    etMinTargetAmount.setText(it.minTargetAmount.toString())
                    etMaxTargetAmount.setText(it.maxTargetAmount.toString())
                    etCurrentAmount.setText(it.currentAmount.toString())
                    
                    it.categoryId?.let { catId ->
                        val index = categoriesList.indexOfFirst { cat -> cat.id == catId }
                        if (index != -1) spCategory.setSelection(index + 1)
                    }
                    
                    btnSaveGoals.text = "Update Goal"
                    findViewById<TextView>(R.id.tvGoalCardTitle)?.text = "Edit Goal"
                }
            }
        }

        btnSaveGoals.setOnClickListener {
            val name = etGoalName.text.toString()
            val target = etTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
            val minTarget = etMinTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
            val maxTarget = etMaxTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
            val current = etCurrentAmount.text.toString().toDoubleOrNull() ?: 0.0
            
            val selectedCatIndex = spCategory.selectedItemPosition
            val categoryId = if (selectedCatIndex > 0) categoriesList[selectedCatIndex - 1].id else null

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a valid goal name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@SetGoalsActivity)
                val newGoal = Goal(
                    id = editingGoalId ?: 0,
                    userId = currentUserId,
                    name = name,
                    targetAmount = target,
                    minTargetAmount = minTarget,
                    maxTargetAmount = maxTarget,
                    currentAmount = current,
                    categoryId = categoryId
                )
                
                if (editingGoalId != null) {
                    db.goalDao().updateGoal(newGoal)
                } else {
                    db.goalDao().insertGoal(newGoal)
                }
                
                val message = if (editingGoalId != null) "Goal updated!" else "Goal added!"
                Toast.makeText(this@SetGoalsActivity, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        setupKeyboardListener()
    }

    private fun setupKeyboardListener() {
        val rootView = findViewById<View>(android.R.id.content)
        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddExpense)
        val chatFab = findViewById<FloatingActionButton>(R.id.fabChatBot)

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) { // Keyboard is shown
                bottomAppBar?.visibility = View.GONE
                fab?.visibility = View.GONE
                chatFab?.visibility = View.GONE
                if (isFabExpanded) collapseFab()
            } else { // Keyboard is hidden
                bottomAppBar?.visibility = View.VISIBLE
                fab?.visibility = View.VISIBLE
                chatFab?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Goals"
        findViewById<View>(R.id.btnHeaderHome)?.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        findViewById<View>(R.id.btnHeaderRightAction)?.setOnClickListener {
            val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
            sharedPref.edit { remove("current_user_id") }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupNavigation() {
        val fabMain = findViewById<FloatingActionButton>(R.id.fabAddExpense)
        val overlay = findViewById<View>(R.id.fabDimOverlay)

        fabMain.setOnClickListener { toggleFab() }
        overlay.setOnClickListener { if (isFabExpanded) collapseFab() }

        findViewById<View>(R.id.miniFabHome).setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }

        findViewById<View>(R.id.miniFabExpense).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
            collapseFab()
            finish()
        }
        
        findViewById<View>(R.id.miniFabReport).setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
            collapseFab()
            finish()
        }
        
        findViewById<View>(R.id.miniFabGoals).setOnClickListener {
            collapseFab()
        }

        findViewById<FloatingActionButton>(R.id.fabChatBot).setOnClickListener {
            ChatBottomSheetFragment().show(supportFragmentManager, "ChatBot")
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_goals
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, Dashboard::class.java)); finish(); true }
                R.id.nav_goals -> true
                R.id.nav_report -> { startActivity(Intent(this, ReportsActivity::class.java)); finish(); true }
                else -> false
            }
        }
    }

    private fun toggleFab() {
        if (isFabExpanded) collapseFab() else expandFab()
    }

    private fun expandFab() {
        isFabExpanded = true
        val fabMain = findViewById<FloatingActionButton>(R.id.fabAddExpense)
        val overlay = findViewById<View>(R.id.fabDimOverlay)

        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(300).start()
        fabMain.animate().rotation(45f).setDuration(300).setInterpolator(OvershootInterpolator()).start()

        animateRadial(findViewById(R.id.containerHome), 0f, -180f, true)
        animateRadial(findViewById(R.id.containerExpense), -100f, -140f, true)
        animateRadial(findViewById(R.id.containerReport), 100f, -140f, true)
        animateRadial(findViewById(R.id.containerGoals), -160f, -20f, true)
    }

    private fun collapseFab() {
        isFabExpanded = false
        val fabMain = findViewById<FloatingActionButton>(R.id.fabAddExpense)
        val overlay = findViewById<View>(R.id.fabDimOverlay)

        overlay.animate().alpha(0f).setDuration(300).withEndAction { overlay.visibility = View.GONE }.start()
        fabMain.animate().rotation(0f).setDuration(300).setInterpolator(OvershootInterpolator()).start()

        animateRadial(findViewById(R.id.containerHome), 0f, 0f, false)
        animateRadial(findViewById(R.id.containerExpense), 0f, 0f, false)
        animateRadial(findViewById(R.id.containerReport), 0f, 0f, false)
        animateRadial(findViewById(R.id.containerGoals), 0f, 0f, false)
    }

    private fun animateRadial(view: View, tx: Float, ty: Float, expand: Boolean) {
        if (expand) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
        }
        
        val density = resources.displayMetrics.density
        val animX = ObjectAnimator.ofFloat(view, "translationX", if (expand) tx * density else 0f)
        val animY = ObjectAnimator.ofFloat(view, "translationY", if (expand) ty * density else 0f)
        val animAlpha = ObjectAnimator.ofFloat(view, "alpha", if (expand) 1f else 0f)
        
        AnimatorSet().apply {
            playTogether(animX, animY, animAlpha)
            duration = 300
            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!expand) view.visibility = View.INVISIBLE
                }
            })
            start()
        }
    }
}
