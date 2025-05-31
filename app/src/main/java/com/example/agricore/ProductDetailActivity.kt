package com.example.agricore

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var ivProductImage: ImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvProductDescription: TextView
    private lateinit var btnAddToCart: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_product_detail)
            initializeViews()
            setupToolbar()
            loadProductData()
        } catch (e: Exception) {
            Log.e("ProductDetailActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error loading product details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        ivProductImage = findViewById(R.id.ivProductImage)
        tvProductName = findViewById(R.id.tvProductName)
        tvProductPrice = findViewById(R.id.tvProductPrice)
        tvProductDescription = findViewById(R.id.tvProductDescription)
        btnAddToCart = findViewById(R.id.btnAddToCart)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Product Details"
        }
    }

    private fun loadProductData() {
        try {
            // Get product data from intent - handle multiple possible keys
            val productId = intent.getIntExtra("PRODUCT_ID",
                intent.getIntExtra("product_id", -1))

            val productName = intent.getStringExtra("PRODUCT_NAME")
                ?: intent.getStringExtra("product_name")
                ?: ""

            val productPrice = intent.getDoubleExtra("PRODUCT_PRICE",
                intent.getDoubleExtra("product_price", 0.0))

            val productDescription = intent.getStringExtra("PRODUCT_DESCRIPTION")
                ?: intent.getStringExtra("product_description")
                ?: ""

            Log.d("ProductDetailActivity", "Product ID: $productId, Name: $productName")

            // Try to get product from sample data first
            var product = getProductDetails(productId)

            // If not found in sample data, create from intent data
            if (product == null && productName.isNotEmpty()) {
                product = Product(
                    id = productId,
                    name = productName,
                    description = productDescription.ifEmpty { "Fresh organic produce" },
                    price = if (productPrice > 0) productPrice else 0.99,
                    imageRes = getDefaultImageResource(productName)
                )
            }

            // Display product details
            product?.let {
                displayProductDetails(it)
                setupClickListeners(it)
            } ?: run {
                Log.e("ProductDetailActivity", "Product not found: ID=$productId, Name=$productName")
                Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            Log.e("ProductDetailActivity", "Error loading product data", e)
            Toast.makeText(this, "Error loading product", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun displayProductDetails(product: Product) {
        try {
            ivProductImage.setImageResource(product.imageRes)
            tvProductName.text = product.name
            tvProductPrice.text = "$${"%.2f".format(product.price)}"
            tvProductDescription.text = product.description
            supportActionBar?.title = product.name
        } catch (e: Exception) {
            Log.e("ProductDetailActivity", "Error displaying product details", e)
            // Set fallback values
            tvProductName.text = product.name
            tvProductPrice.text = "$${product.price}"
            tvProductDescription.text = product.description
            ivProductImage.setImageResource(R.drawable.ic_launcher_foreground) // fallback image
        }
    }

    private fun setupClickListeners(product: Product) {
        btnAddToCart.setOnClickListener {
            try {
                addToCart(product)
                Toast.makeText(this, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ProductDetailActivity", "Error adding to cart", e)
                Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getProductDetails(productId: Int): Product? {
        // Sample data - in a real app, this would come from a database or API
        return when (productId) {
            1 -> Product(1, "Organic Tomatoes", "Freshly picked organic tomatoes from local farms. Rich in vitamins and perfect for cooking.", 2.99, getImageResource("tomatoes"))
            2 -> Product(2, "Fresh Carrots", "Sweet farm carrots, crunchy and nutritious. Great for snacking or cooking.", 1.49, getImageResource("carrots"))
            3 -> Product(3, "Red Apples", "Juicy red apples, crisp and sweet. Perfect for healthy snacking.", 0.99, getImageResource("apples"))
            4 -> Product(4, "Green Lettuce", "Fresh green lettuce leaves, perfect for salads and sandwiches.", 1.99, getImageResource("lettuce"))
            5 -> Product(5, "Yellow Bananas", "Ripe yellow bananas, sweet and creamy. Rich in potassium.", 1.29, getImageResource("bananas"))
            6 -> Product(6, "Fresh Spinach", "Organic spinach leaves, packed with nutrients and flavor.", 2.49, getImageResource("spinach"))
            else -> null
        }
    }

    private fun getImageResource(imageName: String): Int {
        return try {
            val resourceId = resources.getIdentifier("ic_$imageName", "drawable", packageName)
            if (resourceId != 0) resourceId else getDefaultImageResource(imageName)
        } catch (e: Exception) {
            Log.w("ProductDetailActivity", "Image resource not found: $imageName", e)
            getDefaultImageResource(imageName)
        }
    }

    private fun getDefaultImageResource(productName: String): Int {
        return when {
            productName.contains("tomato", ignoreCase = true) -> android.R.drawable.ic_menu_gallery
            productName.contains("carrot", ignoreCase = true) -> android.R.drawable.ic_menu_gallery
            productName.contains("apple", ignoreCase = true) -> android.R.drawable.ic_menu_gallery
            productName.contains("lettuce", ignoreCase = true) -> android.R.drawable.ic_menu_gallery
            productName.contains("banana", ignoreCase = true) -> android.R.drawable.ic_menu_gallery
            productName.contains("spinach", ignoreCase = true) -> android.R.drawable.ic_menu_gallery
            else -> android.R.drawable.ic_menu_gallery
        }
    }

    private fun addToCart(product: Product) {
        // TODO: Implement your cart logic here
        // This could involve:
        // 1. Adding to local database
        // 2. Sending to server
        // 3. Updating shared preferences
        // 4. Broadcasting to other components

        Log.d("ProductDetailActivity", "Added to cart: ${product.name}")

        // For now, we'll just log and show toast
        // You can replace this with actual cart implementation
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources if needed
    }
}