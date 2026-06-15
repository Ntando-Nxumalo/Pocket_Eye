package com.ntando.expensetracker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Expense
import com.ntando.expensetracker.data.entity.Goal
import com.ntando.expensetracker.data.repository.ExpenseRepository
import com.ntando.expensetracker.ui.chat.ChatBottomSheetFragment
import com.ntando.expensetracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * ReportsActivity provides advanced visualization of spending patterns.
 * Features include category filtering, time-period analysis, bar charts with budget limit lines,
 * and PDF report exportation.
 */
class ReportsActivity : AppCompatActivity() {

    private val TAG = "ReportsActivity"
    private lateinit var viewModel: ExpenseViewModel
    private val selectedCategory = MutableStateFlow(0) // 0 means "All"
    private val selectedTimePeriod = MutableStateFlow(0)
    private val startDateFlow = MutableStateFlow("")
    private val endDateFlow = MutableStateFlow("")
    
    private var filteredExpensesList: List<Expense> = emptyList()
    private var currentUserId: Long = -1
    private var isFabExpanded = false
    private var categoriesList: List<Category> = emptyList()
    private lateinit var expenseAdapter: FilteredExpenseAdapter
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: Initializing Reports screen.")
        setContentView(R.layout.reports)

        val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            Log.e(TAG, "No user ID found, redirecting to login.")
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
        setupNavigation()
        setupFilters()
        setupRecyclerView()
        setupChart()
        observeViewModel()
        setupKeyboardListener()
    }

    /**
     * Optimizes UI space by hiding bottom navigation elements when the keyboard is active.
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

            if (keypadHeight > screenHeight * 0.15) {
                if (bottomAppBar?.visibility == View.VISIBLE) {
                    Log.v(TAG, "Keyboard visible: hiding navigation components")
                    bottomAppBar.visibility = View.GONE
                    fab?.visibility = View.GONE
                    chatFab?.visibility = View.GONE
                }
            } else {
                if (bottomAppBar?.visibility == View.GONE) {
                    Log.v(TAG, "Keyboard hidden: restoring navigation components")
                    bottomAppBar.visibility = View.VISIBLE
                    fab?.visibility = View.VISIBLE
                    chatFab?.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Configures the MPAndroidChart bar chart styling and axes.
     */
    private fun setupChart() {
        Log.d(TAG, "Initializing BarChart component")
        barChart = findViewById(R.id.spendingBarChart)
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.axisRight.isEnabled = false
        
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.textColor = AndroidColor.DKGRAY
        xAxis.textSize = 10f
        
        val leftAxis = barChart.axisLeft
        leftAxis.textColor = AndroidColor.DKGRAY
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = AndroidColor.LTGRAY
        leftAxis.axisMinimum = 0f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "R%.0f".format(value)
            }
        }
        
        barChart.legend.apply {
            form = Legend.LegendForm.CIRCLE
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
        }
        
        barChart.animateY(1000)
    }

    /**
     * Updates the chart with filtered data and draws budget limit lines.
     */
    private fun updateChart(expenses: List<Expense>, goals: List<Goal>, selectedCatIdx: Int) {
        if (expenses.isEmpty()) {
            Log.w(TAG, "No expenses to display on chart.")
            barChart.clear()
            barChart.invalidate()
            return
        }

        Log.i(TAG, "Updating chart with ${expenses.size} expenses and ${goals.size} potential goals.")
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()

        // Group by category and sum amounts for the bars
        val groupedSorted = expenses.groupBy { it.categoryId }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        groupedSorted.forEachIndexed { i, (catId, total) ->
            entries.add(BarEntry(i.toFloat(), total.toFloat()))
            val category = categoriesList.find { it.id == catId }
            labels.add(category?.name ?: "Unknown")
            
            val colorIdx = if (category != null) categoriesList.indexOf(category) else i
            colors.add(getCategoryColor(colorIdx))
        }

        val dataSet = BarDataSet(entries, "Total Spending")
        dataSet.colors = colors
        dataSet.valueTextColor = AndroidColor.BLACK
        dataSet.valueTextSize = 10f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "R%.0f".format(value)
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        barChart.data = barData
        
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = labels.size
        
        // Add dynamic limit lines based on budget goals
        val leftAxis = barChart.axisLeft
        leftAxis.removeAllLimitLines()
        
        val relevantGoals = if (selectedCatIdx == 0) {
            goals.filter { it.categoryId == null && it.maxTargetAmount > 0 }
        } else {
            val selectedId = categoriesList.getOrNull(selectedCatIdx - 1)?.id
            goals.filter { it.categoryId == selectedId && it.maxTargetAmount > 0 }
        }
        
        Log.d(TAG, "Applying ${relevantGoals.size} budget limit lines to chart axes.")
        relevantGoals.forEach { goal ->
            if (goal.minTargetAmount > 0) {
                val minLine = LimitLine(goal.minTargetAmount.toFloat(), "${goal.name} Target Min")
                minLine.lineColor = "#FFA726".toColorInt()
                minLine.lineWidth = 2f
                minLine.enableDashedLine(10f, 10f, 0f)
                minLine.textColor = "#E65100".toColorInt()
                minLine.textSize = 10f
                minLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                leftAxis.addLimitLine(minLine)
            }
            if (goal.maxTargetAmount > 0) {
                val maxLine = LimitLine(goal.maxTargetAmount.toFloat(), "${goal.name} Limit")
                maxLine.lineColor = "#EF5350".toColorInt()
                maxLine.lineWidth = 2f
                maxLine.enableDashedLine(10f, 10f, 0f)
                maxLine.textColor = "#C62828".toColorInt()
                maxLine.textSize = 10f
                maxLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                leftAxis.addLimitLine(maxLine)
            }
        }
        
        barChart.invalidate()
    }

    private fun getCategoryColor(position: Int): Int {
        return when (position % 4) {
            0 -> "#66BB6A".toColorInt()
            1 -> "#2196F3".toColorInt()
            2 -> "#FFD54F".toColorInt()
            else -> "#EF5350".toColorInt()
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView for filtered results.")
        val rv = findViewById<RecyclerView>(R.id.rvFilteredExpenses)
        expenseAdapter = FilteredExpenseAdapter(
            onPhotoClick = { expense -> 
                Log.d(TAG, "User clicked expense photo for ID: ${expense.id}")
                showPhotoDialog(expense) 
            },
            loadThumbnail = { path -> loadBitmapFromUri(path, 100) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = expenseAdapter
    }

    private fun getUri(path: String): Uri {
        return if (path.startsWith("content://") || path.startsWith("file://")) {
            Uri.parse(path)
        } else {
            Uri.fromFile(File(path))
        }
    }

    /**
     * Decodes a bitmap from a Uri, with optional downsampling for thumbnails.
     */
    private fun loadBitmapFromUri(path: String, targetSize: Int = 0): Bitmap? {
        return try {
            val uri = getUri(path)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                if (targetSize > 0) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    
                    options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
                    options.inJustDecodeBounds = false
                    
                    contentResolver.openInputStream(uri)?.use { actualStream ->
                        BitmapFactory.decodeStream(actualStream, null, options)
                    }
                } else {
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error loading receipt bitmap from path $path: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Displays a full-screen preview of a receipt photo.
     */
    private fun showPhotoDialog(expense: Expense) {
        val path = expense.photoPath
        if (path.isNullOrEmpty()) {
            Log.w(TAG, "showPhotoDialog: Attempted to show photo for expense without path.")
            Toast.makeText(this, "No photo attached", Toast.LENGTH_SHORT).show()
            return
        }

        Log.i(TAG, "Opening high-res receipt photo preview.")
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_photo, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.ivFullPhoto)
        
        val frameLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        frameLayout.addView(dialogView)
        
        val progressBar = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER)
            visibility = View.VISIBLE
        }
        frameLayout.addView(progressBar)

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmapFromUri(path)
            }
            progressBar.visibility = View.GONE
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                try {
                    imageView.setImageURI(getUri(path))
                } catch (e: Exception) {
                    Log.e(TAG, "Final fallback failed to load image: ${e.message}")
                    Toast.makeText(this@ReportsActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        AlertDialog.Builder(this)
            .setView(frameLayout)
            .setPositiveButton("Close", null)
            .show()
    }

    /**
     * Initializes all UI filter components (Category, Time Period, Date Pickers).
     */
    private fun setupFilters() {
        Log.d(TAG, "Configuring filter interactions.")
        val spCategory = findViewById<Spinner>(R.id.spCategoryReport)
        val spTimePeriod = findViewById<Spinner>(R.id.spTimePeriodReport)
        val etStart = findViewById<EditText>(R.id.etStartDateReport)
        val etEnd = findViewById<EditText>(R.id.etEndDateReport)
        val btnExport = findViewById<Button>(R.id.btnExportReport)

        val timePeriods = listOf("Last 3 Months", "Last 6 Months", "This Year", "All Time", "Custom Range")
        val timeAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, timePeriods) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as TextView).setTextColor(AndroidColor.BLACK)
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as TextView).setTextColor(AndroidColor.BLACK)
                return v
            }
        }
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTimePeriod.adapter = timeAdapter

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                Log.v(TAG, "Category filter changed to index: $position")
                selectedCategory.value = position
                (view as? TextView)?.setTextColor(AndroidColor.BLACK)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spTimePeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                Log.v(TAG, "Time period filter changed to index: $position")
                selectedTimePeriod.value = position
                (view as? TextView)?.setTextColor(AndroidColor.BLACK)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        etStart.setOnClickListener {
            showDatePicker { date ->
                Log.v(TAG, "Custom start date set: $date")
                etStart.setText(date)
                startDateFlow.value = date
                if (spTimePeriod.selectedItemPosition != 4) {
                    spTimePeriod.setSelection(4)
                }
            }
        }

        etEnd.setOnClickListener {
            showDatePicker { date ->
                Log.v(TAG, "Custom end date set: $date")
                etEnd.setText(date)
                endDateFlow.value = date
                if (spTimePeriod.selectedItemPosition != 4) {
                    spTimePeriod.setSelection(4)
                }
            }
        }

        btnExport.setOnClickListener { 
            Log.i(TAG, "Export PDF button clicked.")
            exportToPdf(filteredExpensesList) 
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val selected = Calendar.getInstance().apply { set(y, m, d) }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            onDateSelected(sdf.format(selected.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    /**
     * Combines multiple StateFlows to reactively update the UI when any filter or data changes.
     */
    private fun observeViewModel() {
        val tvTotalExpenses = findViewById<TextView>(R.id.tvTotalExpensesAmount)
        val tvAverageMonth = findViewById<TextView>(R.id.tvAverageMonthAmount)
        val tvHighestExpense = findViewById<TextView>(R.id.tvHighestExpenseAmount)
        val tvLowestExpense = findViewById<TextView>(R.id.tvLowestExpenseAmount)
        val spCategory = findViewById<Spinner>(R.id.spCategoryReport)
        
        Log.d(TAG, "Establishing reactive data observers.")
        
        lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                Log.v(TAG, "Categories updated in UI spinner.")
                categoriesList = categories
                val names = mutableListOf(getString(R.string.all_categories))
                names.addAll(categories.map { it.name })
                
                val adapter = object : ArrayAdapter<String>(this@ReportsActivity, android.R.layout.simple_spinner_item, names) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val v = super.getView(position, convertView, parent)
                        (v as TextView).setTextColor(AndroidColor.BLACK)
                        return v
                    }
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val v = super.getDropDownView(position, convertView, parent)
                        (v as TextView).setTextColor(AndroidColor.BLACK)
                        return v
                    }
                }
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spCategory.adapter = adapter
            }
        }

        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@ReportsActivity)
            val goalsFlow = db.goalDao().getAllGoals(currentUserId)
            
            val filtersFlow = combine(
                selectedCategory,
                selectedTimePeriod,
                startDateFlow,
                endDateFlow
            ) { cat, time, start, end ->
                FilterParams(cat, time, start, end)
            }

            // Combine all data sources for reactive filtering
            combine(
                viewModel.expenses,
                viewModel.categories,
                filtersFlow,
                goalsFlow
            ) { expenses, categories, filters, goals ->
                val filtered = filterExpenses(expenses, categories, filters.categoryIdx, filters.timeIdx, filters.start, filters.end)
                DataWrapper(filtered, categories, goals, filters)
            }.collectLatest { wrapper ->
                Log.i(TAG, "Applying complex filter. Displaying ${wrapper.expenses.size} expenses based on user criteria.")
                filteredExpensesList = wrapper.expenses
                expenseAdapter.updateData(wrapper.expenses, wrapper.categories)
                updateStats(wrapper.expenses, wrapper.filters, tvTotalExpenses, tvAverageMonth, tvHighestExpense, tvLowestExpense)
                updateChart(wrapper.expenses, wrapper.goals, wrapper.filters.categoryIdx)
            }
        }
    }

    data class FilterParams(val categoryIdx: Int, val timeIdx: Int, val start: String, val end: String)
    data class DataWrapper(val expenses: List<Expense>, val categories: List<Category>, val goals: List<Goal>, val filters: FilterParams)

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = getString(R.string.nav_report)
        findViewById<View>(R.id.btnHeaderHome)?.setOnClickListener {
            Log.d(TAG, "Header: Navigating to Dashboard.")
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        findViewById<View>(R.id.btnHeaderRightAction)?.setOnClickListener {
            Log.i(TAG, "Header: User logout requested.")
            val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
            sharedPref.edit { remove("current_user_id") }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupNavigation() {
        Log.d(TAG, "Configuring radial menu navigation.")
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
        findViewById<View>(R.id.miniFabReport).setOnClickListener { collapseFab() }
        findViewById<View>(R.id.miniFabGoals).setOnClickListener {
            startActivity(Intent(this, SetGoalsActivity::class.java)); finish()
        }

        findViewById<FloatingActionButton>(R.id.fabChatBot).setOnClickListener {
            Log.d(TAG, "FAB: Opening Chat Bot assistant.")
            ChatBottomSheetFragment().show(supportFragmentManager, "ChatBot")
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_report
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { 
                    Log.d(TAG, "BottomNav: Home")
                    startActivity(Intent(this, Dashboard::class.java)); finish(); true 
                }
                R.id.nav_goals -> { 
                    Log.d(TAG, "BottomNav: Goals")
                    startActivity(Intent(this, SetGoalsActivity::class.java)); finish(); true 
                }
                R.id.nav_report -> true
                else -> false
            }
        }
    }

    private fun toggleFab() { if (isFabExpanded) collapseFab() else expandFab() }

    private fun expandFab() {
        Log.v(TAG, "Expanding Radial Menu.")
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
        Log.v(TAG, "Collapsing Radial Menu.")
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

    /**
     * Logic for filtering the list based on category and time period selections.
     */
    private fun filterExpenses(expenses: List<Expense>, categories: List<Category>, categoryIdx: Int, timeIdx: Int, customStart: String, customEnd: String): List<Expense> {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val boundary = when (timeIdx) {
            0 -> Calendar.getInstance().apply { add(Calendar.MONTH, -3); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
            1 -> Calendar.getInstance().apply { add(Calendar.MONTH, -6); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
            else -> null
        }

        val filteredByTime = expenses.filter {
            val expenseDate = try {
                sdf.parse(it.date)
            } catch (_: Exception) { null } ?: return@filter false
            
            when (timeIdx) {
                0, 1 -> !expenseDate.before(boundary)
                2 -> {
                    val cal = Calendar.getInstance().apply { time = expenseDate }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
                3 -> true
                4 -> {
                    if (customStart.isEmpty() && customEnd.isEmpty()) true
                    else {
                        val start = if (customStart.isNotEmpty()) try { sdf.parse(customStart) } catch(_: Exception) { null } else null
                        val end = if (customEnd.isNotEmpty()) try { sdf.parse(customEnd) } catch(_: Exception) { null } else null
                        
                        val afterStart = start?.let { !expenseDate.before(it) } ?: true
                        val beforeEnd = end?.let { !expenseDate.after(it) } ?: true
                        afterStart && beforeEnd
                    }
                }
                else -> true
            }
        }
        
        if (categoryIdx == 0) return filteredByTime
        
        val selectedCategoryId = categories.getOrNull(categoryIdx - 1)?.id ?: -1
        return filteredByTime.filter { it.categoryId == selectedCategoryId }
    }

    /**
     * Calculates and updates the summary statistics (Total, Avg, High, Low).
     */
    private fun updateStats(expenses: List<Expense>, filters: FilterParams, totalTv: TextView, avgTv: TextView, highTv: TextView, lowTv: TextView) {
        val total = expenses.sumOf { it.amount }
        totalTv.text = getString(R.string.currency_format_decimal, total)
        
        Log.d(TAG, "Calculating statistics for summary board.")
        val months = when (filters.timeIdx) {
            0 -> 3.0
            1 -> 6.0
            2 -> (Calendar.getInstance().get(Calendar.MONTH) + 1).toDouble()
            4 -> {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val start = try { sdf.parse(filters.start) } catch(_: Exception) { null }
                val end = try { sdf.parse(filters.end) } catch(_: Exception) { null }
                if (start != null && end != null) {
                    val diff = end.time - start.time
                    val days = diff / (1000 * 60 * 60 * 24)
                    (days / 30.0).coerceAtLeast(1.0)
                } else if (expenses.isNotEmpty()) {
                    val dates = expenses.mapNotNull { try { sdf.parse(it.date) } catch(_: Exception) { null } }
                    if (dates.isNotEmpty()) {
                        val diff = dates.maxOf { it.time } - dates.minOf { it.time }
                        (diff / (1000L * 60 * 60 * 24 * 30)).toDouble().coerceAtLeast(1.0)
                    } else 1.0
                } else 1.0
            }
            else -> {
                if (expenses.isEmpty()) 1.0
                else {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dates = expenses.mapNotNull { try { sdf.parse(it.date) } catch(_: Exception) { null } }
                    if (dates.isEmpty()) 1.0
                    else {
                        val min = dates.minOrNull()!!.time
                        val max = dates.maxOrNull()!!.time
                        val diff = max - min
                        (diff / (1000L * 60 * 60 * 24 * 30)).toDouble().coerceAtLeast(1.0)
                    }
                }
            }
        }

        val avgMonthly = total / months
        avgTv.text = getString(R.string.currency_format_decimal, avgMonthly)
        
        val highest = expenses.maxByOrNull { it.amount }?.amount ?: 0.0
        val lowest = expenses.minByOrNull { it.amount }?.amount ?: 0.0
        highTv.text = getString(R.string.currency_format_decimal, highest)
        lowTv.text = getString(R.string.currency_format_decimal, lowest)
    }

    /**
     * Generates a PDF document with the currently filtered expense list.
     */
    private fun exportToPdf(expenses: List<Expense>) {
        Log.i(TAG, "Generating PDF document for ${expenses.size} expenses.")
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 14f
        canvas.drawText(getString(R.string.pdf_title), 10f, 25f, paint)
        
        paint.textSize = 10f
        var yPos = 50f
        val total = expenses.sumOf { it.amount }
        canvas.drawText("Total spent: R%,.2f".format(total), 10f, yPos, paint)
        yPos += 20f

        expenses.forEach {
            if (yPos < 580f) {
                canvas.drawText("${it.date}: ${it.description} - R${it.amount}", 10f, yPos, paint)
                yPos += 15f
            }
        }

        pdfDocument.finishPage(page)

        val filePath = File(getExternalFilesDir(null), "ExpenseReport.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(filePath))
            Log.i(TAG, "PDF saved to disk: ${filePath.absolutePath}")
            
            val contentUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", filePath)
            
            AlertDialog.Builder(this)
                .setTitle("Export Successful")
                .setMessage("PDF saved successfully.")
                .setPositiveButton("Open Report") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
                    }
                    try {
                        startActivity(Intent.createChooser(intent, "Open with"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start PDF viewer intent: ${e.message}")
                        Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Close", null)
                .show()
                
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during PDF generation/save: ${e.message}")
            Toast.makeText(this, "PDF Export failed", Toast.LENGTH_SHORT).show()
        }
        pdfDocument.close()
    }
}

/**
 * Adapter for showing the list of expenses that match the user's filters.
 */
class FilteredExpenseAdapter(
    private val onPhotoClick: (Expense) -> Unit,
    private val loadThumbnail: (String) -> Bitmap?
) : ListAdapter<Expense, FilteredExpenseAdapter.ViewHolder>(ExpenseDiffCallback()) {
    private var categories = emptyList<Category>()

    fun updateData(newExpenses: List<Expense>, newCategories: List<Category>) {
        categories = newCategories
        submitList(newExpenses)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_expense, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = getItem(position)
        val category = categories.find { it.id == expense.categoryId }
        
        holder.tvDescription.text = expense.description
        holder.tvDate.text = expense.date
        holder.tvCategory.text = category?.name ?: "Unknown"
        holder.tvAmount.text = holder.itemView.context.getString(R.string.currency_format_decimal, expense.amount)
        
        val posInColorList = if (category != null) categories.indexOf(category) else position
        val color = when (posInColorList % 4) {
            0 -> "#66BB6A".toColorInt()
            1 -> "#2196F3".toColorInt()
            2 -> "#FFD54F".toColorInt()
            else -> "#EF5350".toColorInt()
        }
        holder.tvCategory.setTextColor(color)
        
        if (!expense.photoPath.isNullOrEmpty()) {
            holder.ivReceipt.visibility = View.VISIBLE
            val thumbnail = loadThumbnail(expense.photoPath)
            if (thumbnail != null) {
                holder.ivReceipt.setImageBitmap(thumbnail)
                holder.ivReceipt.imageTintList = null
                holder.ivReceipt.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                holder.ivReceipt.setImageResource(android.R.drawable.ic_menu_camera)
                holder.ivReceipt.scaleType = ImageView.ScaleType.FIT_CENTER
                holder.ivReceipt.imageTintList = android.content.res.ColorStateList.valueOf("#0D2B45".toColorInt())
            }

            holder.ivReceipt.setOnClickListener { onPhotoClick(expense) }
            holder.itemView.setOnClickListener { onPhotoClick(expense) }
        } else {
            holder.ivReceipt.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDescription: TextView = view.findViewById(R.id.tvExpenseDescription)
        val tvDate: TextView = view.findViewById(R.id.tvExpenseDate)
        val tvCategory: TextView = view.findViewById(R.id.tvExpenseCategory)
        val tvAmount: TextView = view.findViewById(R.id.tvExpenseAmount)
        val ivReceipt: ImageView = view.findViewById(R.id.ivReceipt)
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense) = oldItem == newItem
    }
}
