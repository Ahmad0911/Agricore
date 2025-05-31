package com.example.agricore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ProductAdapter(
    private val originalProducts: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Filtered list that will be displayed
    private var filteredProducts: MutableList<Product> = originalProducts.toMutableList()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val productName: TextView = itemView.findViewById(R.id.tvProductName)
        val productDescription: TextView = itemView.findViewById(R.id.tvProductDescription)
        val productPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val addToCartButton: TextView = itemView.findViewById(R.id.btnAddToCart)

        fun bind(product: Product) {
            try {
                productName.text = product.name
                productDescription.text = product.description
                productPrice.text = "$${String.format("%.2f", product.price)}"

                // Set product image (using default for now)
                productImage.setImageResource(android.R.drawable.ic_menu_gallery)

                // Add entrance animation
                itemView.alpha = 0f
                itemView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay((adapterPosition * 50).toLong())
                    .start()

                // Handle item click with modern animation
                itemView.setOnClickListener {
                    animateClick(itemView) {
                        onProductClick(product)
                    }
                }

                // Handle add to cart button with modern animation
                addToCartButton.setOnClickListener {
                    animateClick(it) {
                        addToCart(product)
                    }
                }

            } catch (e: Exception) {
                // Handle binding errors gracefully
                productName.text = "Product"
                productDescription.text = "Description not available"
                productPrice.text = "$0.00"
            }
        }

        private fun animateClick(view: View, action: () -> Unit) {
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            action()
                        }
                        .start()
                }
                .start()
        }

        private fun addToCart(product: Product) {
            try {
                val context = itemView.context

                // Show success feedback with custom toast
                Toast.makeText(
                    context,
                    "${product.name} added to cart! 🛒",
                    Toast.LENGTH_SHORT
                ).show()

                // Add ripple effect to the button
                addToCartButton.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(150)
                    .withEndAction {
                        addToCartButton.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start()
                    }
                    .start()

                // Here you would typically add the product to an actual cart
                // CartManager.addProduct(product)

            } catch (e: Exception) {
                Toast.makeText(itemView.context, "Error adding to cart", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return try {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product, parent, false)
            ProductViewHolder(itemView)
        } catch (e: Exception) {
            // Fallback in case of layout inflation error
            throw RuntimeException("Error creating ViewHolder: ${e.message}", e)
        }
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        try {
            if (position < filteredProducts.size) {
                holder.bind(filteredProducts[position])
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = filteredProducts.size

    // Enhanced search functionality with better performance
    fun filter(query: String) {
        try {
            val oldSize = filteredProducts.size
            filteredProducts.clear()

            if (query.isEmpty()) {
                // If search is empty, show all products
                filteredProducts.addAll(originalProducts)
            } else {
                // Filter products based on name, description, and price
                val searchQuery = query.lowercase(Locale.getDefault()).trim()

                filteredProducts.addAll(
                    originalProducts.filter { product ->
                        product.name.lowercase(Locale.getDefault()).contains(searchQuery) ||
                                product.description.lowercase(Locale.getDefault()).contains(searchQuery) ||
                                String.format("%.2f", product.price).contains(searchQuery)
                    }
                )
            }

            // Use more efficient notification methods
            if (oldSize == filteredProducts.size) {
                notifyItemRangeChanged(0, filteredProducts.size)
            } else {
                notifyDataSetChanged()
            }

        } catch (e: Exception) {
            // Handle filter errors gracefully
            filteredProducts.clear()
            filteredProducts.addAll(originalProducts)
            notifyDataSetChanged()
        }
    }

    // Method to update the product list with smooth animations
    fun updateProducts(newProducts: List<Product>) {
        try {
            val oldSize = filteredProducts.size
            filteredProducts.clear()
            filteredProducts.addAll(newProducts)

            // Animate the changes
            if (newProducts.size > oldSize) {
                notifyItemRangeInserted(oldSize, newProducts.size - oldSize)
            } else if (newProducts.size < oldSize) {
                notifyItemRangeRemoved(newProducts.size, oldSize - newProducts.size)
            } else {
                notifyItemRangeChanged(0, newProducts.size)
            }

        } catch (e: Exception) {
            // Fallback to simple refresh
            notifyDataSetChanged()
        }
    }

    // Get current filtered products count for empty state handling
    fun getFilteredCount(): Int = filteredProducts.size

    // Method to get filtered products (useful for other operations)
    fun getFilteredProducts(): List<Product> = filteredProducts.toList()

    // Method to refresh all items with animation
    fun refreshWithAnimation() {
        for (i in filteredProducts.indices) {
            notifyItemChanged(i)
        }
    }
}