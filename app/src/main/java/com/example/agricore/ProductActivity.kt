package com.example.agricore

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var searchView: SearchView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var searchInfoTextView: TextView

    private var isGridView = true // Default to grid view
    private val spanCount = 2 // Number of columns in grid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ProductActivity", "onCreate started")

        try {
            setContentView(R.layout.activity_product)
            Log.d("ProductActivity", "Layout set successfully")

            // Initialize views
            initializeViews()

            // Setup custom toolbar
            setupToolbar()

            // Initialize RecyclerView with grid layout
            setupRecyclerView()

            Log.d("ProductActivity", "ProductActivity setup completed successfully")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error loading products: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            // Find all views
            toolbar = findViewById(R.id.toolbar)
            productsRecyclerView = findViewById(R.id.productsRecyclerView)
            emptyStateLayout = findViewById(R.id.emptyStateLayout)
            searchInfoTextView = findViewById(R.id.tvSearchInfo)

            Log.d("ProductActivity", "All views found successfully")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error finding views", e)
            throw e
        }
    }

    private fun setupToolbar() {
        try {
            // Set toolbar as action bar
            setSupportActionBar(toolbar)

            // Configure action bar
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                title = "Fresh Products"
            }

            Log.d("ProductActivity", "Toolbar setup completed")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up toolbar", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            val products = createSampleProducts()
            Log.d("ProductActivity", "Created ${products.size} sample products")

            // Create adapter
            productAdapter = ProductAdapter(products) { product ->
                openProductDetail(product)
            }
            Log.d("ProductActivity", "ProductAdapter created")

            // Setup RecyclerView with grid layout
            productsRecyclerView.apply {
                layoutManager = GridLayoutManager(this@ProductActivity, spanCount).apply {
                    // Optional: Handle span sizes for different item types
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return 1 // Each item takes 1 span
                        }
                    }
                }
                adapter = productAdapter

                // Add item decoration for better spacing
                addItemDecoration(GridSpacingItemDecoration(spanCount, 16, true))

                // Improve scrolling performance
                setHasFixedSize(true)

                Log.d("ProductActivity", "RecyclerView setup completed with grid layout")
            }

            // Update empty state
            updateEmptyState()

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up RecyclerView", e)
            throw e
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        try {
            menuInflater.inflate(R.menu.menu_product, menu)

            val searchItem = menu?.findItem(R.id.action_search)
            searchView = searchItem?.actionView as SearchView

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
                // Configure search view appearance
                queryHint = "Search products..."
                maxWidth = Integer.MAX_VALUE

                // Handle search query changes
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

                // Handle search view expand/collapse
                setOnSearchClickListener {
                    Log.d("ProductActivity", "Search expanded")
                }

                setOnCloseListener {
                    Log.d("ProductActivity", "Search closed")
                    updateSearchInfo("")
                    false
                }
            }

            Log.d("ProductActivity", "SearchView setup completed")

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error setting up SearchView", e)
        }
    }

    private fun filterProducts(query: String) {
        try {
            productAdapter.filter(query)
            updateSearchInfo(query)
            updateEmptyState()

            // Scroll to top after filtering
            productsRecyclerView.scrollToPosition(0)

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error filtering products", e)
        }
    }

    private fun updateSearchInfo(query: String) {
        try {
            val filteredCount = productAdapter.getFilteredCount()
            val totalCount = productAdapter.itemCount

            if (query.isNotEmpty()) {
                searchInfoTextView.text = "Found $filteredCount products for '$query'"
                searchInfoTextView.visibility = View.VISIBLE
            } else {
                searchInfoTextView.text = "Showing all products"
                searchInfoTextView.visibility = View.GONE
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
            R.id.action_search -> {
                // Handled by SearchView
                true
            }
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
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun switchToGridView() {
        if (!isGridView) {
            isGridView = true
            productsRecyclerView.layoutManager = GridLayoutManager(this, spanCount)
            Toast.makeText(this, "Switched to grid view", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchToListView() {
        if (isGridView) {
            isGridView = false
            productsRecyclerView.layoutManager = LinearLayoutManager(this)
            Toast.makeText(this, "Switched to list view", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshProducts() {
        try {
            // Simulate refresh with new data
            val newProducts = createSampleProducts()
            productAdapter.updateProducts(newProducts)
            updateEmptyState()
            Toast.makeText(this, "Products refreshed", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("ProductActivity", "Error refreshing products", e)
            Toast.makeText(this, "Error refreshing products", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFilterDialog() {
        // TODO: Implement filter dialog
        Toast.makeText(this, "Filter options coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun createSampleProducts(): List<Product> {
        return try {
            listOf(
                Product(1, "Organic Tomatoes", "Freshly picked organic tomatoes from local farms", 2.99, android.R.drawable.ic_menu_gallery),
                Product(2, "Sweet Carrots", "Sweet farm-fresh carrots", 1.49, android.R.drawable.ic_menu_gallery),
                Product(3, "Red Apples", "Juicy red apples, perfect for snacking", 0.99, android.R.drawable.ic_menu_gallery),
                Product(4, "Russet Potatoes", "Organic russet potatoes, great for cooking", 1.29, android.R.drawable.ic_menu_gallery),
                Product(5, "Baby Spinach", "Fresh baby spinach leaves", 2.49, android.R.drawable.ic_menu_gallery),
                Product(6, "Organic Lettuce", "Crisp fresh lettuce leaves", 1.99, android.R.drawable.ic_menu_gallery),
                Product(7, "Bell Peppers", "Colorful bell peppers mix", 3.49, android.R.drawable.ic_menu_gallery),
                Product(8, "Green Cucumbers", "Fresh green cucumbers", 1.79, android.R.drawable.ic_menu_gallery),
                Product(9, "Broccoli", "Fresh green broccoli crowns", 2.29, android.R.drawable.ic_menu_gallery),
                Product(10, "Sweet Corn", "Fresh sweet corn on the cob", 0.79, android.R.drawable.ic_menu_gallery),
                Product(11, "Red Onions", "Fresh red onions", 1.19, android.R.drawable.ic_menu_gallery),
                Product(12, "Green Beans", "Tender green beans", 2.99, android.R.drawable.ic_menu_gallery)
            )
        } catch (e: Exception) {
            Log.e("ProductActivity", "Error creating sample products", e)
            emptyList()
        }
    }

    private fun openProductDetail(product: Product) {
        try {
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_ID", product.id)
                putExtra("PRODUCT_NAME", product.name)
                putExtra("PRODUCT_DESCRIPTION", product.description)
                putExtra("PRODUCT_PRICE", product.price)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ProductActivity", "Error opening product detail", e)
            Toast.makeText(this, "Product detail not available yet", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (::searchView.isInitialized && !searchView.isIconified) {
            searchView.isIconified = true
        } else {
            super.onBackPressed()
        }
    }
}