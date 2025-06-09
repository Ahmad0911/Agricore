package com.example.agricore

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProductActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var searchView: SearchView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var searchInfoCard: CardView
    private lateinit var searchInfoTextView: TextView
    private lateinit var searchCard: CardView
    private lateinit var searchHint: TextView
    private lateinit var filterIcon: View
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var btnClearSearch: TextView

    // ApiHelper instance
    private lateinit var apiHelper: ApiHelper

    private var isGridView = true
    private val spanCount = 2

    // API data holders
    private var allProducts: List<Product> = emptyList()
    private var categories: List<Category> = emptyList()
    private var filterOptions: FilterOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        try {
            // Initialize ApiHelper
            apiHelper = ApiHelper(this)

            initializeViews()
            setupToolbar()
            setupSearchCard()
            setupRecyclerView()
            setupCategoryPills()
            setupFloatingActionButton()

            // Load data from ApiHelper
            loadDataFromApi()

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error in onCreate", e)
        }
    }

    private fun initializeViews() {
        try {
            // Main views
            toolbar = findViewById(R.id.toolbar)
            productsRecyclerView = findViewById(R.id.productsRecyclerView)
            emptyStateLayout = findViewById(R.id.emptyStateLayout)

            // Search related views
            searchInfoCard = findViewById(R.id.searchInfoCard)
            searchInfoTextView = findViewById(R.id.tvSearchInfo)
            searchCard = findViewById(R.id.searchCard)
            searchHint = findViewById(R.id.searchHint)
            filterIcon = findViewById(R.id.filterIcon)

            // FAB and other controls
            fabAddProduct = findViewById(R.id.fabAddProduct)
            btnClearSearch = findViewById(R.id.btnClearSearch)

            Log.d("ProductActivity", "All views found successfully")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error finding views", e)
            throw e
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                title = "" // Title is already set in XML
            }

            Log.d("ProductActivity", "Toolbar setup completed")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up toolbar", e)
            throw e
        }
    }

    private fun setupSearchCard() {
        try {
            // Make search card clickable to open search
            searchCard.setOnClickListener {
                openSearch()
            }

            // Setup filter icon click
            filterIcon.setOnClickListener {
                showFilterDialog()
            }

            // Setup clear search button
            btnClearSearch.setOnClickListener {
                clearSearch()
            }

            Log.d("ProductActivity", "Search card setup completed")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up search card", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            productAdapter = ProductAdapter(emptyList()) { product ->
                openProductDetail(product)
            }

            productsRecyclerView.apply {
                layoutManager = GridLayoutManager(this@ProductActivity, spanCount).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int = 1
                    }
                }
                adapter = productAdapter
                setHasFixedSize(true)
                // Remove nested scrolling since we're using NestedScrollView
                isNestedScrollingEnabled = false
            }

            updateEmptyState()

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up RecyclerView", e)
            throw e
        }
    }

    private fun setupCategoryPills() {
        try {
            // Category pills are already defined in XML
            // Here you would add click listeners to each category pill
            // For now, they're just visual elements

            // You can add category filtering logic here
            Log.d("ProductActivity", "Category pills setup completed")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up category pills", e)
        }
    }

    private fun setupFloatingActionButton() {
        try {
            // For now, hide the FAB as mentioned in XML
            fabAddProduct.visibility = View.GONE

            // If you want to use it later:
            fabAddProduct.setOnClickListener {
                // Add new product functionality
                openAddProductActivity()
            }

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up FAB", e)
        }
    }

    private fun loadDataFromApi() {
        try {
            // Load products from ApiHelper
            allProducts = apiHelper.getProducts()
            Log.d("ProductActivity", "Loaded ${allProducts.size} products from ApiHelper")

            // Load categories from ApiHelper
            categories = apiHelper.getCategories()
            Log.d("ProductActivity", "Loaded ${categories.size} categories from ApiHelper")

            // Load filter options from ApiHelper
            filterOptions = apiHelper.getFilterOptions()
            filterOptions?.let {
                Log.d("ProductActivity", "Loaded filter options from ApiHelper")
            } ?: Log.d("ProductActivity", "No filter options available")

            // Update adapter with new products
            productAdapter.updateProducts(allProducts)
            updateEmptyState()

            Log.d("ProductActivity", "Successfully loaded all data from ApiHelper")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error loading data from ApiHelper", e)
            // If ApiHelper fails, we still have fallback data from ApiHelper itself
            // No need for additional fallback here since ApiHelper handles it
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        try {
            menuInflater.inflate(R.menu.menu_product, menu)

            val searchItem = menu?.findItem(R.id.action_search)
            searchView = searchItem?.actionView as? SearchView ?: return false

            setupSearchView()

            Log.d("ProductActivity", "Options menu created successfully")
            return true

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error creating options menu", e)
            return false
        }
    }

    private fun setupSearchView() {
        try {
            searchView.apply {
                queryHint = "Search fresh products..."
                maxWidth = Integer.MAX_VALUE

                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        searchView.clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        val query = newText?.trim() ?: ""
                        filterProducts(query)
                        return true
                    }
                })

                setOnCloseListener {
                    updateSearchInfo("")
                    false
                }
            }

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up SearchView", e)
        }
    }

    private fun openSearch() {
        // Programmatically open the search view from toolbar
        searchView.isIconified = false
        searchView.requestFocus()
    }

    private fun clearSearch() {
        searchView.setQuery("", false)
        searchView.clearFocus()
        searchView.isIconified = true
        updateSearchInfo("")
        updateEmptyState()
    }

    private fun filterProducts(query: String) {
        try {
            productAdapter.filter(query)
            updateSearchInfo(query)
            updateEmptyState()
            productsRecyclerView.scrollToPosition(0)

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error filtering products", e)
        }
    }

    private fun updateSearchInfo(query: String) {
        try {
            val filteredCount = productAdapter.getFilteredCount()

            if (query.isNotEmpty()) {
                searchInfoTextView.text = "Found $filteredCount products for '$query'"
                searchInfoCard.visibility = View.VISIBLE
            } else {
                searchInfoCard.visibility = View.GONE
            }

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error updating search info", e)
        }
    }

    private fun updateEmptyState() {
        try {
            val isEmpty = productAdapter.getFilteredCount() == 0

            if (isEmpty) {
                emptyStateLayout.visibility = View.VISIBLE
                productsRecyclerView.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                productsRecyclerView.visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error updating empty state", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> true
            R.id.action_refresh -> {
                refreshProducts()
                true
            }
            R.id.action_view_grid -> {
                switchToGridView()
                true
            }
            R.id.action_view_list -> {
                switchToListView()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun switchToGridView() {
        if (!isGridView) {
            isGridView = true
            productsRecyclerView.layoutManager = GridLayoutManager(this, spanCount)
        }
    }

    private fun switchToListView() {
        if (isGridView) {
            isGridView = false
            productsRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun refreshProducts() {
        try {
            // Reload data from ApiHelper
            loadDataFromApi()

            Log.d("ProductActivity", "Products refreshed successfully")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error refreshing products", e)
        }
    }

    private fun showFilterDialog() {
        try {
            // Use the loaded categories and filterOptions for filtering
            filterOptions?.let { options ->
                // TODO: Implement filter dialog using the loaded filterOptions
                // You now have access to:
                // - categories: List<Category>
                // - options.priceRanges: List<PriceRange> (if available)
                // - options.badges: List<String> (if available)
                // - options.ratings: List<Double> (if available)

                Log.d("ProductActivity", "Filter dialog with ${categories.size} categories")
            } ?: run {
                Log.w("ProductActivity", "No filter options available")
            }
        } catch (e: Exception) {
            Log.e("ProductActivity", "Error showing filter dialog", e)
        }
    }

    private fun openAddProductActivity() {
        try {
            // TODO: Implement add product activity
            Log.d("ProductActivity", "Add product functionality not yet implemented")
        } catch (e: Exception) {
            Log.e("ProductActivity", "Error opening add product activity", e)
        }
    }

    private fun openProductDetail(product: Product) {
        try {
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT", product) // Pass the entire Product object
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ProductActivity", "Error opening product detail", e)
        }
    }

    override fun onBackPressed() {
        if (::searchView.isInitialized && !searchView.isIconified) {
            searchView.isIconified = true
        } else {
            super.onBackPressed()
        }
    }
}