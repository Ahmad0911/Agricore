package com.example.agricore

sealed class ProductState {
    object Idle : ProductState()
    object Loading : ProductState()
    data class Error(val message: String) : ProductState()
    object Success : ProductState()
}

data class Product(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val imageRes: Int = android.R.drawable.ic_menu_gallery, // Use only one image property
    val state: ProductState = ProductState.Idle
) {
    // Helper method for search functionality
    fun matchesSearchQuery(query: String): Boolean {
        val searchTerm = query.lowercase().trim()
        return name.lowercase().contains(searchTerm) ||
                description.lowercase().contains(searchTerm)
    }

    // Helper method for price formatting
    fun getFormattedPrice(): String = "$${String.format("%.2f", price)}"
}