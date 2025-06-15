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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var fabToggleView: FloatingActionButton
    private lateinit var emptyStateView: View
    private lateinit var loadingView: View

    // Adapter and data
    private lateinit var productAdapter: ProductAdapter
    private lateinit var apiHelper: ApiHelper

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
            setupRecyclerView()
            setupFab()

            apiHelper = ApiHelper(this)

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
            recyclerView = findViewById(R.id.recycler_view_products)
            chipGroup = findViewById(R.id.chip_group_categories)
            fabToggleView = findViewById(R.id.fab_toggle_view)
            emptyStateView = findViewById(R.id.layout_empty_state)
            loadingView = findViewById(R.id.layout_loading)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Products"
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
                // Add item animator to prevent flicker during updates
                itemAnimator = null
                // Set fixed size for better performance
                setHasFixedSize(true)
            }

            updateLayoutManager()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
            throw e
        }
    }

    private fun setupFab() {
        fabToggleView.setOnClickListener {
            toggleViewType()
        }
        updateFabIcon()
    }

    private fun handleIntentExtras() {
        selectedCategoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        currentSearchQuery = intent.getStringExtra(EXTRA_SEARCH_QUERY) ?: ""

        // Update title based on context
        when {
            currentSearchQuery.isNotEmpty() -> {
                supportActionBar?.title = "Search: $currentSearchQuery"
            }
            selectedCategoryId != -1 -> {
                supportActionBar?.title = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Products"
            }
            else -> {
                supportActionBar?.title = "All Products"
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

        activityScope.launch {
            try {
                // Load data in background
                val productsDeferred = async(Dispatchers.IO) {
                    when {
                        currentSearchQuery.isNotEmpty() -> apiHelper.searchProducts(currentSearchQuery)
                        selectedCategoryId != -1 -> apiHelper.getProductsByCategory(selectedCategoryId)
                        else -> apiHelper.getProducts()
                    }
                }

                val categoriesDeferred = async(Dispatchers.IO) { apiHelper.getCategories() }

                // Wait for both to complete
                val loadedProducts = productsDeferred.await()
                val loadedCategories = categoriesDeferred.await()

                // Update data safely on main thread
                withContext(Dispatchers.Main) {
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
                }

                Log.d(TAG, "Loaded ${allProducts.size} products and ${categories.size} categories")

            } catch (e: CancellationException) {
                Log.d(TAG, "Loading cancelled")
                isLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Error loading products data", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to load products: ${e.localizedMessage}")
                    showLoading(false)
                    isLoading = false
                }
            }
        }
    }

    private fun setupCategoryChips() {
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
            // Create a copy of the list to avoid concurrent modification
            val productsCopy = currentProducts.toList()

            if (productsCopy.isEmpty() && !isLoading) {
                showEmptyState(true)
                productAdapter.submitList(emptyList())
            } else {
                showEmptyState(false)
                productAdapter.submitList(productsCopy)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product list", e)
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
                chipGroup.visibility = View.VISIBLE
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
            updateFabIcon()
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

    private fun updateFabIcon() {
        try {
            val iconRes = if (isGridView) {
                android.R.drawable.ic_menu_sort_by_size // List view icon
            } else {
                android.R.drawable.ic_menu_view // Grid view icon
            }
            fabToggleView.setImageResource(iconRes)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FAB icon", e)
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
        menuInflater.inflate(R.menu.menu_product, menu)

        try {
            // Setup search
            val searchItem = menu.findItem(R.id.action_search)
            val searchView = searchItem?.actionView as? SearchView

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
            R.id.action_filter -> {
                openFilterDialog()
                true
            }
            R.id.action_sort -> {
                openSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun performSearch(query: String) {
        if (isLoading) {
            Log.d(TAG, "Search cancelled - already loading")
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
                    apiHelper.searchProducts(currentSearchQuery)
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            activityScope.cancel()
            if (::apiHelper.isInitialized) {
                apiHelper.cleanup()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
}