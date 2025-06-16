package com.example.agricore

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*


class ProductActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ProductActivity"
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_SEARCH_QUERY = "search_query"
    }

    // Views
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var emptyStateView: View
    private lateinit var loadingView: View
    private lateinit var bottomNavigation: BottomNavigationView

    // Quick access cards - These don't exist in your layout, so we'll make them optional
    private var seedsQuickAccess: View? = null
    private var fertilizersQuickAccess: View? = null
    private var toolsQuickAccess: View? = null
    private var pesticidesQuickAccess: View? = null

    // Adapter and data
    private lateinit var productAdapter: ProductAdapter
    private var apiHelper: ApiHelper? = null

    // State - Use mutable lists to prevent concurrent modification
    private var currentProducts = mutableListOf<Product>()
    private var allProducts = mutableListOf<Product>()
    private var categories = mutableListOf<Category>()
    private var isGridView = false
    private var selectedCategoryId: Int = -1
    private var currentSearchQuery = ""
    private var isLoading = false

    // Coroutines
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        try {
            initializeViews()
            setupToolbar()
            setupScrolling()
            setupRecyclerView()
            setupSwipeRefresh()
            setupBottomNavigation()
            setupQuickAccessCards()

            // Initialize API helper only if needed
            if (shouldUseApi()) {
                apiHelper = ApiHelper(this)
            }

            // Handle intent extras
            handleIntentExtras()

            // Load initial data
            loadProductsData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Failed to initialize: ${e.localizedMessage}")
        }
    }

    private fun initializeViews() {
        try {
            // The actual ID from your XML layout is without underscore
            nestedScrollView = findViewById(R.id.nested_scroll_view)
                ?: throw IllegalStateException("NestedScrollView not found in layout")

            swipeRefreshLayout = findViewById(R.id.swipe_refresh)
                ?: throw IllegalStateException("SwipeRefreshLayout not found in layout")

            // Use the correct ID from your XML
            recyclerView = findViewById(R.id.recycler_view_products)
                ?: throw IllegalStateException("RecyclerView not found in layout")

            // Chip group from your XML
            chipGroup = findViewById(R.id.chip_group_categories)

            // These are the actual state views from your XML
            emptyStateView = findViewById(R.id.layout_empty_state)
            loadingView = findViewById(R.id.layout_loading)

            // Bottom navigation from your XML
            bottomNavigation = findViewById(R.id.bottom_navigation)

            // Quick access cards - these are LinearLayouts in your XML
            seedsQuickAccess = findViewById(R.id.ll_seeds_quick_access)
            fertilizersQuickAccess = findViewById(R.id.ll_fertilizers_quick_access)
            toolsQuickAccess = findViewById(R.id.ll_tools_quick_access)
            pesticidesQuickAccess = findViewById(R.id.ll_pesticides_quick_access)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun setupScrolling() {
        try {
            // Enable smooth scrolling for NestedScrollView
            nestedScrollView.isSmoothScrollingEnabled = true
            nestedScrollView.isNestedScrollingEnabled = true

            // Optional: Add scroll listener for advanced behavior
            nestedScrollView.setOnScrollChangeListener { _: NestedScrollView, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
                // Handle scroll events if needed (e.g., show/hide FAB, toolbar effects)
                handleScrollChange(scrollY, oldScrollY)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up scrolling", e)
        }
    }

    private fun handleScrollChange(scrollY: Int, oldScrollY: Int) {
        // Optional: Implement scroll-based UI changes
        // Example: Hide/show FAB based on scroll direction
        try {
            val fab = findViewById<FloatingActionButton?>(R.id.fab_cart)
            fab?.let {
                if (scrollY > oldScrollY && scrollY > 100) {
                    // Scrolling down - hide FAB
                    it.hide()
                } else if (scrollY < oldScrollY) {
                    // Scrolling up - show FAB
                    it.show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling scroll change", e)
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "AgriCore"
            setDisplayHomeAsUpEnabled(intent.hasExtra(EXTRA_CATEGORY_ID) || intent.hasExtra(EXTRA_SEARCH_QUERY))
        }
    }

    private fun setupRecyclerView() {
        try {
            productAdapter = ProductAdapter(
                context = this,
                onProductClick = { product -> openProductDetails(product) },
                onFavoriteClick = { product -> toggleFavorite(product) },
                onAddToCartClick = { product -> addToCart(product) }
            )

            recyclerView.apply {
                adapter = productAdapter
                // IMPORTANT: Disable nested scrolling for RecyclerView inside NestedScrollView
                isNestedScrollingEnabled = false
                // Add item animator to prevent flicker during updates
                itemAnimator = null
                // Set fixed size for better performance
                setHasFixedSize(true)
                // Enable overscroll mode for better UX
                overScrollMode = View.OVER_SCROLL_NEVER
            }

            updateLayoutManager()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
            throw e
        }
    }

    private fun setupSwipeRefresh() {
        try {
            swipeRefreshLayout.setOnRefreshListener {
                refreshProducts()
            }

            // Customize swipe refresh colors
            swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up swipe refresh", e)
        }
    }

    private fun refreshProducts() {
        try {
            // Reset current state
            selectedCategoryId = -1
            currentSearchQuery = ""

            // Reload data
            loadProductsData()

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing products", e)
            swipeRefreshLayout.isRefreshing = false
            showError("Failed to refresh products")
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Scroll to top smoothly
                    nestedScrollView.smoothScrollTo(0, 0)
                    true
                }
                R.id.nav_weather -> {
                    navigateToWeather()
                    true
                }
                R.id.nav_tips -> {
                    navigateToPlantingTips()
                    true
                }
                R.id.nav_profile -> {
                    // Navigate to About Activity
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupQuickAccessCards() {
        // Add debug logging to see if cards are found
        Log.d(TAG, "Setting up quick access cards...")
        Log.d(TAG, "Available categories: ${categories.map { "${it.name}(${it.id})" }}")

        // Setup quick access cards - these are the LinearLayouts in your XML
        seedsQuickAccess?.setOnClickListener {
            Log.d(TAG, "Seeds card clicked!")
            // Try to find a matching category or use a fallback
            val categoryId = getCategoryIdByName("Seeds")
            if (categoryId == -1) {
                // Fallback to Vegetables since seeds are often vegetables
                val fallbackId = getCategoryIdByName("Vegetables")
                Log.d(TAG, "Seeds category not found, using Vegetables fallback: $fallbackId")
                if (fallbackId != -1) {
                    filterByCategory(fallbackId)
                } else {
                    Toast.makeText(this, "Seeds category not available", Toast.LENGTH_SHORT).show()
                }
            } else {
                filterByCategory(categoryId)
            }
        }

        fertilizersQuickAccess?.setOnClickListener {
            Log.d(TAG, "Fertilizers card clicked!")
            val categoryId = getCategoryIdByName("Fertilizers")
            if (categoryId == -1) {
                // Show message or implement custom logic
                Toast.makeText(this, "Fertilizers category coming soon", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Fertilizers category not found")
            } else {
                filterByCategory(categoryId)
            }
        }

        toolsQuickAccess?.setOnClickListener {
            Log.d(TAG, "Tools card clicked!")
            val categoryId = getCategoryIdByName("Tools")
            if (categoryId == -1) {
                Toast.makeText(this, "Tools category coming soon", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Tools category not found")
            } else {
                filterByCategory(categoryId)
            }
        }

        pesticidesQuickAccess?.setOnClickListener {
            Log.d(TAG, "Pesticides card clicked!")
            val categoryId = getCategoryIdByName("Pesticides")
            if (categoryId == -1) {
                Toast.makeText(this, "Pesticides category coming soon", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Pesticides category not found")
            } else {
                filterByCategory(categoryId)
            }
        }


        findViewById<View?>(R.id.btn_search)?.setOnClickListener {
            Log.d(TAG, "Search button clicked!")
            openSearch()
        }

        // Log which quick access views were found
        Log.d(TAG, "Quick access views found:")
        Log.d(TAG, "Seeds: ${seedsQuickAccess != null}")
        Log.d(TAG, "Fertilizers: ${fertilizersQuickAccess != null}")
        Log.d(TAG, "Tools: ${toolsQuickAccess != null}")
        Log.d(TAG, "Pesticides: ${pesticidesQuickAccess != null}")
    }

    private fun getCategoryIdByName(categoryName: String): Int {
        val foundCategory = categories.find { it.name.equals(categoryName, ignoreCase = true) }
        val categoryId = foundCategory?.id ?: -1
        Log.d(TAG, "Looking for category '$categoryName': found ID $categoryId")
        return categoryId
    }


    private fun openSearch() {
        // Trigger search action
        onSearchRequested()
    }

    private fun shouldUseApi(): Boolean {
        // Determine if we should use API based on context
        return intent.hasExtra(EXTRA_CATEGORY_ID) ||
                intent.hasExtra(EXTRA_SEARCH_QUERY) ||
                intent.getBooleanExtra("use_api", false)
    }

    private fun handleIntentExtras() {
        selectedCategoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        currentSearchQuery = intent.getStringExtra(EXTRA_SEARCH_QUERY) ?: ""

        // Update title based on context
        when {
            currentSearchQuery.isNotEmpty() -> {
                supportActionBar?.title = "Search: $currentSearchQuery"
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            selectedCategoryId != -1 -> {
                supportActionBar?.title = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Products"
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            else -> {
                supportActionBar?.title = "AgriCore"
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
        }
    }

    private fun loadProductsData() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping duplicate request")
            return
        }

        showLoading(true)
        isLoading = true

        if (apiHelper != null) {
            // Load from API
            apiHelper?.let { api ->
                activityScope.launch {
                    try {
                        // Load data in background
                        val productsDeferred = async(Dispatchers.IO) {
                            when {
                                currentSearchQuery.isNotEmpty() -> api.searchProducts(currentSearchQuery)
                                selectedCategoryId != -1 -> api.getProductsByCategory(selectedCategoryId)
                                else -> api.getProducts()
                            }
                        }

                        val categoriesDeferred = async(Dispatchers.IO) { api.getCategories() }

                        // Wait for both to complete
                        val loadedProducts = productsDeferred.await()
                        val loadedCategories = categoriesDeferred.await()

                        // Update data safely on main thread
                        withContext(Dispatchers.Main) {
                            updateProductData(loadedProducts, loadedCategories)
                        }

                        Log.d(TAG, "Loaded ${allProducts.size} products and ${categories.size} categories")

                    } catch (e: CancellationException) {
                        Log.d(TAG, "Loading cancelled")
                        isLoading = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading products data", e)
                        withContext(Dispatchers.Main) {
                            showError("Failed to load products: ${e.localizedMessage}")
                            loadFallbackData()
                        }
                    }
                }
            }
        } else {
            // Load static/fallback data
            loadFallbackData()
        }
    }

    private fun updateProductData(loadedProducts: List<Product>, loadedCategories: List<Category>) {
        allProducts.clear()
        allProducts.addAll(loadedProducts)

        categories.clear()
        categories.addAll(loadedCategories)

        currentProducts.clear()
        currentProducts.addAll(allProducts)

        updateProductList()
        setupCategoryChips()
        showLoading(false)
        isLoading = false

        // Stop swipe refresh animation
        swipeRefreshLayout.isRefreshing = false
    }

    private fun loadFallbackData() {
        // Load static data when API is not available
        try {
            val fallbackProducts = getFallbackProducts()
            val fallbackCategories = getFallbackCategories()

            updateProductData(fallbackProducts, fallbackCategories)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fallback data", e)
            showError("Failed to load products")
            showLoading(false)
            isLoading = false
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getFallbackProducts(): List<Product> {
        return listOf(
            Product(
                id = 1,
                name = "Organic Tomatoes",
                description = "Fresh organic produce from local farms. Rich in vitamins and perfect for cooking.",
                price = 2.99,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Organic",
                imageRes = "ic_tomatoes",
                rating = 4.8,
                reviewCount = 24,
                badge = "Organic",
                inStock = true,
                stockQuantity = 150,
                discount = 0,
                isFavorite = false,
                tags = listOf("organic", "fresh", "local", "vitamin-rich")
            ),
            Product(
                id = 2,
                name = "Sweet Carrots",
                description = "Sweet farm-fresh carrots, perfect for snacking and cooking. High in beta-carotene.",
                price = 1.49,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Root Vegetables",
                imageRes = "ic_carrot",
                rating = 4.6,
                reviewCount = 18,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 200,
                discount = 10,
                isFavorite = false,
                tags = listOf("sweet", "fresh", "beta-carotene", "crunchy")
            ),
            Product(
                id = 3,
                name = "Red Apples",
                description = "Juicy red apples, perfect for snacking. Crisp texture with natural sweetness.",
                price = 3.99,
                priceUnit = "per kg",
                category = "Fruits",
                subcategory = "Tree Fruits",
                imageRes = "ic_apple",
                rating = 4.9,
                reviewCount = 45,
                badge = "Premium",
                inStock = true,
                stockQuantity = 120,
                discount = 0,
                isFavorite = true,
                tags = listOf("juicy", "sweet", "crisp", "premium")
            ),
            Product(
                id = 4,
                name = "Russet Potatoes",
                description = "Organic russet potatoes, great for cooking, baking, and making fries.",
                price = 1.29,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Root Vegetables",
                imageRes = "ic_potato",
                rating = 4.5,
                reviewCount = 32,
                badge = "Organic",
                inStock = true,
                stockQuantity = 300,
                discount = 5,
                isFavorite = false,
                tags = listOf("organic", "versatile", "cooking", "baking")
            ),
            Product(
                id = 5,
                name = "Baby Spinach",
                description = "Fresh baby spinach leaves, perfect for salads and smoothies. Packed with iron.",
                price = 2.49,
                priceUnit = "per bundle",
                category = "Vegetables",
                subcategory = "Leafy Greens",
                imageRes = "ic_spinach",
                rating = 4.7,
                reviewCount = 28,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 80,
                discount = 0,
                isFavorite = false,
                tags = listOf("baby", "fresh", "iron-rich", "salad")
            ),
            Product(
                id = 6,
                name = "Organic Lettuce",
                description = "Crisp fresh lettuce leaves, perfect for salads and sandwiches.",
                price = 1.99,
                priceUnit = "per head",
                category = "Vegetables",
                subcategory = "Leafy Greens",
                imageRes = "ic_lettuce",
                rating = 4.4,
                reviewCount = 15,
                badge = "Organic",
                inStock = true,
                stockQuantity = 60,
                discount = 0,
                isFavorite = false,
                tags = listOf("organic", "crisp", "fresh", "salad")
            ),
            Product(
                id = 7,
                name = "Bell Peppers Mix",
                description = "Colorful bell peppers mix - red, yellow, and green. Perfect for cooking and salads.",
                price = 3.49,
                priceUnit = "per pack",
                category = "Vegetables",
                subcategory = "Peppers",
                imageRes = "ic_bell_pepper",
                rating = 4.6,
                reviewCount = 21,
                badge = "Premium",
                inStock = true,
                stockQuantity = 90,
                discount = 15,
                isFavorite = true,
                tags = listOf("colorful", "mix", "vitamin-c", "premium")
            ),
            Product(
                id = 8,
                name = "Green Cucumbers",
                description = "Fresh green cucumbers, perfect for salads, sandwiches, and healthy snacking.",
                price = 1.79,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Vine Vegetables",
                imageRes = "ic_cucumber",
                rating = 4.3,
                reviewCount = 19,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 140,
                discount = 0,
                isFavorite = false,
                tags = listOf("fresh", "hydrating", "low-calorie", "crunchy")
            ),
            Product(
                id = 9,
                name = "Fresh Broccoli",
                description = "Fresh green broccoli crowns, packed with nutrients and perfect for steaming or stir-frying.",
                price = 2.29,
                priceUnit = "per head",
                category = "Vegetables",
                subcategory = "Cruciferous",
                imageRes = "ic_broccoli",
                rating = 4.5,
                reviewCount = 26,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 75,
                discount = 0,
                isFavorite = false,
                tags = listOf("fresh", "nutritious", "vitamin-rich", "superfood")
            ),
            Product(
                id = 10,
                name = "Sweet Corn",
                description = "Fresh sweet corn on the cob, perfect for grilling, boiling, or roasting.",
                price = 0.79,
                priceUnit = "per ear",
                category = "Vegetables",
                subcategory = "Grain Vegetables",
                imageRes = "ic_corn",
                rating = 4.7,
                reviewCount = 33,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 180,
                discount = 0,
                isFavorite = false,
                tags = listOf("sweet", "fresh", "grilling", "summer")
            ),
            Product(
                id = 11,
                name = "Red Onions",
                description = "Fresh red onions with mild flavor, perfect for salads, cooking, and caramelizing.",
                price = 1.19,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Bulb Vegetables",
                imageRes = "ic_onion",
                rating = 4.2,
                reviewCount = 14,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 220,
                discount = 0,
                isFavorite = false,
                tags = listOf("red", "mild", "cooking", "versatile")
            ),
            Product(
                id = 12,
                name = "Green Beans",
                description = "Tender green beans, perfect for steaming, sautéing, or adding to casseroles.",
                price = 2.99,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Pod Vegetables",
                imageRes = "ic_green_beans",
                rating = 4.4,
                reviewCount = 22,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 110,
                discount = 0,
                isFavorite = false,
                tags = listOf("tender", "green", "fresh", "versatile")
            ),
            Product(
                id = 13,
                name = "Golden Bananas",
                description = "Ripe golden bananas, perfect for snacking, smoothies, and baking.",
                price = 1.89,
                priceUnit = "per bunch",
                category = "Fruits",
                subcategory = "Tropical Fruits",
                imageRes = "ic_banana",
                rating = 4.8,
                reviewCount = 67,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 95,
                discount = 0,
                isFavorite = true,
                tags = listOf("golden", "ripe", "potassium", "energy")
            ),
            Product(
                id = 14,
                name = "Organic Strawberries",
                description = "Sweet organic strawberries, perfect for desserts, smoothies, and snacking.",
                price = 4.99,
                priceUnit = "per basket",
                category = "Fruits",
                subcategory = "Berries",
                imageRes = "ic_strawberry",
                rating = 4.9,
                reviewCount = 89,
                badge = "Organic",
                inStock = true,
                stockQuantity = 45,
                discount = 20,
                isFavorite = true,
                tags = listOf("organic", "sweet", "antioxidants", "premium")
            ),
            Product(
                id = 15,
                name = "Fresh Lemons",
                description = "Juicy fresh lemons, perfect for cooking, drinks, and adding zest to dishes.",
                price = 2.49,
                priceUnit = "per kg",
                category = "Fruits",
                subcategory = "Citrus Fruits",
                imageRes = "ic_lemon",
                rating = 4.6,
                reviewCount = 31,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 160,
                discount = 0,
                isFavorite = false,
                tags = listOf("juicy", "citrus", "vitamin-c", "zesty")
            )
        )
    }

    private fun getFallbackCategories(): List<Category> {
        return listOf(
            Category(1, "Vegetables", 15),
            Category(2, "Fruits", 12),
            Category(3, "Dairy", 8),
            Category(4, "Meat", 6)
        )
    }


    private fun setupCategoryChips() {
        if (categories.isEmpty()) return

        try {
            chipGroup.removeAllViews()

            // Add "All" chip
            val allChip = createCategoryChip("All", -1, selectedCategoryId == -1)
            chipGroup.addView(allChip)

            // Add category chips
            categories.forEach { category ->
                val chip = createCategoryChip(
                    text = "${category.name} (${category.count})",
                    categoryId = category.id,
                    isSelected = category.id == selectedCategoryId
                )
                chipGroup.addView(chip)
            }

            chipGroup.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up category chips", e)
        }
    }

    private fun createCategoryChip(text: String, categoryId: Int, isSelected: Boolean): Chip {
        return Chip(this).apply {
            this.text = text
            isCheckable = true
            isChecked = isSelected

            if (isSelected) {
                setChipBackgroundColorResource(android.R.color.holo_blue_light)
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }

            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    filterByCategory(categoryId)
                    updateChipStates(categoryId)
                }
            }
        }
    }

    private fun updateChipStates(selectedCategoryId: Int) {
        try {
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                val isThisChip = when (i) {
                    0 -> selectedCategoryId == -1 // "All" chip
                    else -> categories.getOrNull(i - 1)?.id == selectedCategoryId
                }

                chip.isChecked = isThisChip

                if (isThisChip) {
                    chip.setChipBackgroundColorResource(android.R.color.holo_blue_light)
                    chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                } else {
                    chip.setChipBackgroundColorResource(android.R.color.transparent)
                    chip.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating chip states", e)
        }
    }

    private fun filterByCategory(categoryId: Int) {
        try {
            selectedCategoryId = categoryId

            val filteredProducts = if (categoryId == -1) {
                allProducts.toList()
            } else {
                allProducts.filter { product ->
                    categories.find { it.id == categoryId }?.name?.let { categoryName ->
                        product.category.equals(categoryName, ignoreCase = true)
                    } ?: false
                }
            }

            // Update current products safely
            currentProducts.clear()
            currentProducts.addAll(filteredProducts)

            updateProductList()

            // Scroll to top after filtering
            nestedScrollView.smoothScrollTo(0, 0)

            // Update title
            val categoryName = if (categoryId == -1) {
                "All Products"
            } else {
                categories.find { it.id == categoryId }?.name ?: "Products"
            }
            supportActionBar?.title = categoryName
        } catch (e: Exception) {
            Log.e(TAG, "Error filtering by category", e)
            showError("Filter failed")
        }
    }

    private fun updateProductList() {
        try {
            if (currentProducts.isEmpty()) {
                showEmptyState(true)
                productAdapter.submitList(emptyList())
            } else {
                showEmptyState(false)
                productAdapter.submitList(currentProducts.toList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product list", e)
            showEmptyState(true)
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (show) {
                loadingView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                chipGroup.visibility = View.GONE
                emptyStateView.visibility = View.GONE
            } else {
                loadingView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                if (categories.isNotEmpty()) {
                    chipGroup.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading state", e)
        }
    }

    private fun showEmptyState(show: Boolean) {
        try {
            if (show && !isLoading) {
                emptyStateView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing empty state", e)
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleViewType() {
        try {
            isGridView = !isGridView
            updateLayoutManager()
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling view type", e)
        }
    }

    private fun updateLayoutManager() {
        try {
            val layoutManager = if (isGridView) {
                GridLayoutManager(this, 2)
            } else {
                LinearLayoutManager(this)
            }

            recyclerView.layoutManager = layoutManager
        } catch (e: Exception) {
            Log.e(TAG, "Error updating layout manager", e)
        }
    }

    // Navigation methods from original implementation
    private fun navigateToWeather() {
        try {
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening weather page", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToPlantingTips() {
        try {
            val intent = Intent(this, PlantingTipsActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening planting tips page", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToAbout() {
        try {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening about page", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openProductDetails(product: Product) {
        try {
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("product", product)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening product details", e)
            Toast.makeText(this, "Cannot open product details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFavorite(product: Product) {
        try {
            val newFavoriteStatus = !product.isFavorite

            // Update the product in all lists
            updateProductInLists(product.id) { it.copy(isFavorite = newFavoriteStatus) }

            // Update adapter
            productAdapter.updateProductFavoriteStatus(product.id, newFavoriteStatus)

            // Show feedback
            val message = if (newFavoriteStatus) {
                "Added to favorites"
            } else {
                "Removed from favorites"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            // Here you would typically save to database or preferences
            // saveFavoriteStatus(product.id, newFavoriteStatus)

        } catch (e: Exception) {
            Log.e(TAG, "Error toggling favorite", e)
            Toast.makeText(this, "Failed to update favorite", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToCart(product: Product) {
        try {
            if (!product.inStock || product.stockQuantity <= 0) {
                Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show()
                return
            }

            // Here you would typically add to cart database/preferences
            // CartManager.addToCart(product)

            Toast.makeText(this, "${product.name} added to cart", Toast.LENGTH_SHORT).show()

            // Update stock in all lists
            val newStock = (product.stockQuantity - 1).coerceAtLeast(0)
            updateProductInLists(product.id) { it.copy(stockQuantity = newStock) }

            // Update adapter
            productAdapter.updateProductStock(product.id, newStock)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding to cart", e)
            Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProductInLists(productId: Int, updateFunction: (Product) -> Product) {
        try {
            // Update in allProducts
            val allIndex = allProducts.indexOfFirst { it.id == productId }
            if (allIndex != -1) {
                allProducts[allIndex] = updateFunction(allProducts[allIndex])
            }

            // Update in currentProducts
            val currentIndex = currentProducts.indexOfFirst { it.id == productId }
            if (currentIndex != -1) {
                currentProducts[currentIndex] = updateFunction(currentProducts[currentIndex])
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product in lists", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Add product-specific menu items if needed
        if (apiHelper != null) {
            // Only inflate product menu if it exists
            try {
                menuInflater.inflate(R.menu.menu_product, menu)
            } catch (e: Exception) {
                Log.w(TAG, "Product menu not found, skipping")
            }
        }

        try {
            // Setup search if available
            menu.findItem(R.id.action_search)?.let { searchItem ->
                val searchView = searchItem.actionView as? SearchView

                searchView?.apply {
                    queryHint = "Search products..."

                    // Set initial query if we came from search
                    if (currentSearchQuery.isNotEmpty()) {
                        searchItem.expandActionView()
                        setQuery(currentSearchQuery, false)
                    }

                    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            query?.let { performSearch(it) }
                            return true
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            // Optional: implement real-time search
                            return true
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up search", e)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_about -> {
                navigateToAbout()
                true
            }
            R.id.action_weather -> {
                navigateToWeather()
                true
            }
            R.id.action_tips -> {
                navigateToPlantingTips()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_logout -> {
                handleLogout()
                true
            }
            R.id.action_sort -> {
                openSortDialog()
                true
            }
            R.id.action_view_toggle -> {
                toggleViewType()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun performSearch(query: String) {
        if (isLoading || apiHelper == null) {
            Log.d(TAG, "Search cancelled - already loading or no API")
            return
        }

        currentSearchQuery = query.trim()
        if (currentSearchQuery.isEmpty()) {
            return
        }

        supportActionBar?.title = "Search: $currentSearchQuery"
        showLoading(true)
        isLoading = true

        activityScope.launch {
            try {
                val searchResults = withContext(Dispatchers.IO) {
                    apiHelper!!.searchProducts(currentSearchQuery)
                }

                withContext(Dispatchers.Main) {
                    currentProducts.clear()
                    currentProducts.addAll(searchResults)

                    allProducts.clear()
                    allProducts.addAll(searchResults)

                    updateProductList()
                    showLoading(false)
                    isLoading = false

                    // Reset category selection
                    selectedCategoryId = -1
                    setupCategoryChips()

                    // Scroll to top after search
                    nestedScrollView.smoothScrollTo(0, 0)
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Search cancelled")
                isLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Error performing search", e)
                withContext(Dispatchers.Main) {
                    showError("Search failed: ${e.localizedMessage}")
                    showLoading(false)
                    isLoading = false
                }
            }
        }
    }

    private fun openFilterDialog() {
        // TODO: Implement filter dialog
        Toast.makeText(this, "Filter dialog coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun openSortDialog() {
        // TODO: Implement sort dialog
        Toast.makeText(this, "Sort dialog coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun handleLogout() {
        // Clear any saved login data
        // Navigate back to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            activityScope.cancel()
            apiHelper?.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
}