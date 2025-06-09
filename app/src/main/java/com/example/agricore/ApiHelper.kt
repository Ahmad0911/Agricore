package com.example.agricore

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException

// Data Classes
@Parcelize
data class Products(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val priceUnit: String = "per kg",
    val category: String,
    val subcategory: String = "",
    val imageUrl: String = "",
    val imageRes: String = "ic_menu_gallery",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val badge: String = "",
    val inStock: Boolean = true,
    val stockQuantity: Int = 0,
    val discount: Int = 0,
    val isFavorite: Boolean = false,
    val nutritionInfo: NutritionInfo = NutritionInfo("0 per 100g"),
    val tags: List<String> = emptyList()
) : Parcelable

@Parcelize
data class NutritionInfos(
    val calories: String,
    @SerializedName("vitamin_c") val vitaminC: String? = null,
    val fiber: String? = null,
    @SerializedName("vitamin_a") val vitaminA: String? = null,
    val potassium: String? = null,
    val iron: String? = null,
    @SerializedName("vitamin_k") val vitaminK: String? = null,
    val folate: String? = null,
    @SerializedName("water_content") val waterContent: String? = null,
    val quercetin: String? = null,
    @SerializedName("vitamin_b6") val vitaminB6: String? = null,
    val antioxidants: String? = null,
    @SerializedName("citric_acid") val citricAcid: String? = null
) : Parcelable

@Parcelize
data class Category(
    val id: Int,
    val name: String,
    val count: Int,
    val isSelected: Boolean = false
) : Parcelable

@Parcelize
data class PriceRange(
    val min: Double,
    val max: Double,
    val label: String
) : Parcelable

@Parcelize
data class FilterOptions(
    @SerializedName("price_ranges") val priceRanges: List<PriceRange>,
    val badges: List<String>,
    val ratings: List<Double>
) : Parcelable

// API Response wrapper
data class ApiResponse(
    val status: String,
    val message: String,
    val data: ProductsData
)

data class ProductsData(
    val products: List<Product>,
    val categories: List<Category>,
    val filters: FilterOptions
)

// Result wrapper for better error handling
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable, val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

// Callback interfaces for async operations
interface ApiCallback<T> {
    fun onSuccess(data: T)
    fun onError(error: String)
}

class ApiHelper(private val context: Context) {

    companion object {
        private const val TAG = "ApiHelper"
        private const val JSON_FILE_NAME = "products_api.json"

        // Utility function to get drawable resource ID from string name
        fun getDrawableResourceId(context: Context, drawableName: String): Int {
            return try {
                val resourceId = context.resources.getIdentifier(
                    drawableName, "drawable", context.packageName
                )
                if (resourceId != 0) resourceId else android.R.drawable.ic_menu_gallery
            } catch (e: Exception) {
                Log.w(TAG, "Could not find drawable: $drawableName", e)
                android.R.drawable.ic_menu_gallery
            }
        }
    }

    private val gson = Gson()
    private var cachedApiResponse: ApiResponse? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Sample JSON data with comprehensive product catalog
    private val sampleJsonData = """
    {
      "status": "success",
      "message": "Products retrieved successfully",
      "data": {
        "products": [
          {
            "id": 1,
            "name": "Organic Tomatoes",
            "description": "Fresh organic produce from local farms. Rich in vitamins and perfect for cooking.",
            "price": 2.99,
            "priceUnit": "per kg",
            "category": "Vegetables",
            "subcategory": "Organic",
            "imageUrl": "https://images.unsplash.com/photo-1546094683-6de8d7ce1c0c?w=400&h=300&fit=crop",
            "imageRes": "ic_tomato",
            "rating": 4.8,
            "reviewCount": 24,
            "badge": "Organic",
            "inStock": true,
            "stockQuantity": 150,
            "discount": 0,
            "isFavorite": false,
            "nutritionInfo": {
              "calories": "18 per 100g",
              "vitamin_c": "High",
              "fiber": "Medium"
            },
            "tags": ["organic", "fresh", "local", "vitamin-rich"]
          },
          {
            "id": 2,
            "name": "Sweet Carrots",
            "description": "Sweet farm-fresh carrots, perfect for snacking and cooking. High in beta-carotene.",
            "price": 1.49,
            "priceUnit": "per kg",
            "category": "Vegetables",
            "subcategory": "Root Vegetables",
            "imageUrl": "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=400&h=300&fit=crop",
            "imageRes": "ic_carrot",
            "rating": 4.6,
            "reviewCount": 18,
            "badge": "Fresh",
            "inStock": true,
            "stockQuantity": 200,
            "discount": 10,
            "isFavorite": false,
            "nutritionInfo": {
              "calories": "41 per 100g",
              "vitamin_a": "Very High",
              "fiber": "High"
            },
            "tags": ["sweet", "fresh", "beta-carotene", "crunchy"]
          },
          {
            "id": 3,
            "name": "Red Apples",
            "description": "Juicy red apples, perfect for snacking. Crisp texture with natural sweetness.",
            "price": 3.99,
            "priceUnit": "per kg",
            "category": "Fruits",
            "subcategory": "Tree Fruits",
            "imageUrl": "https://images.unsplash.com/photo-1619546813926-a78fa6372cd2?w=400&h=300&fit=crop",
            "imageRes": "ic_apple",
            "rating": 4.9,
            "reviewCount": 45,
            "badge": "Premium",
            "inStock": true,
            "stockQuantity": 120,
            "discount": 0,
            "isFavorite": true,
            "nutritionInfo": {
              "calories": "52 per 100g",
              "vitamin_c": "Medium",
              "fiber": "High"
            },
            "tags": ["juicy", "sweet", "crisp", "premium"]
          },
          {
            "id": 4,
            "name": "Russet Potatoes",
            "description": "Organic russet potatoes, great for cooking, baking, and making fries.",
            "price": 1.29,
            "priceUnit": "per kg",
            "category": "Vegetables",
            "subcategory": "Root Vegetables",
            "imageUrl": "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=400&h=300&fit=crop",
            "imageRes": "ic_potato",
            "rating": 4.5,
            "reviewCount": 32,
            "badge": "Organic",
            "inStock": true,
            "stockQuantity": 300,
            "discount": 5,
            "isFavorite": false,
            "nutritionInfo": {
              "calories": "77 per 100g",
              "potassium": "High",
              "vitamin_c": "Medium"
            },
            "tags": ["organic", "versatile", "cooking", "baking"]
          },
          {
            "id": 5,
            "name": "Baby Spinach",
            "description": "Fresh baby spinach leaves, perfect for salads and smoothies. Packed with iron.",
            "price": 2.49,
            "priceUnit": "per bundle",
            "category": "Vegetables",
            "subcategory": "Leafy Greens",
            "imageUrl": "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400&h=300&fit=crop",
            "imageRes": "ic_spinach",
            "rating": 4.7,
            "reviewCount": 28,
            "badge": "Fresh",
            "inStock": true,
            "stockQuantity": 80,
            "discount": 0,
            "isFavorite": false,
            "nutritionInfo": {
              "calories": "23 per 100g",
              "iron": "Very High",
              "vitamin_k": "Very High"
            },
            "tags": ["baby", "fresh", "iron-rich", "salad"]
          },
          {
            "id": 13,
            "name": "Golden Bananas",
            "description": "Ripe golden bananas, perfect for snacking, smoothies, and baking.",
            "price": 1.89,
            "priceUnit": "per bunch",
            "category": "Fruits",
            "subcategory": "Tropical Fruits",
            "imageUrl": "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=400&h=300&fit=crop",
            "imageRes": "ic_banana",
            "rating": 4.8,
            "reviewCount": 67,
            "badge": "Fresh",
            "inStock": true,
            "stockQuantity": 95,
            "discount": 0,
            "isFavorite": true,
            "nutritionInfo": {
              "calories": "89 per 100g",
              "potassium": "Very High",
              "vitamin_b6": "High"
            },
            "tags": ["golden", "ripe", "potassium", "energy"]
          },
          {
            "id": 14,
            "name": "Organic Strawberries",
            "description": "Sweet organic strawberries, perfect for desserts, smoothies, and snacking.",
            "price": 4.99,
            "priceUnit": "per basket",
            "category": "Fruits",
            "subcategory": "Berries",
            "imageUrl": "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=400&h=300&fit=crop",
            "imageRes": "ic_strawberry",
            "rating": 4.9,
            "reviewCount": 89,
            "badge": "Organic",
            "inStock": true,
            "stockQuantity": 45,
            "discount": 20,
            "isFavorite": true,
            "nutritionInfo": {
              "calories": "32 per 100g",
              "vitamin_c": "Very High",
              "antioxidants": "Very High"
            },
            "tags": ["organic", "sweet", "antioxidants", "premium"]
          }
        ],
        "categories": [
          {
            "id": 1,
            "name": "All",
            "count": 7,
            "isSelected": true
          },
          {
            "id": 2,
            "name": "Vegetables",
            "count": 4,
            "isSelected": false
          },
          {
            "id": 3,
            "name": "Fruits",
            "count": 3,
            "isSelected": false
          },
          {
            "id": 4,
            "name": "Organic",
            "count": 3,
            "isSelected": false
          }
        ],
        "filters": {
          "price_ranges": [
            {"min": 0, "max": 1, "label": "Under $1"},
            {"min": 1, "max": 2, "label": "$1 - $2"},
            {"min": 2, "max": 3, "label": "$2 - $3"},
            {"min": 3, "max": 5, "label": "$3 - $5"},
            {"min": 5, "max": 999, "label": "Above $5"}
          ],
          "badges": ["Fresh", "Organic", "Premium"],
          "ratings": [4.0, 4.5, 4.8]
        }
      }
    }
    """.trimIndent()

    // SYNCHRONOUS METHODS (for current implementation compatibility)

    /**
     * Load API data synchronously with comprehensive error handling
     */
    private fun loadApiData(): ApiResponse? {
        return try {
            if (cachedApiResponse == null) {
                Log.d(TAG, "Loading API data...")

                // Try to load from assets first
                val jsonString = loadJsonFromAssets(JSON_FILE_NAME)
                    ?: run {
                        Log.w(TAG, "Assets file not found, using sample data")
                        sampleJsonData
                    }

                cachedApiResponse = gson.fromJson(jsonString, ApiResponse::class.java)

                // Validate loaded data
                cachedApiResponse?.let { response ->
                    if (response.status != "success") {
                        Log.w(TAG, "API response indicates error: ${response.message}")
                        return getFallbackApiResponse()
                    }
                    if (response.data.products.isEmpty()) {
                        Log.w(TAG, "No products found in API response")
                        return getFallbackApiResponse()
                    }
                    Log.d(TAG, "Successfully loaded ${response.data.products.size} products")
                } ?: run {
                    Log.e(TAG, "Failed to parse API data")
                    return getFallbackApiResponse()
                }
            }
            cachedApiResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error loading API data", e)
            getFallbackApiResponse()
        }
    }

    /**
     * Load JSON from assets with null safety
     */
    private fun loadJsonFromAssets(fileName: String): String? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Could not read JSON file: $fileName", e)
            null
        }
    }

    /**
     * Get fallback API response with default data
     */
    private fun getFallbackApiResponse(): ApiResponse {
        return ApiResponse(
            status = "success",
            message = "Using fallback data",
            data = ProductsData(
                products = getFallbackProducts(),
                categories = getDefaultCategories(),
                filters = getDefaultFilterOptions()
            )
        )
    }

    /**
     * Get all products with comprehensive error handling
     */
    fun getProducts(): List<Product> {
        return try {
            val response = loadApiData()
            response?.data?.products ?: getFallbackProducts()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting products", e)
            getFallbackProducts()
        }
    }

    /**
     * Get all categories
     */
    fun getCategories(): List<Category> {
        return try {
            val response = loadApiData()
            response?.data?.categories ?: getDefaultCategories()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting categories", e)
            getDefaultCategories()
        }
    }

    /**
     * Get filter options
     */
    fun getFilterOptions(): FilterOptions? {
        return try {
            val response = loadApiData()
            response?.data?.filters ?: getDefaultFilterOptions()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting filter options", e)
            getDefaultFilterOptions()
        }
    }

    // FILTERING AND SEARCH METHODS

    /**
     * Get products by category with case-insensitive matching
     */
    fun getProductsByCategory(categoryName: String): List<Product> {
        return try {
            val allProducts = getProducts()
            when {
                categoryName.equals("All", ignoreCase = true) -> allProducts
                categoryName.equals("Organic", ignoreCase = true) ->
                    allProducts.filter { it.badge.equals("Organic", ignoreCase = true) }
                else -> allProducts.filter { it.category.equals(categoryName, ignoreCase = true) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting products by category: $categoryName", e)
            emptyList()
        }
    }

    /**
     * Get products by badge
     */
    fun getProductsByBadge(badge: String): List<Product> {
        return try {
            getProducts().filter { it.badge.equals(badge, ignoreCase = true) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting products by badge: $badge", e)
            emptyList()
        }
    }

    /**
     * Get products by price range
     */
    fun getProductsByPriceRange(minPrice: Double, maxPrice: Double): List<Product> {
        return try {
            getProducts().filter { product ->
                product.price >= minPrice &&
                        (maxPrice >= 999 || product.price <= maxPrice) // Handle "Above $X" case
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting products by price range: $minPrice-$maxPrice", e)
            emptyList()
        }
    }

    /**
     * Get products by minimum rating
     */
    fun getProductsByMinRating(minRating: Double): List<Product> {
        return try {
            getProducts().filter { it.rating >= minRating }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting products by min rating: $minRating", e)
            emptyList()
        }
    }

    /**
     * Get favorite products
     */
    fun getFavoriteProducts(): List<Product> {
        return try {
            getProducts().filter { it.isFavorite }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting favorite products", e)
            emptyList()
        }
    }

    /**
     * Get products in stock
     */
    fun getInStockProducts(): List<Product> {
        return try {
            getProducts().filter { it.inStock && it.stockQuantity > 0 }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting in-stock products", e)
            emptyList()
        }
    }

    /**
     * Get products with discount
     */
    fun getDiscountedProducts(): List<Product> {
        return try {
            getProducts().filter { it.discount > 0 }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting discounted products", e)
            emptyList()
        }
    }

    /**
     * Enhanced search with weighted results
     */
    fun searchProducts(query: String): List<Product> {
        return try {
            if (query.isBlank()) return getProducts()

            val allProducts = getProducts()
            val searchQuery = query.lowercase().trim()

            // Weighted search results
            val exactMatches = mutableListOf<Product>()
            val partialMatches = mutableListOf<Product>()
            val tagMatches = mutableListOf<Product>()

            allProducts.forEach { product ->
                val productName = product.name.lowercase()
                val productDesc = product.description.lowercase()
                val productCategory = product.category.lowercase()

                when {
                    productName == searchQuery -> exactMatches.add(product)
                    productName.contains(searchQuery) ||
                            productDesc.contains(searchQuery) ||
                            productCategory.contains(searchQuery) -> partialMatches.add(product)
                    product.tags.any { it.lowercase().contains(searchQuery) } -> tagMatches.add(product)
                }
            }

            // Return results in order of relevance
            (exactMatches + partialMatches + tagMatches).distinctBy { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching products with query: $query", e)
            emptyList()
        }
    }

    /**
     * Get product by ID with null safety
     */
    fun getProductById(productId: Int): Product? {
        return try {
            getProducts().find { it.id == productId }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product by ID: $productId", e)
            null
        }
    }

    // UTILITY METHODS

    /**
     * Get API status and message
     */
    fun getApiStatus(): Pair<String, String> {
        return try {
            val response = loadApiData()
            response?.let {
                Pair(it.status, it.message)
            } ?: Pair("error", "Failed to load data")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting API status", e)
            Pair("error", "Exception: ${e.message}")
        }
    }

    /**
     * Clear cache and reload data
     */
    fun refreshData() {
        try {
            cachedApiResponse = null
            Log.d(TAG, "Data cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing data", e)
        }
    }

    /**
     * Get total product count
     */
    fun getTotalProductCount(): Int {
        return try {
            getProducts().size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total product count", e)
            0
        }
    }

    /**
     * Get category product count with updated counts
     */
    fun getCategoryProductCount(categoryName: String): Int {
        return try {
            getProductsByCategory(categoryName).size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting category product count for: $categoryName", e)
            0
        }
    }

    /**
     * Get updated categories with current product counts
     */
    fun getCategoriesWithUpdatedCounts(): List<Category> {
        return try {
            val categories = getCategories().toMutableList()
            categories.forEach { category ->
                val updatedCategory = category.copy(
                    count = getCategoryProductCount(category.name)
                )
                val index = categories.indexOf(category)
                categories[index] = updatedCategory
            }
            categories
        } catch (e: Exception) {
            Log.e(TAG, "Error updating category counts", e)
            getCategories()
        }
    }

    // ASYNC METHODS (for future use)

    /**
     * Load products asynchronously
     */
    fun getProductsAsync(callback: ApiCallback<List<Product>>) {
        coroutineScope.launch {
            try {
                val products = getProducts()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(products)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Failed to load products: ${e.message}")
                }
            }
        }
    }

    /**
     * Search products asynchronously
     */
    fun searchProductsAsync(query: String, callback: ApiCallback<List<Product>>) {
        coroutineScope.launch {
            try {
                val results = searchProducts(query)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(results)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Search failed: ${e.message}")
                }
            }
        }
    }

    // FALLBACK DATA METHODS

    private fun getFallbackProducts(): List<Product> {
        return listOf(
            Product(
                id = 1,
                name = "Organic Tomatoes",
                description = "Fresh organic produce from local farms",
                price = 2.99,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Organic",
                imageRes = "ic_tomato",
                rating = 4.8,
                reviewCount = 24,
                badge = "Organic",
                inStock = true,
                stockQuantity = 150,
                nutritionInfo = NutritionInfo("18 per 100g", vitaminC = "High"),
                tags = listOf("organic", "fresh", "local")
            ),
            Product(
                id = 2,
                name = "Sweet Carrots",
                description = "Sweet farm-fresh carrots, perfect for snacking",
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
                nutritionInfo = NutritionInfo("41 per 100g", vitaminA = "Very High"),
                tags = listOf("sweet", "fresh", "crunchy")
            ),
            Product(
                id = 3,
                name = "Red Apples",
                description = "Juicy red apples with natural sweetness",
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
                isFavorite = true,
                nutritionInfo = NutritionInfo("52 per 100g", vitaminC = "Medium"),
                tags = listOf("juicy", "sweet", "crisp")
            )
        )
    }

    private fun getDefaultCategories(): List<Category> {
        return listOf(
            Category(1, "All", 0, true),
            Category(2, "Vegetables", 0, false),
            Category(3, "Fruits", 0, false),
            Category(4, "Organic", 0, false)
        )
    }

    private fun getDefaultFilterOptions(): FilterOptions {
        return FilterOptions(
            priceRanges = listOf(
                PriceRange(0.0, 1.0, "Under $1"),
                PriceRange(1.0, 2.0, "$1 - $2"),
                PriceRange(2.0, 3.0, "$2 - $3"),
                PriceRange(3.0, 5.0, "$3 - $5"),
                PriceRange(5.0, 999.0, "Above $5")
            ),
            badges = listOf("Fresh", "Organic", "Premium"),
            ratings = listOf(4.0, 4.5, 4.8)
        )
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            coroutineScope.cancel()
            cachedApiResponse = null
            Log.d(TAG, "ApiHelper cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}