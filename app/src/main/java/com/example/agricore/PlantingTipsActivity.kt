package com.example.agricore

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class PlantingTipsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PlantingTipsActivity"
    }

    // Views
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingView: View

    // Data
    private lateinit var tipsAdapter: PlantingTipsAdapter
    private var allTips = mutableListOf<PlantingTip>()
    private var currentTips = mutableListOf<PlantingTip>()
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var selectedCategory = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planting_tip)

        try {
            setupToolbar()
            initializeViews()
            setupRecyclerView()
            setupCategoryChips()
            loadPlantingTips()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Failed to initialize planting tips page")
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Planting Tips"
        }
    }

    private fun initializeViews() {
        chipGroup = findViewById(R.id.chip_group_categories)
        recyclerView = findViewById(R.id.rv_planting_tips)
        loadingView = findViewById(R.id.layout_loading)
    }

    private fun setupRecyclerView() {
        tipsAdapter = PlantingTipsAdapter { tip ->
            // Handle tip click - could expand/collapse or show details
            Toast.makeText(this, "Tip: ${tip.title}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.apply {
            adapter = tipsAdapter
            layoutManager = LinearLayoutManager(this@PlantingTipsActivity)
            setHasFixedSize(true)
        }
    }

    private fun setupCategoryChips() {
        val categories = listOf("All", "Vegetables", "Fruits", "Grains", "Seasonal", "General")

        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                isChecked = category == selectedCategory

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        filterTipsByCategory(category)
                        updateChipStates(category)
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateChipStates(selectedCategory: String) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.isChecked = chip.text == selectedCategory
        }
    }

    private fun filterTipsByCategory(category: String) {
        selectedCategory = category

        val filteredTips = if (category == "All") {
            allTips.toList()
        } else {
            allTips.filter { it.category.equals(category, ignoreCase = true) }
        }

        currentTips.clear()
        currentTips.addAll(filteredTips)
        tipsAdapter.submitList(currentTips.toList())
    }

    private fun loadPlantingTips() {
        showLoading(true)

        activityScope.launch {
            try {
                delay(1000) // Simulate loading

                val tips = generatePlantingTips()

                withContext(Dispatchers.Main) {
                    allTips.clear()
                    allTips.addAll(tips)

                    currentTips.clear()
                    currentTips.addAll(allTips)

                    tipsAdapter.submitList(currentTips.toList())
                    showLoading(false)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading planting tips", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to load planting tips")
                    showLoading(false)
                }
            }
        }
    }

    private fun generatePlantingTips(): List<PlantingTip> {
        val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())

        return listOf(
            PlantingTip(
                id = 1,
                title = "Best Time to Plant Tomatoes",
                description = "Plant tomatoes after the last frost date in your area. Soil temperature should be at least 60°F (16°C).",
                category = "Vegetables",
                season = "Spring",
                difficulty = "Beginner",
                tips = listOf(
                    "Start seeds indoors 6-8 weeks before last frost",
                    "Harden off seedlings for a week before transplanting",
                    "Choose a sunny location with well-draining soil",
                    "Space plants 24-36 inches apart"
                ),
                isCurrentSeason = isCurrentSeason("Spring")
            ),
            PlantingTip(
                id = 2,
                title = "Maize Planting Guidelines",
                description = "Maize thrives in warm weather and needs plenty of space and nutrients for optimal growth.",
                category = "Grains",
                season = "Rainy Season",
                difficulty = "Intermediate",
                tips = listOf(
                    "Plant when soil temperature reaches 60°F (16°C)",
                    "Sow seeds 1-2 inches deep",
                    "Space rows 30-36 inches apart",
                    "Apply nitrogen fertilizer when plants are 6 inches tall",
                    "Ensure consistent moisture during tasseling"
                ),
                isCurrentSeason = isCurrentSeason("Rainy Season")
            ),
            PlantingTip(
                id = 3,
                title = "Okra Growing Tips",
                description = "Okra is a heat-loving vegetable perfect for warm climates. It's drought-tolerant once established.",
                category = "Vegetables",
                season = "Summer",
                difficulty = "Beginner",
                tips = listOf(
                    "Soak seeds overnight before planting",
                    "Plant in full sun location",
                    "Space plants 12-18 inches apart",
                    "Harvest pods when 3-4 inches long",
                    "Pick regularly to encourage continued production"
                ),
                isCurrentSeason = isCurrentSeason("Summer")
            ),
            PlantingTip(
                id = 4,
                title = "Cassava Cultivation",
                description = "Cassava is a drought-resistant root crop that provides excellent yields in tropical climates.",
                category = "Root Crops",
                season = "All Year",
                difficulty = "Beginner",
                tips = listOf(
                    "Plant stem cuttings 6-8 inches long",
                    "Choose well-draining, sandy soil",
                    "Plant at 45-degree angle",
                    "Harvest after 8-12 months",
                    "Can tolerate poor soil conditions"
                ),
                isCurrentSeason = true
            ),
            PlantingTip(
                id = 5,
                title = "Seasonal Crop Rotation",
                description = "Proper crop rotation maintains soil health and reduces pest and disease problems.",
                category = "General",
                season = "All Seasons",
                difficulty = "Advanced",
                tips = listOf(
                    "Follow legumes with heavy feeders like corn",
                    "Plant root crops after leafy vegetables",
                    "Include cover crops in rotation",
                    "Keep detailed planting records",
                    "Allow some plots to rest each season"
                ),
                isCurrentSeason = true
            ),
            PlantingTip(
                id = 6,
                title = "Water Management in $currentMonth",
                description = "Proper watering techniques for the current season to maximize crop yield and water efficiency.",
                category = "Seasonal",
                season = currentMonth,
                difficulty = "Intermediate",
                tips = listOf(
                    "Water early morning to reduce evaporation",
                    "Use mulch to retain soil moisture",
                    "Install drip irrigation for efficiency",
                    "Monitor soil moisture at root level",
                    "Adjust watering based on weather conditions"
                ),
                isCurrentSeason = true
            )
        )
    }

    private fun isCurrentSeason(season: String): Boolean {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        return when (season) {
            "Spring" -> currentMonth in 2..4  // March-May
            "Summer" -> currentMonth in 5..7  // June-August
            "Rainy Season" -> currentMonth in 3..9  // April-October (Nigeria)
            "All Year", "All Seasons" -> true
            else -> false
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            loadingView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}

// Data class
data class PlantingTip(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val season: String,
    val difficulty: String,
    val tips: List<String>,
    val isCurrentSeason: Boolean = false
)