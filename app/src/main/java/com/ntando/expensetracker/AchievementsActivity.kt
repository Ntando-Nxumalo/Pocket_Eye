/**
 * AchievementsActivity.kt
 * Displays the user's earned badges and locked achievements.
 * Maps achievement data to visual icons and handles unlocking progress.
 * 
 * References:
 * - RecyclerView Adapter: https://developer.android.com/guide/topics/ui/layout/recyclerview
 * - Android Vector Assets: https://developer.android.com/develop/ui/views/graphics/vector-drawable-resources
 */
package com.ntando.expensetracker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
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

/**
 * AchievementsActivity displays the user's "gamified" progress.
 * It lists all possible badges and shows which ones have been unlocked based on their activity.
 */
class AchievementsActivity : AppCompatActivity() {

    private val TAG = "AchievementsActivity"
    private lateinit var viewModel: AchievementViewModel
    private var currentUserId: Long = -1
    private var isFabExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Achievements screen")
        setContentView(R.layout.activity_achievements)

        val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            Log.w(TAG, "No user session found, redirecting")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Setup ViewModel with dependency injection
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
        
        // Safety check to ensure achievements are initialized for the user
        lifecycleScope.launch {
            val existing = db.achievementDao().getAllAchievementsOnce(currentUserId)
            if (existing.isEmpty()) {
                Log.i(TAG, "No achievements found for user $currentUserId, seeding defaults")
                val initialAchievements = listOf(
                    Achievement(userId = currentUserId, title = "First Step", description = "Log your first expense", icon = "star"),
                    Achievement(userId = currentUserId, title = "Week Warrior", description = "Log expenses for 7 consecutive days", icon = "bolt"),
                    Achievement(userId = currentUserId, title = "Budget Boss", description = "Stay within your monthly goal for the full month", icon = "wallet"),
                    Achievement(userId = currentUserId, title = "Consistent Tracker", description = "Log at least one expense every day for 30 days", icon = "calendar")
                )
                initialAchievements.forEach { db.achievementDao().insertAchievement(it) }
            }
        }
    }

    /**
     * Initializes the RecyclerView to display the list of achievements.
     */
    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvAchievements)
        val tvStats = findViewById<TextView>(R.id.tvAchievementStats)
        val adapter = AchievementAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // Observe the achievements flow from ViewModel
        lifecycleScope.launch {
            viewModel.achievements.collectLatest { list ->
                Log.d(TAG, "Updating UI with ${list.size} achievements")
                adapter.submitList(list)
                val unlocked = list.count { it.isUnlocked }
                tvStats.text = getString(R.string.achievements_unlocked_format, unlocked, list.size)
            }
        }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Achievements"
        findViewById<View>(R.id.btnHeaderHome)?.setOnClickListener {
            Log.v(TAG, "Header Home clicked")
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        findViewById<View>(R.id.btnHeaderRightAction)?.setOnClickListener {
            Log.i(TAG, "Logging out from Achievements screen")
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
            Log.d(TAG, "Opening Chat Bot")
            ChatBottomSheetFragment().show(supportFragmentManager, "ChatBot")
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = 0 
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
        Log.v(TAG, "Expanding Radial Menu")
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
        Log.v(TAG, "Collapsing Radial Menu")
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

/**
 * Adapter for rendering individual achievement items.
 * Handles visual state for locked vs unlocked achievements (greyscale vs colored).
 */
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
        
        // Map string-based icon keys to actual Android resources
        val iconRes = when (item.icon) {
            "star" -> android.R.drawable.btn_star_big_on
            "bolt" -> android.R.drawable.ic_menu_send
            "wallet" -> android.R.drawable.ic_menu_view
            "calendar" -> android.R.drawable.ic_menu_today
            else -> android.R.drawable.btn_star_big_on
        }
        holder.ivIcon.setImageResource(iconRes)
        
        // If unlocked, show colored and display the date
        if (item.isUnlocked) {
            holder.ivIcon.alpha = 1.0f
            holder.ivLock.visibility = View.GONE
            holder.tvDate.visibility = View.VISIBLE
            holder.tvDate.text = "Unlocked: ${item.dateUnlocked?.let { sdf.format(Date(it)) } ?: "N/A"}"
            holder.container.alpha = 1.0f
            holder.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD700"))
        } else {
            // If locked, show greyed out with a lock icon
            holder.ivIcon.alpha = 0.3f
            holder.ivLock.visibility = View.VISIBLE
            holder.tvDate.visibility = View.GONE
            holder.container.alpha = 0.6f
            holder.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
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
