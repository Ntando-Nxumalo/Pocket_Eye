package com.ntando.expensetracker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.repository.AchievementRepository
import com.ntando.expensetracker.data.repository.ExpenseRepository
import com.ntando.expensetracker.ui.chat.ChatBottomSheetFragment
import com.ntando.expensetracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AddExpenseActivity allows users to log new financial transactions.
 * It handles input for amount, date, time, category, notes, and an optional receipt photo.
 */
class AddExpenseActivity : AppCompatActivity() {

    private val TAG = "AddExpenseActivity"
    private lateinit var viewModel: ExpenseViewModel
    private var selectedDate = Calendar.getInstance()
    private var startTime = Calendar.getInstance()
    private var endTime = Calendar.getInstance()
    private var photoUri: Uri? = null
    private var isFabExpanded = false
    private var categoriesList: List<Category> = emptyList()

    // Launcher for selecting an image from the gallery
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            Log.d(TAG, "Image selected from gallery: $uri")
            photoUri = it
            val ivPhoto = findViewById<ImageView>(R.id.ivExpensePhoto)
            ivPhoto.visibility = View.VISIBLE
            ivPhoto.setImageURI(it)
            
            // Persist permission so the app can load the image again later
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d(TAG, "Persistable URI permission granted for: $it")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to take persistable permission: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: Activity started. Initializing UI and ViewModel.")
        setContentView(R.layout.add_expenses_activity)

        val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            Log.e(TAG, "No authenticated user found. Redirecting to Login.")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        Log.d(TAG, "Authenticated user ID: $currentUserId")

        // Dependency injection for ViewModel
        val db = DatabaseProvider.getDatabase(this)
        val repository = ExpenseRepository(db.expenseDao(), db.categoryDao())
        val achievementRepository = AchievementRepository(db.achievementDao(), db.expenseDao(), db.goalDao())
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository, currentUserId, achievementRepository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[ExpenseViewModel::class.java]

        setupHeader()
        setupNavigation()
        setupForm()
        observeViewModel()
        setupKeyboardListener()
    }

    /**
     * Hides the navigation bar when the keyboard is open to maximize screen space.
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
                    Log.v(TAG, "Keyboard visible: Hiding navigation UI")
                    bottomAppBar.visibility = View.GONE
                    fab?.visibility = View.GONE
                    chatFab?.visibility = View.GONE
                }
            } else {
                if (bottomAppBar?.visibility == View.GONE) {
                    Log.v(TAG, "Keyboard hidden: Showing navigation UI")
                    bottomAppBar.visibility = View.VISIBLE
                    fab?.visibility = View.VISIBLE
                    chatFab?.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Initializes form fields and click listeners for date/time pickers and saving.
     */
    private fun setupForm() {
        Log.d(TAG, "Configuring form inputs and listeners")
        val etDate = findViewById<EditText>(R.id.etDate)
        val etStartTime = findViewById<EditText>(R.id.etStartTime)
        val etEndTime = findViewById<EditText>(R.id.etEndTime)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val spCategory = findViewById<Spinner>(R.id.spCategory)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnAddPhoto = findViewById<Button>(R.id.btnAddPhoto)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnManageCategories = findViewById<Button>(R.id.btnManageCategoriesForm)

        // Date Picker
        etDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day)
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                etDate.setText(dateStr)
                Log.d(TAG, "Date selected: $dateStr")
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Start Time Picker
        etStartTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                startTime.set(Calendar.HOUR_OF_DAY, hour)
                startTime.set(Calendar.MINUTE, minute)
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(startTime.time)
                etStartTime.setText(timeStr)
                Log.d(TAG, "Start time selected: $timeStr")
            }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true).show()
        }

        // End Time Picker
        etEndTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                endTime.set(Calendar.HOUR_OF_DAY, hour)
                endTime.set(Calendar.MINUTE, minute)
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(endTime.time)
                etEndTime.setText(timeStr)
                Log.d(TAG, "End time selected: $timeStr")
            }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true).show()
        }

        btnAddPhoto.setOnClickListener { 
            Log.d(TAG, "Opening gallery for photo selection")
            selectImageLauncher.launch("image/*") 
        }

        btnManageCategories?.setOnClickListener {
            Log.d(TAG, "User clicked Manage Categories. Navigating...")
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (view as? TextView)?.setTextColor(Color.BLACK)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Save Button logic
        btnSave.setOnClickListener {
            val amountStr = etAmount.text.toString()
            val dateStr = etDate.text.toString()
            val startStr = etStartTime.text.toString()
            val endStr = etEndTime.text.toString()
            val note = etNote.text.toString()
            val selectedCategoryName = spCategory.selectedItem?.toString() ?: ""

            Log.i(TAG, "Attempting to save expense. Category: $selectedCategoryName, Amount: $amountStr")

            // Validation
            if (amountStr.isEmpty() || dateStr.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
                Log.w(TAG, "Validation failed: One or more required fields are empty")
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCategoryName.isEmpty()) {
                Log.w(TAG, "Validation failed: No category selected")
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            
            lifecycleScope.launch {
                val categoryId = categoriesList.find { it.name == selectedCategoryName }?.id ?: -1
                
                if (categoryId == -1) {
                    Log.e(TAG, "Error: Category ID not found for name '$selectedCategoryName'")
                    Toast.makeText(this@AddExpenseActivity, "Invalid category", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d(TAG, "Saving to database via ViewModel...")
                // Call ViewModel to save data
                viewModel.addExpense(
                    amount = amount,
                    description = note.ifEmpty { selectedCategoryName },
                    date = dateStr,
                    categoryId = categoryId,
                    startTime = startStr,
                    endTime = endStr,
                    photoPath = photoUri?.toString()
                )
                
                Log.i(TAG, "Expense successfully saved. Returning to previous screen.")
                Toast.makeText(this@AddExpenseActivity, "Expense saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnCancel.setOnClickListener { 
            Log.d(TAG, "User cancelled operation")
            finish() 
        }
    }

    /**
     * Observes the category list from the ViewModel to populate the Spinner.
     */
    private fun observeViewModel() {
        val spCategory = findViewById<Spinner>(R.id.spCategory)
        lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                Log.d(TAG, "Categories updated in database. Count: ${categories.size}")
                categoriesList = categories
                val names = categories.map { it.name }
                
                val adapter = object : ArrayAdapter<String>(this@AddExpenseActivity, android.R.layout.simple_spinner_item, names) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val v = super.getView(position, convertView, parent)
                        (v as TextView).setTextColor(Color.BLACK)
                        return v
                    }
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val v = super.getDropDownView(position, convertView, parent)
                        (v as TextView).setTextColor(Color.BLACK)
                        return v
                    }
                }
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spCategory.adapter = adapter
            }
        }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Add Expense"
        findViewById<View>(R.id.btnHeaderHome)?.setOnClickListener {
            Log.d(TAG, "Header: Home button clicked")
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        findViewById<View>(R.id.btnHeaderRightAction)?.setOnClickListener {
            Log.i(TAG, "Header: Logout initiated")
            val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
            sharedPref.edit { remove("current_user_id") }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    /**
     * Sets up the custom radial menu and bottom navigation.
     */
    private fun setupNavigation() {
        val fabMain = findViewById<FloatingActionButton>(R.id.fabAddExpense)
        val overlay = findViewById<View>(R.id.fabDimOverlay)
        fabMain.setOnClickListener { toggleFab() }
        overlay.setOnClickListener { if (isFabExpanded) collapseFab() }

        findViewById<View>(R.id.miniFabHome).setOnClickListener {
            Log.d(TAG, "Radial Menu: Navigating Home")
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
        findViewById<View>(R.id.miniFabExpense).setOnClickListener { 
            Log.d(TAG, "Radial Menu: Already on Add Expense screen")
            collapseFab() 
        }
        findViewById<View>(R.id.miniFabReport).setOnClickListener {
            Log.d(TAG, "Radial Menu: Navigating to Reports")
            startActivity(Intent(this, ReportsActivity::class.java)); finish()
        }
        findViewById<View>(R.id.miniFabGoals).setOnClickListener {
            Log.d(TAG, "Radial Menu: Navigating to Goals")
            startActivity(Intent(this, SetGoalsActivity::class.java)); finish()
        }

        findViewById<FloatingActionButton>(R.id.fabChatBot).setOnClickListener {
            Log.i(TAG, "ChatBot FAB clicked. Opening AI Assistant.")
            ChatBottomSheetFragment().show(supportFragmentManager, "ChatBot")
        }

        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { 
                    Log.d(TAG, "BottomNav: Home")
                    startActivity(Intent(this, Dashboard::class.java)); finish(); true 
                }
                R.id.nav_goals -> { 
                    Log.d(TAG, "BottomNav: Goals")
                    startActivity(Intent(this, SetGoalsActivity::class.java)); finish(); true 
                }
                R.id.nav_report -> { 
                    Log.d(TAG, "BottomNav: Reports")
                    startActivity(Intent(this, ReportsActivity::class.java)); finish(); true 
                }
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
}
