package com.example.agricore

import android.content.Context
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class ProductAdapter(
    private val context: Context,
    private val onProductClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit = {},
    private val onAddToCartClick: (Product) -> Unit = {}
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_PRODUCT = 0
        private const val VIEW_TYPE_LOADING = 1
        private const val VIEW_TYPE_EMPTY = 2
        private const val TAG = "ProductAdapter"
    }

    private var isLoading = false
    private var showEmptyState = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOADING -> LoadingViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
            )
            VIEW_TYPE_EMPTY -> EmptyViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_empty, parent, false)
            )
            else -> ProductItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        // Add bounds checking
        if (position < 0) {
            Log.w(TAG, "Invalid position: $position")
            return
        }

        when (holder) {
            is ProductItemViewHolder -> {
                // Only bind if position is valid for products
                if (position < currentList.size) {
                    holder.bind(currentList[position])
                } else {
                    Log.w(TAG, "Position $position out of bounds for product list size ${currentList.size}")
                }
            }
            is LoadingViewHolder -> holder.bind()
            is EmptyViewHolder -> holder.bind()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            // Show loading only when list is empty and loading
            isLoading && currentList.isEmpty() && position == 0 -> VIEW_TYPE_LOADING
            // Show empty state only when list is empty and not loading
            showEmptyState && currentList.isEmpty() && !isLoading && position == 0 -> VIEW_TYPE_EMPTY
            // All other cases are products
            position < currentList.size -> VIEW_TYPE_PRODUCT
            else -> {
                Log.w(TAG, "Invalid position $position for getItemViewType, currentList size: ${currentList.size}")
                VIEW_TYPE_PRODUCT // Fallback
            }
        }
    }

    override fun getItemCount(): Int {
        return when {
            // Show 1 item for loading state when list is empty
            isLoading && currentList.isEmpty() -> 1
            // Show 1 item for empty state when list is empty and not loading
            showEmptyState && currentList.isEmpty() && !isLoading -> 1
            // Otherwise show actual product count
            else -> currentList.size
        }
    }

    fun setLoading(loading: Boolean) {
        val wasShowingSpecialState = (isLoading && currentList.isEmpty()) || (showEmptyState && currentList.isEmpty())
        val willShowSpecialState = (loading && currentList.isEmpty()) || (showEmptyState && currentList.isEmpty())

        isLoading = loading
        if (loading) {
            showEmptyState = false
        }

        // Only notify if the special state visibility changed
        if (wasShowingSpecialState != willShowSpecialState) {
            if (currentList.isEmpty()) {
                if (wasShowingSpecialState && !willShowSpecialState) {
                    notifyItemRemoved(0)
                } else if (!wasShowingSpecialState && willShowSpecialState) {
                    notifyItemInserted(0)
                } else {
                    notifyItemChanged(0)
                }
            }
        } else if (currentList.isEmpty() && (wasShowingSpecialState || willShowSpecialState)) {
            notifyItemChanged(0)
        }
    }

    fun setEmptyState(empty: Boolean) {
        val wasShowingSpecialState = (isLoading && currentList.isEmpty()) || (showEmptyState && currentList.isEmpty())

        showEmptyState = empty
        if (empty) {
            isLoading = false
        }

        val willShowSpecialState = (isLoading && currentList.isEmpty()) || (showEmptyState && currentList.isEmpty())

        // Only notify if the special state visibility changed
        if (wasShowingSpecialState != willShowSpecialState) {
            if (currentList.isEmpty()) {
                if (wasShowingSpecialState && !willShowSpecialState) {
                    notifyItemRemoved(0)
                } else if (!wasShowingSpecialState && willShowSpecialState) {
                    notifyItemInserted(0)
                } else {
                    notifyItemChanged(0)
                }
            }
        } else if (currentList.isEmpty() && (wasShowingSpecialState || willShowSpecialState)) {
            notifyItemChanged(0)
        }
    }

    fun updateProductFavoriteStatus(productId: Int, isFavorite: Boolean) {
        val index = currentList.indexOfFirst { it.id == productId }
        if (index != -1 && index < currentList.size) {
            val updatedList = currentList.toMutableList()
            updatedList[index] = updatedList[index].copy(isFavorite = isFavorite)
            submitList(updatedList)
        }
    }

    fun updateProductStock(productId: Int, newStock: Int) {
        val index = currentList.indexOfFirst { it.id == productId }
        if (index != -1 && index < currentList.size) {
            val updatedList = currentList.toMutableList()
            updatedList[index] = updatedList[index].copy(stockQuantity = newStock)
            submitList(updatedList)
        }
    }

    override fun submitList(list: List<Product>?) {
        val newSize = list?.size ?: 0
        val oldSize = currentList.size

        // Your state management logic
        if (newSize > 0) {
            isLoading = false
            showEmptyState = false
        } else if (!isLoading) {
            showEmptyState = true
        }

        super.submitList(list)
    }

    abstract class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ProductItemViewHolder(itemView: View) : ProductViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.iv_product_image)
        private val productName: TextView = itemView.findViewById(R.id.tv_product_name)
        private val productDescription: TextView = itemView.findViewById(R.id.tv_product_description)
        private val productPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        private val originalPrice: TextView? = itemView.findViewById(R.id.tv_original_price)
        private val discountBadge: TextView? = itemView.findViewById(R.id.tv_discount_badge)
        private val productRating: TextView = itemView.findViewById(R.id.tv_product_rating)
        private val productBadge: TextView? = itemView.findViewById(R.id.tv_product_badge)
        private val stockStatus: TextView? = itemView.findViewById(R.id.tv_stock_status)
        private val favoriteButton: ImageView? = itemView.findViewById(R.id.iv_favorite)
        private val addToCartButton: View? = itemView.findViewById(R.id.btn_add_to_cart)

        fun bind(product: Product) {
            try {
                // Basic info
                productName.text = product.name
                productDescription.text = product.description
                productRating.text = product.getDisplayRating()

                // Image loading - Use ApiHelper centralized method
                loadProductImage(product)

                // Pricing
                setupPricing(product)

                // Badges and status
                setupBadge(product)
                setupStockStatus(product)
                setupFavoriteButton(product)
                setupClickListeners(product)

            } catch (e: Exception) {
                Log.e(TAG, "Error binding product ${product.id}", e)
                // Fallback to basic display
                productName.text = product.name
                productPrice.text = "$${String.format("%.2f", product.price)}"
                productImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        private fun loadProductImage(product: Product) {
            try {
                // Use ApiHelper's centralized image loading
                val drawableId = ApiHelper.getDrawableResourceId(context, product.imageRes)
                if (drawableId != 0) {
                    Glide.with(context)
                        .load(drawableId)
                        .apply(createGlideOptions())
                        .into(productImage)
                } else {
                    // Fallback to default image
                    productImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image for product ${product.id}", e)
                productImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        private fun createGlideOptions(): RequestOptions {
            return RequestOptions()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
        }

        private fun setupPricing(product: Product) {
            try {
                if (product.discount > 0) {
                    // Show discounted price
                    productPrice.text = "$${String.format("%.2f", product.getDiscountedPrice())}"

                    // Show original price with strikethrough
                    originalPrice?.apply {
                        visibility = View.VISIBLE
                        text = "$${String.format("%.2f", product.price)}"
                        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    }

                    // Show discount badge
                    discountBadge?.apply {
                        visibility = View.VISIBLE
                        text = "${product.discount}% OFF"
                    }
                } else {
                    // Regular price
                    productPrice.text = product.getDisplayPrice()
                    originalPrice?.visibility = View.GONE
                    discountBadge?.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up pricing for product ${product.id}", e)
                productPrice.text = "$${String.format("%.2f", product.price)}"
            }
        }

        private fun setupBadge(product: Product) {
            try {
                productBadge?.let { badge ->
                    if (product.badge.isNotEmpty()) {
                        badge.visibility = View.VISIBLE
                        badge.text = product.badge

                        // Set badge styling based on type
                        val backgroundColor = when (product.badge.lowercase()) {
                            "organic" -> ContextCompat.getColor(context, android.R.color.holo_green_light)
                            "fresh" -> ContextCompat.getColor(context, android.R.color.holo_blue_light)
                            "premium" -> ContextCompat.getColor(context, android.R.color.holo_orange_light)
                            else -> ContextCompat.getColor(context, android.R.color.darker_gray)
                        }

                        badge.setBackgroundColor(backgroundColor)
                        badge.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    } else {
                        badge.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up badge for product ${product.id}", e)
                productBadge?.visibility = View.GONE
            }
        }

        private fun setupStockStatus(product: Product) {
            try {
                stockStatus?.let { status ->
                    status.text = product.getStockStatus()
                    status.visibility = View.VISIBLE

                    val textColor = when {
                        !product.inStock || product.stockQuantity <= 0 ->
                            ContextCompat.getColor(context, android.R.color.holo_red_dark)
                        product.isLowStock() ->
                            ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                        else ->
                            ContextCompat.getColor(context, android.R.color.holo_green_dark)
                    }

                    status.setTextColor(textColor)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up stock status for product ${product.id}", e)
                stockStatus?.visibility = View.GONE
            }
        }

        private fun setupFavoriteButton(product: Product) {
            try {
                favoriteButton?.let { button ->
                    val iconRes = if (product.isFavorite) {
                        android.R.drawable.btn_star_big_on
                    } else {
                        android.R.drawable.btn_star_big_off
                    }

                    val tintColor = if (product.isFavorite) {
                        ContextCompat.getColor(context, android.R.color.holo_red_light)
                    } else {
                        ContextCompat.getColor(context, android.R.color.darker_gray)
                    }

                    button.setImageResource(iconRes)
                    button.setColorFilter(tintColor)
                    button.setOnClickListener { onFavoriteClick(product) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up favorite button for product ${product.id}", e)
            }
        }

        private fun setupClickListeners(product: Product) {
            try {
                itemView.setOnClickListener { onProductClick(product) }

                addToCartButton?.let { button ->
                    val isAvailable = product.inStock && product.stockQuantity > 0
                    button.isEnabled = isAvailable
                    button.alpha = if (isAvailable) 1f else 0.5f
                    button.setOnClickListener {
                        if (isAvailable) {
                            onAddToCartClick(product)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up click listeners for product ${product.id}", e)
            }
        }
    }

    inner class LoadingViewHolder(itemView: View) : ProductViewHolder(itemView) {
        fun bind() {
            // Loading state UI handled by layout
            try {
                itemView.findViewById<TextView>(R.id.tv_loading_message)?.text = "Loading products..."
            } catch (e: Exception) {
                Log.e(TAG, "Error in loading view holder", e)
            }
        }
    }

    inner class EmptyViewHolder(itemView: View) : ProductViewHolder(itemView) {
        fun bind() {
            try {
                itemView.findViewById<TextView>(R.id.tv_empty_message)?.text = "No products found"
                itemView.findViewById<ImageView>(R.id.iv_empty_image)?.setImageResource(
                    android.R.drawable.ic_menu_search
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in empty view holder", e)
            }
        }
    }
}

class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Product, newItem: Product): Any? {
        return when {
            oldItem.isFavorite != newItem.isFavorite -> "favorite"
            oldItem.stockQuantity != newItem.stockQuantity -> "stock"
            oldItem.price != newItem.price || oldItem.discount != newItem.discount -> "price"
            else -> null
        }
    }
}