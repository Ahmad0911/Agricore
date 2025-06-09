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
            // Get product data from intent - matching the keys used in ProductActivity
            val productId = intent.getIntExtra("PRODUCT_ID", -1)
            val productName = intent.getStringExtra("PRODUCT_NAME") ?: ""
            val productPrice = intent.getDoubleExtra("PRODUCT_PRICE", 0.0)
            val productDescription = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: ""
            val productImage = intent.getIntExtra("PRODUCT_IMAGE", android.R.drawable.ic_menu_gallery)
            val productCategory = intent.getStringExtra("PRODUCT_CATEGORY") ?: "General" // Default category

            Log.d("ProductDetailActivity", "Product ID: $productId, Name: $productName, Price: $productPrice")

            // Create product from intent data
            if (productName.isNotEmpty() && productPrice > 0) {
                val product = Product(
                    id = productId,
                    name = productName,
                    description = productDescription.ifEmpty { "Fresh organic produce" },
                    price = productPrice,
                    category = productCategory, // Added category
                    imageRes = productImage.toString() // Convert to String as per your Product class
                )

                displayProductDetails(product)
                setupClickListeners(product)
            } else {
                Log.e("ProductDetailActivity", "Invalid product data: ID=$productId, Name=$productName, Price=$productPrice")
                Toast.makeText(this, "Invalid product data", Toast.LENGTH_SHORT).show()
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
            // Convert imageRes string to resource ID
            val imageResId = ApiHelper.getDrawableResourceId(this, product.imageRes)
            ivProductImage.setImageResource(imageResId)
            tvProductName.text = product.name
            tvProductPrice.text = product.getDisplayPrice() // Using the extension function
            tvProductDescription.text = product.description
            supportActionBar?.title = product.name

            Log.d("ProductDetailActivity", "Product details displayed successfully")
        } catch (e: Exception) {
            Log.e("ProductDetailActivity", "Error displaying product details", e)
            // Set fallback values
            tvProductName.text = product.name
            tvProductPrice.text = product.getDisplayPrice()
            tvProductDescription.text = product.description
            try {
                val imageResId = ApiHelper.getDrawableResourceId(this, product.imageRes)
                ivProductImage.setImageResource(imageResId)
            } catch (imgE: Exception) {
                Log.w("ProductDetailActivity", "Error setting image, using fallback", imgE)
                ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    private fun setupClickListeners(product: Product) {
        btnAddToCart.setOnClickListener {
            try {
                addToCart(product)

                // Animate button like in ProductAdapter
                btnAddToCart.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(150)
                    .withEndAction {
                        btnAddToCart.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start()
                    }
                    .start()

                Toast.makeText(this, "${product.name} added to cart! 🛒", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ProductDetailActivity", "Error adding to cart", e)
                Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addToCart(product: Product) {
        // TODO: Implement your cart logic here
        // This could involve:
        // 1. Adding to local database
        // 2. Sending to server
        // 3. Updating shared preferences
        // 4. Broadcasting to other components
        // 5. Integrating with CartManager.addProduct(product) when implemented

        Log.d("ProductDetailActivity", "Added to cart: ${product.name} - ${product.getDisplayPrice()}")

        // For now, we'll just log
        // You can replace this with actual cart implementation
        // CartManager.addProduct(product)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources if needed
        Log.d("ProductDetailActivity", "ProductDetailActivity destroyed")
    }
}