package com.example.agricore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ProductAdapter(
    private val originalProducts: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var filteredProducts: MutableList<Product> = originalProducts.toMutableList()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views from item_product.xml
        val productImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val productName: TextView = itemView.findViewById(R.id.tvProductName)
        val productDescription: TextView = itemView.findViewById(R.id.tvProductDescription)
        val productPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val priceUnit: TextView = itemView.findViewById(R.id.tvPriceUnit)
        val rating: TextView = itemView.findViewById(R.id.tvRating)
        val badge: TextView = itemView.findViewById(R.id.tvBadge)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.ivFavorite)
        val addToCartButton: TextView = itemView.findViewById(R.id.btnAddToCart)

        fun bind(product: Product) {
            try {
                // Set basic product info
                productName.text = product.name
                productDescription.text = product.description
                productPrice.text = "$${String.format("%.2f", product.price)}"
                priceUnit.text = product.priceUnit

                // Set product image - FIXED: Use getDrawableResourceId to convert string to resource ID
                val imageResId = ApiHelper.getDrawableResourceId(itemView.context, product.imageRes)
                productImage.setImageResource(imageResId)

                // Set rating from actual product data
                rating.text = product.getDisplayRating()

                // Set badge from product data
                badge.text = product.badge.ifEmpty {
                    when {
                        product.name.contains("Organic", ignoreCase = true) -> "Organic"
                        product.name.contains("Fresh", ignoreCase = true) -> "Fresh"
                        product.price > 2.0 -> "Premium"
                        else -> "Fresh"
                    }
                }

                // Set favorite state from product data
                favoriteIcon.setImageResource(
                    if (product.isFavorite) android.R.drawable.btn_star_big_on
                    else android.R.drawable.btn_star_big_off
                )

                var isFavorite = product.isFavorite
                favoriteIcon.setOnClickListener {
                    isFavorite = !isFavorite
                    favoriteIcon.setImageResource(
                        if (isFavorite) android.R.drawable.btn_star_big_on
                        else android.R.drawable.btn_star_big_off
                    )

                    // Add animation
                    favoriteIcon.animate()
                        .scaleX(1.3f)
                        .scaleY(1.3f)
                        .setDuration(150)
                        .withEndAction {
                            favoriteIcon.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start()
                        }
                        .start()

                    val message = if (isFavorite) "Added to favorites ❤️" else "Removed from favorites"
                    Toast.makeText(itemView.context, message, Toast.LENGTH_SHORT).show()
                }

                // Add entrance animation
                itemView.alpha = 0f
                itemView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay((adapterPosition * 50).toLong())
                    .start()

                // Handle item click
                itemView.setOnClickListener {
                    animateClick(itemView) {
                        onProductClick(product)
                    }
                }

                // Handle add to cart button
                addToCartButton.setOnClickListener {
                    animateClick(it) {
                        addToCart(product)
                    }
                }

            } catch (e: Exception) {
                // Handle binding errors gracefully
                productName.text = "Product"
                productDescription.text = "Fresh organic produce"
                productPrice.text = "$0.00"
                priceUnit.text = "per kg"
                rating.text = "4.0"
                badge.text = "Fresh"
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

                // Show success feedback
                Toast.makeText(
                    context,
                    "${product.name} added to cart! 🛒",
                    Toast.LENGTH_SHORT
                ).show()

                // Animate the add to cart button
                addToCartButton.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(150)
                    .withEndAction {
                        addToCartButton.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start()
                    }
                    .start()

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
            throw RuntimeException("Error creating ViewHolder: ${e.message}", e)
        }
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        try {
            if (position < filteredProducts.size) {
                holder.bind(filteredProducts[position])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = filteredProducts.size

    fun filter(query: String) {
        try {
            val oldSize = filteredProducts.size
            filteredProducts.clear()

            if (query.isEmpty()) {
                filteredProducts.addAll(originalProducts)
            } else {
                val searchQuery = query.lowercase(Locale.getDefault()).trim()

                filteredProducts.addAll(
                    originalProducts.filter { product ->
                        product.name.lowercase(Locale.getDefault()).contains(searchQuery) ||
                                product.description.lowercase(Locale.getDefault()).contains(searchQuery) ||
                                product.category.lowercase(Locale.getDefault()).contains(searchQuery) ||
                                String.format("%.2f", product.price).contains(searchQuery)
                    }
                )
            }

            // Efficient notification
            if (oldSize == filteredProducts.size) {
                notifyItemRangeChanged(0, filteredProducts.size)
            } else {
                notifyDataSetChanged()
            }

        } catch (e: Exception) {
            filteredProducts.clear()
            filteredProducts.addAll(originalProducts)
            notifyDataSetChanged()
        }
    }

    fun updateProducts(newProducts: List<Product>) {
        try {
            val oldSize = filteredProducts.size
            filteredProducts.clear()
            filteredProducts.addAll(newProducts)

            if (newProducts.size > oldSize) {
                notifyItemRangeInserted(oldSize, newProducts.size - oldSize)
            } else if (newProducts.size < oldSize) {
                notifyItemRangeRemoved(newProducts.size, oldSize - newProducts.size)
            } else {
                notifyItemRangeChanged(0, newProducts.size)
            }

        } catch (e: Exception) {
            notifyDataSetChanged()
        }
    }

    fun getFilteredCount(): Int = filteredProducts.size

    fun getFilteredProducts(): List<Product> = filteredProducts.toList()

    fun refreshWithAnimation() {
        for (i in filteredProducts.indices) {
            notifyItemChanged(i)
        }
    }
}