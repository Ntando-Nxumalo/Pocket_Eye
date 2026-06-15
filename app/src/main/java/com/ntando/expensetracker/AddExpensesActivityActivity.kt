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

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private var selectedDate = Calendar.getInstance()
    private var startTime = Calendar.getInstance()
    private var endTime = Calendar.getInstance()
    private var photoUri: Uri? = null
    private var isFabExpanded = false
    private var categoriesList: List<Category> = emptyList()

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            photoUri = it
            val ivPhoto = findViewById<ImageView>(R.id.ivExpensePhoto)
            ivPhoto.visibility = View.VISIBLE
            ivPhoto.setImageURI(it)
            
            // Attempt to take persistable permission for gallery images
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Not all providers support persistable permissions, ignoring
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_expenses_activity)

        val sharedPref = getSharedPreferences("PocketEyePrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

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
            } else { // Keyboard is hidden
                bottomAppBar?.visibility = View.VISIBLE
                fab?.visibility = View.VISIBLE
                chatFab?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupForm() {
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

        etDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day)
                etDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time))
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        etStartTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                startTime.set(Calendar.HOUR_OF_DAY, hour)
                startTime.set(Calendar.MINUTE, minute)
                etStartTime.setText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(startTime.time))
            }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true).show()
        }

        etEndTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                endTime.set(Calendar.HOUR_OF_DAY, hour)
                endTime.set(Calendar.MINUTE, minute)
                etEndTime.setText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(endTime.time))
            }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true).show()
        }

        btnAddPhoto.setOnClickListener { selectImageLauncher.launch("image/*") }

        btnManageCategories?.setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (view as? TextView)?.setTextColor(Color.BLACK)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener {
            val amountStr = etAmount.text.toString()
            val dateStr = etDate.text.toString()
            val startStr = etStartTime.text.toString()
            val endStr = etEndTime.text.toString()
            val note = etNote.text.toString()
            val selectedCategoryName = spCategory.selectedItem?.toString() ?: ""

            if (amountStr.isEmpty() || dateStr.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCategoryName.isEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            
            lifecycleScope.launch {
                val categoryId = categoriesList.find { it.name == selectedCategoryName }?.id ?: -1
                
                if (categoryId == -1) {
                    Toast.makeText(this@AddExpenseActivity, "Invalid category", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                viewModel.addExpense(
                    amount = amount,
                    description = note.ifEmpty { selectedCategoryName },
                    date = dateStr,
                    categoryId = categoryId,
                    startTime = startStr,
                    endTime = endStr,
                    photoPath = photoUri?.toString()
                )
                
                Toast.makeText(this@AddExpenseActivity, "Expense saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnCancel.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        val spCategory = findViewById<Spinner>(R.id.spCategory)
        lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
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
        findViewById<View>(R.id.miniFabExpense).setOnClickListener { collapseFab() }
        findViewById<View>(R.id.miniFabReport).setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java)); finish()
        }
        findViewById<View>(R.id.miniFabGoals).setOnClickListener {
            startActivity(Intent(this, SetGoalsActivity::class.java)); finish()
        }

        findViewById<FloatingActionButton>(R.id.fabChatBot).setOnClickListener {
            ChatBottomSheetFragment().show(supportFragmentManager, "ChatBot")
        }

        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener { item ->
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
}
