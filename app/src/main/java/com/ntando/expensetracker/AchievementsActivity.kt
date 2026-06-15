package com.ntando.expensetracker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Achievement
import com.ntando.expensetracker.data.repository.AchievementRepository
import com.ntando.expensetracker.ui.chat.ChatBottomSheetFragment
import com.ntando.expensetracker.viewmodel.AchievementViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AchievementsActivity : AppCompatActivity() {

    private lateinit var viewModel: AchievementViewModel
    private var currentUserId: Long = -1
    private var isFabExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val db = DatabaseProvider.getDatabase(this)
        val repository = AchievementRepository(db.achievementDao(), db.expenseDao(), db.goalDao())
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AchievementViewModel(repository, currentUserId) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[AchievementViewModel::class.java]

        setupHeader()
        setupNavigation()
        setupRecyclerView()
        setupKeyboardListener()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvAchievements)
        val tvStats = findViewById<TextView>(R.id.tvAchievementStats)
        val adapter = AchievementAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        lifecycleScope.launch {
            viewModel.achievements.collectLatest { list ->
                adapter.submitList(list)
                val unlocked = list.count { it.isUnlocked }
                tvStats.text = getString(R.string.achievements_unlocked_format, unlocked, list.size)
            }
        }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Achievements"
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
            startActivity(Intent(this, Dashboard::class.java)); finish()
        }
        findViewById<View>(R.id.miniFabExpense).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java)); finish()
        }
        findViewById<View>(R.id.miniFabReport).setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java)); finish()
        }
        findViewById<View>(R.id.miniFabGoals).setOnClickListener {
            startActivity(Intent(this, SetGoalsActivity::class.java)); finish()
        }

        findViewById<FloatingActionButton>(R.id.fabChatBot).setOnClickListener {
            ChatBottomSheetFragment().show(supportFragmentManager, "ChatBot")
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = 0 // None selected
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, Dashboard::class.java)); finish(); true }
                R.id.nav_goals -> { startActivity(Intent(this, SetGoalsActivity::class.java)); finish(); true }
                R.id.nav_report -> { startActivity(Intent(this, ReportsActivity::class.java)); finish(); true }
                else -> false
            }
        }
    }

    private fun toggleFab() { if (isFabExpanded) collapseFab() else expandFab() }

    private fun expandFab() {
        isFabExpanded = true
        findViewById<View>(R.id.fabDimOverlay).apply {
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(300).start()
        }
        findViewById<FloatingActionButton>(R.id.fabAddExpense).animate().rotation(45f).setDuration(300).start()
        animateRadial(findViewById(R.id.containerHome), 0f, -180f, true)
        animateRadial(findViewById(R.id.containerExpense), -100f, -140f, true)
        animateRadial(findViewById(R.id.containerReport), 100f, -140f, true)
        animateRadial(findViewById(R.id.containerGoals), -160f, -20f, true)
    }

    private fun collapseFab() {
        isFabExpanded = false
        findViewById<View>(R.id.fabDimOverlay).animate().alpha(0f).setDuration(300).withEndAction { 
            findViewById<View>(R.id.fabDimOverlay).visibility = View.GONE 
        }.start()
        findViewById<FloatingActionButton>(R.id.fabAddExpense).animate().rotation(0f).setDuration(300).start()
        animateRadial(findViewById(R.id.containerHome), 0f, 0f, false)
        animateRadial(findViewById(R.id.containerExpense), 0f, 0f, false)
        animateRadial(findViewById(R.id.containerReport), 0f, 0f, false)
        animateRadial(findViewById(R.id.containerGoals), 0f, 0f, false)
    }

    private fun animateRadial(view: View, tx: Float, ty: Float, expand: Boolean) {
        if (expand) { view.visibility = View.VISIBLE; view.alpha = 0f }
        val density = resources.displayMetrics.density
        val animX = ObjectAnimator.ofFloat(view, "translationX", if (expand) tx * density else 0f)
        val animY = ObjectAnimator.ofFloat(view, "translationY", if (expand) ty * density else 0f)
        val animAlpha = ObjectAnimator.ofFloat(view, "alpha", if (expand) 1f else 0f)
        AnimatorSet().apply {
            playTogether(animX, animY, animAlpha)
            duration = 300
            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { if (!expand) view.visibility = View.INVISIBLE }
            })
            start()
        }
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

            if (keypadHeight > screenHeight * 0.15) {
                bottomAppBar?.visibility = View.GONE
                fab?.visibility = View.GONE
                chatFab?.visibility = View.GONE
            } else {
                bottomAppBar?.visibility = View.VISIBLE
                fab?.visibility = View.VISIBLE
                chatFab?.visibility = View.VISIBLE
            }
        }
    }
}

class AchievementAdapter : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {
    private var items = emptyList<Achievement>()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun submitList(newItems: List<Achievement>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.description
        
        if (item.isUnlocked) {
            holder.ivIcon.alpha = 1.0f
            holder.ivLock.visibility = View.GONE
            holder.tvDate.visibility = View.VISIBLE
            holder.tvDate.text = "Unlocked: ${item.dateUnlocked?.let { sdf.format(Date(it)) } ?: "N/A"}"
            holder.container.alpha = 1.0f
        } else {
            holder.ivIcon.alpha = 0.3f
            holder.ivLock.visibility = View.VISIBLE
            holder.tvDate.visibility = View.GONE
            holder.container.alpha = 0.6f
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.containerAchievement)
        val ivIcon: ImageView = view.findViewById(R.id.ivAchievementIcon)
        val ivLock: ImageView = view.findViewById(R.id.ivLockStatus)
        val tvTitle: TextView = view.findViewById(R.id.tvAchievementTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvAchievementDesc)
        val tvDate: TextView = view.findViewById(R.id.tvUnlockedDate)
    }
}
