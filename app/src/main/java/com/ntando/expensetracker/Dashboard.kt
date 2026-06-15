/**
 * Dashboard.kt
 * Main screen of the Pocket Eye app.
 * Displays spending summary, gamification XP bar, savings goal charts,
 * budget tracker, and a currency converter.
 * 
 * Features:
 * - Radial navigation menu
 * - Jetpack Compose charts (Spending & Savings)
 * - Real-time currency conversion
 * - XP and Leveling system logic with celebrations
 * 
 * References:
 * - Jetpack Compose Canvas: https://developer.android.com/jetpack/compose/graphics/draw/overview
 * - Android MVVM Pattern: https://developer.android.com/topic/libraries/architecture/viewmodel
 * - Animated Radial Menu: Inspired by Material Design FAB patterns.
 */
package com.ntando.expensetracker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Goal
import com.ntando.expensetracker.data.repository.AchievementRepository
import com.ntando.expensetracker.data.repository.ExpenseRepository
import com.ntando.expensetracker.ui.chat.ChatBottomSheetFragment
import com.ntando.expensetracker.viewmodel.CurrencyViewModel
import com.ntando.expensetracker.viewmodel.ExpenseEvent
import com.ntando.expensetracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Extension function to capitalize the first character of a string.
 */
fun String.customCapitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

class Dashboard : AppCompatActivity() {

    private val TAG = "DashboardActivity"
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var currencyViewModel: CurrencyViewModel
    private var currentUserId: Long = -1
    private var isBalanceVisible = true
    private var originalBalanceText = ""
    private var isFabExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: Initializing Dashboard Activity")
        setContentView(R.layout.activity_home)

        // Retrieve the current user ID from SharedPreferences
        val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        // Redirect to Login if no user is found
        if (currentUserId == -1L) {
            Log.e(TAG, "Authentication failed: No user ID found in session. Redirecting to Login.")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        Log.d(TAG, "Authenticated user found with ID: $currentUserId")

        // Initialize Database and Repositories
        val db = DatabaseProvider.getDatabase(this)
        val repository = ExpenseRepository(db.expenseDao(), db.categoryDao())
        val achievementRepository = AchievementRepository(db.achievementDao(), db.expenseDao(), db.goalDao())
        
        // Setup ViewModel with Factory for dependency injection
        Log.d(TAG, "Setting up ViewModels with Repository Pattern")
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository, currentUserId, achievementRepository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[ExpenseViewModel::class.java]
        currencyViewModel = ViewModelProvider(this)[CurrencyViewModel::class.java]

        // Setup UI components and observers
        setupUI(db)
        setupNavigation()
        setupCurrencyConverter()
        observeViewModel()
        setupKeyboardListener()
        
        Log.i(TAG, "Dashboard initialization complete.")
    }

    /**
     * Listens for keyboard visibility to hide bottom bars and FABs, preventing UI overlap.
     */
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
                if (bottomAppBar?.visibility == View.VISIBLE) {
                    Log.v(TAG, "Keyboard detected: Dynamically hiding navigation bars to maximize screen space.")
                    bottomAppBar.visibility = View.GONE
                    fab?.visibility = View.GONE
                    chatFab?.visibility = View.GONE
                    if (isFabExpanded) collapseFab()
                }
            } else { // Keyboard is hidden
                if (bottomAppBar?.visibility == View.GONE) {
                    Log.v(TAG, "Keyboard hidden: Restoring navigation bars.")
                    bottomAppBar.visibility = View.VISIBLE
                    fab?.visibility = View.VISIBLE
                    chatFab?.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Initializes core UI components, click listeners, and charts.
     */
    private fun setupUI(db: com.ntando.expensetracker.data.database.AppDatabase) {
        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val ivShowHide = findViewById<ImageView>(R.id.ivShowHideBalance)
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val btnLogout = findViewById<View>(R.id.btnHeaderRightAction)
        val btnEditGoals = findViewById<ImageButton>(R.id.btnEditGoals)
        val btnManageCategories = findViewById<ImageButton>(R.id.btnManageCategories)
        val xpCard = findViewById<View>(R.id.xpCard)

        // Toggle balance visibility for privacy
        ivShowHide.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            Log.d(TAG, "User toggled balance privacy. Visible: $isBalanceVisible")
            if (isBalanceVisible) {
                tvBalance.text = originalBalanceText
                ivShowHide.setImageResource(android.R.drawable.ic_menu_view)
            } else {
                originalBalanceText = tvBalance.text.toString()
                tvBalance.text = "••••••••"
                ivShowHide.setImageResource(android.R.drawable.ic_partial_secure)
            }
        }

        // Fetch and display user name from Room Database
        lifecycleScope.launch {
            db.userDao().getUserById(currentUserId).collect { user ->
                Log.v(TAG, "Greeting user: ${user?.name}")
                tvHeaderTitle.text = user?.name?.customCapitalize() ?: "User"
            }
        }

        btnLogout.setOnClickListener {
            Log.i(TAG, "User Logout requested. Clearing preferences.")
            val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
            sharedPref.edit { remove("current_user_id") }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnEditGoals?.setOnClickListener { 
            Log.d(TAG, "Navigating to Goals management screen")
            startActivity(Intent(this, SetGoalsActivity::class.java)) 
        }
        btnManageCategories?.setOnClickListener { 
            Log.d(TAG, "Navigating to Category management screen")
            startActivity(Intent(this, ManageCategoriesActivity::class.java)) 
        }
        xpCard?.setOnClickListener { 
            Log.d(TAG, "Navigating to Achievements screen")
            startActivity(Intent(this, AchievementsActivity::class.java)) 
        }

        findViewById<View>(R.id.ivLogo)?.setOnClickListener { if (isFabExpanded) collapseFab() }

        setupCharts(db)
        setupRecyclerView()
    }

    /**
     * Sets up the custom multi-currency converter tool on the dashboard.
     */
    private fun setupCurrencyConverter() {
        Log.i(TAG, "Initializing Integrated Multi-Currency Converter")
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etResult = findViewById<EditText>(R.id.etResult)
        val spFrom = findViewById<Spinner>(R.id.spFromCurrency)
        val spTo = findViewById<Spinner>(R.id.spToCurrency)
        val btnSwap = findViewById<ImageButton>(R.id.btnSwap)

        if (etAmount == null || etResult == null || spFrom == null || spTo == null) {
            Log.w(TAG, "Currency converter views missing in layout. Skipping setup.")
            return
        }

        etAmount.setTextColor(AndroidColor.BLACK)
        etAmount.setHintTextColor(AndroidColor.BLACK)
        etResult.setTextColor(AndroidColor.BLACK)
        etResult.setHintTextColor(AndroidColor.BLACK)

        val currencies = listOf("ZAR", "USD", "EUR", "GBP", "AUD", "CNY")

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currencies) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.setTextColor(AndroidColor.BLACK)
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.setTextColor(AndroidColor.BLACK)
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spFrom.adapter = adapter
        spTo.adapter = adapter

        spFrom.setSelection(0) // ZAR default
        spTo.setSelection(1)   // USD default

        val updateConversion = {
            val from = spFrom.selectedItem?.toString() ?: "ZAR"
            val to = spTo.selectedItem?.toString() ?: "USD"
            val amountStr = etAmount.text.toString()
            if (amountStr.isNotEmpty()) {
                Log.v(TAG, "Triggering real-time conversion: $amountStr $from to $to")
                currencyViewModel.convert(amountStr, from, to)
            } else {
                etResult.setText("")
            }
        }

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateConversion()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                updateConversion()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        spFrom.onItemSelectedListener = listener
        spTo.onItemSelectedListener = listener

        btnSwap?.setOnClickListener {
            Log.v(TAG, "Swapping currency positions")
            val f = spFrom.selectedItemPosition
            val t = spTo.selectedItemPosition
            spFrom.setSelection(t)
            spTo.setSelection(f)
        }

        currencyViewModel.convertedAmount.observe(this) { result ->
            etResult.setText(result)
        }
        
        if (etAmount.text.isEmpty()) {
            etAmount.setText(getString(R.string.default_amount))
        }
        updateConversion()
    }

    /**
     * Initializes modern Jetpack Compose-based charts and interactive budget trackers.
     */
    private fun setupCharts(db: com.ntando.expensetracker.data.database.AppDatabase) {
        Log.i(TAG, "Rendering Hybrid UI: Injecting Jetpack Compose Charts into XML Layout")
        
        // Savings Goals Horizontal Scroll
        findViewById<ComposeView>(R.id.cvGoalsCharts).setContent {
            val goals by db.goalDao().getSavingsGoals(currentUserId).collectAsState(initial = emptyList())
            Log.v(TAG, "Compose: Updating Savings Goals charts. Found ${goals.size} active goals.")
            
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (goals.isEmpty()) {
                    item {
                        PremiumGoalChart(percentage = 0.6f, label = "Sample Goal", color = Color(0xFF66BB6A), savedAmount = 6000.0, targetAmount = 10000.0, onEditClick = {
                            startActivity(Intent(this@Dashboard, SetGoalsActivity::class.java))
                        })
                    }
                } else {
                    itemsIndexed(goals) { index, goal ->
                        val color = when (index % 4) {
                            0 -> Color(0xFF66BB6A)
                            1 -> Color(0xFFFFD54F)
                            2 -> Color(0xFF2196F3)
                            else -> Color(0xFFEF5350)
                        }
                        val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                        PremiumGoalChart(
                            percentage = progress,
                            label = goal.name,
                            color = color,
                            savedAmount = goal.currentAmount,
                            targetAmount = goal.targetAmount,
                            onEditClick = {
                                Log.d(TAG, "User clicked goal ${goal.name}. Navigating to editor.")
                                val intent = Intent(this@Dashboard, SetGoalsActivity::class.java)
                                intent.putExtra("EXTRA_GOAL_ID", goal.id)
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // Spending Categories Donut Chart
        findViewById<ComposeView>(R.id.cvSpendingChart).setContent {
            val summaries by viewModel.categorySummaries.collectAsState()
            val total by viewModel.totalSpending.collectAsState()
            val categories by viewModel.categories.collectAsState()
            Log.v(TAG, "Compose: Re-drawing Spending Donut Chart. Total Spending: R$total")
            PremiumSpendingChart(summaries, total, categories)
        }

        // Budget Tracker Progress Bars
        findViewById<ComposeView>(R.id.cvBudgetTracker).setContent {
            val monthSummaries by viewModel.currentMonthCategorySummaries.collectAsState()
            val totalMonthSpending by viewModel.currentMonthTotalSpending.collectAsState()
            val goals by db.goalDao().getAllGoals(currentUserId).collectAsState(initial = emptyList())
            val categories by viewModel.categories.collectAsState()
            
            Log.v(TAG, "Compose: Updating Monthly Budget Tracker")
            BudgetTracker(monthSummaries, totalMonthSpending, goals, categories, onEditGoal = { goalId ->
                val intent = Intent(this@Dashboard, SetGoalsActivity::class.java)
                intent.putExtra("EXTRA_GOAL_ID", goalId)
                startActivity(intent)
            })
        }
    }

    /**
     * Sets up the RecyclerView for detailed spending breakdown.
     */
    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvSpendingDetails)
        rv.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            viewModel.categorySummaries.collectLatest { summaries ->
                Log.d(TAG, "Updating Spending Details list. Categories: ${summaries.size}")
                val total = summaries.sumOf { it.totalAmount }
                val categories = DatabaseProvider.getDatabase(this@Dashboard).categoryDao().getAllCategoriesOnce(currentUserId)
                rv.adapter = SpendingAdapter(summaries, total, categories)
            }
        }
    }

    /**
     * Configures the unique Radial Menu and standard bottom navigation.
     */
    private fun setupNavigation() {
        Log.d(TAG, "Initializing Navigation Systems (Radial Menu & BottomBar)")
        val fabMain = findViewById<FloatingActionButton>(R.id.fabAddExpense)
        val overlay = findViewById<View>(R.id.fabDimOverlay)

        fabMain.setOnClickListener { toggleFab() }
        overlay.setOnClickListener { if (isFabExpanded) collapseFab() }

        findViewById<View>(R.id.miniFabHome).setOnClickListener { 
            Log.d(TAG, "Radial Menu: Home clicked")
            collapseFab() 
        }

        findViewById<View>(R.id.miniFabExpense).setOnClickListener {
            Log.d(TAG, "Radial Menu: Navigating to Add Expense")
            startActivity(Intent(this, AddExpenseActivity::class.java))
            collapseFab()
        }

        findViewById<View>(R.id.miniFabReport).setOnClickListener {
            Log.d(TAG, "Radial Menu: Navigating to Reports")
            startActivity(Intent(this, ReportsActivity::class.java))
            collapseFab()
        }

        findViewById<View>(R.id.miniFabGoals).setOnClickListener {
            Log.d(TAG, "Radial Menu: Navigating to Goals")
            startActivity(Intent(this, SetGoalsActivity::class.java))
            collapseFab()
        }

        findViewById<FloatingActionButton>(R.id.fabChatBot).setOnClickListener {
            Log.i(TAG, "PocketEye AI Assistant opened.")
            ChatBottomSheetFragment().show(supportFragmentManager, "ChatBot")
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { Log.d(TAG, "BottomNav: Home selected"); true }
                R.id.nav_goals -> { 
                    Log.d(TAG, "BottomNav: Goals selected")
                    startActivity(Intent(this, SetGoalsActivity::class.java)); false
                }
                R.id.nav_report -> { 
                    Log.d(TAG, "BottomNav: Reports selected")
                    startActivity(Intent(this, ReportsActivity::class.java)); false 
                }
                else -> false
            }
        }
    }

    private fun toggleFab() { if (isFabExpanded) collapseFab() else expandFab() }

    /**
     * Animates the radial menu items outwards with an Overshoot effect.
     */
    private fun expandFab() {
        Log.v(TAG, "Radial Menu expanded.")
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

    /**
     * Animates the radial menu items back to the center.
     */
    private fun collapseFab() {
        Log.v(TAG, "Radial Menu collapsed.")
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

    /**
     * Helper to animate individual radial menu buttons using ObjectAnimators.
     */
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
                override fun onAnimationEnd(animation: Animator) {
                    if (!expand) view.visibility = View.INVISIBLE
                }
            })
            start()
        }
    }

    /**
     * Observes ViewModel flows for spending, XP, and leveling events.
     * This is the bridge between our Repository data and the Dashboard UI.
     */
    private fun observeViewModel() {
        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val tvXP = findViewById<TextView>(R.id.tvXP)
        val progressLevel = findViewById<LinearProgressIndicator>(R.id.progressLevel)
        val tvLevelLabel = findViewById<TextView>(R.id.tvLevelLabel)

        // Observe total spending
        lifecycleScope.launch {
            viewModel.totalSpending.collect { total ->
                val formatted = "R%,.2f".format(total)
                if (isBalanceVisible) tvBalance.text = formatted else originalBalanceText = formatted
            }
        }
        
        // Observe gamification progress and update XP bar
        lifecycleScope.launch {
            viewModel.expenseCount.collect { count ->
                val expensesPerLevel = 5
                val currentLevel = (count / expensesPerLevel) + 1
                val progress = (count % expensesPerLevel) * 20 // 20 XP per expense
                
                Log.d(TAG, "XP Progress Update: $progress/100, Level $currentLevel")
                tvXP.text = getString(R.string.xp_progress_format, progress)
                progressLevel.progress = progress
                tvLevelLabel.text = getString(R.string.level_format, currentLevel)
            }
        }

        // Observe special events like leveling up or achievements for user celebration
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                Log.i(TAG, "Significant Event Observed: ${event.javaClass.simpleName}")
                when (event) {
                    is ExpenseEvent.LevelUp -> {
                        showCelebrationDialog("Level Up!", "Congratulations! You've reached Level ${event.newLevel}. Your financial management skills are improving!")
                    }
                    is ExpenseEvent.AchievementUnlocked -> {
                        showCelebrationDialog("Badge Unlocked!", "You've earned the '${event.title}' badge! Keep logging your progress.")
                    }
                }
            }
        }
    }

    /**
     * Shows a popup dialog to celebrate user milestones and maintain engagement.
     */
    private fun showCelebrationDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Keep going!") { dialog, _ -> dialog.dismiss() }
            .setIcon(android.R.drawable.btn_star_big_on)
            .show()
    }
}

/**
 * Adapter for showing spending breakdown by category in a list.
 */
class SpendingAdapter(
    private val summaries: List<CategorySummary>,
    private val total: Double,
    private val categories: List<Category>
) : RecyclerView.Adapter<SpendingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvAmount: TextView = view.findViewById(R.id.tvCategoryAmount)
        val progress: LinearProgressIndicator = view.findViewById(R.id.categoryProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_spending_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val summary = summaries[position]
        val category = categories.find { it.id == summary.categoryId }
        holder.tvName.text = category?.name ?: "Unknown"
        holder.tvAmount.text = "R%,.2f".format(summary.totalAmount)
        val percentage = if (total > 0) (summary.totalAmount / total * 100).toInt() else 0
        holder.progress.progress = percentage

        val colorIdx = if (category != null) categories.indexOf(category) else position
        val color = when (colorIdx % 4) {
            0 -> AndroidColor.parseColor("#66BB6A")
            1 -> AndroidColor.parseColor("#2196F3")
            2 -> AndroidColor.parseColor("#FFD54F")
            else -> AndroidColor.parseColor("#EF5350")
        }
        holder.progress.setIndicatorColor(color)
        holder.ivIcon.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun getItemCount() = summaries.size
}

/**
 * Compose-based circular progress chart for savings goals.
 */
@Composable
fun PremiumGoalChart(percentage: Float, label: String, color: Color, savedAmount: Double, targetAmount: Double, onEditClick: () -> Unit) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(percentage) {
        animatedProgress.animateTo(percentage, animationSpec = tween(durationMillis = 1000))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier.padding(6.dp).clickable { onEditClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center, 
            modifier = Modifier
                .size(90.dp)
                .background(Color.White, CircleShape)
        ) {
            Canvas(modifier = Modifier.size(75.dp)) {
                val sw = 7.dp.toPx()
                drawCircle(
                    color = Color(0xFFF5F5F5),
                    style = Stroke(width = sw)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360 * animatedProgress.value,
                    useCenter = false,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D2B45)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D2B45))
        val locale = Locale.getDefault()
        Text(text = "R${String.format(locale, "%,.0f", savedAmount)} / R${String.format(locale, "%,.0f", targetAmount)}", fontSize = 11.sp, color = Color.Gray)
    }
}

/**
 * Compose-based donut chart for spending distribution.
 */
@Composable
fun PremiumSpendingChart(summaries: List<CategorySummary>, total: Double, categories: List<Category>) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(summaries) {
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.size(180.dp)) {
            var currentAngle = -90f
            if (summaries.isEmpty()) {
                drawCircle(color = Color.LightGray.copy(alpha = 0.2f), style = Stroke(width = 20.dp.toPx()))
            } else {
                summaries.forEachIndexed { index, summary ->
                    val sweep = (summary.totalAmount / total).toFloat() * 360f * animatedProgress.value
                    val category = categories.find { it.id == summary.categoryId }
                    val colorIdx = if (category != null) categories.indexOf(category) else index
                    val color = when (colorIdx % 4) {
                        0 -> Color(0xFF66BB6A)
                        1 -> Color(0xFF2196F3)
                        2 -> Color(0xFFFFD54F)
                        else -> Color(0xFFEF5350)
                    }
                    drawArc(
                        color = color,
                        startAngle = currentAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                    currentAngle += (summary.totalAmount / total).toFloat() * 360f
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", fontSize = 14.sp, color = Color.Gray)
            Text("R%,.0f".format(total), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D2B45))
        }
    }
}

/**
 * Compose-based budget tracker showing progress against set limits.
 */
@Composable
fun BudgetTracker(
    summaries: List<CategorySummary>,
    totalSpentThisMonth: Double,
    goals: List<Goal>,
    categories: List<Category>,
    onEditGoal: (Int) -> Unit
) {
    val budgetGoals = goals.filter { it.maxTargetAmount > 0 }
    val totalBudgetGoal = budgetGoals.find { it.categoryId == null }
    val categoryBudgets = budgetGoals.filter { it.categoryId != null }
    val totalMaxBudget = totalBudgetGoal?.maxTargetAmount ?: categoryBudgets.sumOf { it.maxTargetAmount }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = totalBudgetGoal != null) {
                    totalBudgetGoal?.let { onEditGoal(it.id) }
                },
            color = Color(0xFFE8F5E9),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = totalBudgetGoal?.name ?: "Monthly Budget Summary",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Spent R%,.2f of R%,.2f budgeted this month".format(totalSpentThisMonth, totalMaxBudget),
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val overallProgress = if (totalMaxBudget > 0) (totalSpentThisMonth / totalMaxBudget).toFloat().coerceIn(0f, 1.1f) else 0f
                val overallColor = if (overallProgress >= 1f) Color(0xFFEF5350) else if (overallProgress > 0.8f) Color(0xFFFFA726) else Color(0xFF66BB6A)
                
                LinearProgressIndicator(
                    progress = { overallProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = overallColor,
                    trackColor = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        categoryBudgets.forEach { goal ->
            val category = categories.find { it.id == goal.categoryId }
            val categoryName = category?.name ?: goal.name
            val spentInCategory = summaries.find { it.categoryId == goal.categoryId }?.totalAmount ?: 0.0
            val maxGoal = goal.maxTargetAmount
            val minGoal = goal.minTargetAmount
            val progress = if (maxGoal > 0) (spentInCategory / maxGoal).toFloat() else 0f
            
            val barColor = when {
                spentInCategory >= maxGoal -> Color(0xFFEF5350) 
                minGoal > 0 && spentInCategory >= minGoal -> Color(0xFFFFA726)
                progress >= 0.85f -> Color(0xFFFFA726)
                else -> Color(0xFF66BB6A)
            }

            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable { onEditGoal(goal.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = categoryName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0D2B45)
                        )
                        if (minGoal > 0) {
                            Text(
                                text = "Target: R%.0f - R%.0f".format(minGoal, maxGoal),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Text(
                        text = "R%,.0f / R%,.0f".format(spentInCategory, maxGoal),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (spentInCategory >= maxGoal) Color(0xFFEF5350) else Color(0xFF0D2B45)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 1000)
                )

                val animatedColor by animateColorAsState(
                    targetValue = barColor,
                    animationSpec = tween(durationMillis = 500)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF0F0F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(animatedColor)
                    )
                }
            }
        }
        
        if (budgetGoals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category budgets set for this month.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
