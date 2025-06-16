package com.example.agricore

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import java.io.IOException
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
data class Product(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val priceUnit: String = "per kg",
    val category: String,
    val subcategory: String = "",
    @Deprecated("Use imageRes for local drawables")
    val imageUrl: String = "",
    val imageRes: String = "ic_menu_gallery",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val badge: String = "",
    val inStock: Boolean = true,
    val stockQuantity: Int = 0,
    val discount: Int = 0,
    var isFavorite: Boolean = false,
    val nutritionInfo: NutritionInfo = NutritionInfo("0 per 100g"),
    val tags: List<String> = emptyList()
) : Parcelable {

    fun getDisplayRating(): String {
        return if (rating > 0) "%.1f ★ (%d)".format(Locale.getDefault(), rating, reviewCount) else "No rating"
    }

    fun getDiscountedPrice(): Double = if (discount > 0) price * (100 - discount) / 100 else price

    fun getDisplayPrice(): String {
        return if (discount > 0) {
            "$${"%.2f".format(Locale.getDefault(), getDiscountedPrice())} (${discount}% off)"
        } else {
            "$${"%.2f".format(Locale.getDefault(), price)} $priceUnit"
        }
    }

    fun isLowStock(): Boolean = inStock && stockQuantity > 0 && stockQuantity <= 10

    fun isNew(): Boolean = reviewCount < 5

    fun getStockStatus(): String = when {
        !inStock -> "Out of stock"
        stockQuantity <= 0 -> "Out of stock"
        isLowStock() -> "Low stock ($stockQuantity left)"
        else -> "In stock"
    }
}

@Parcelize
data class NutritionInfo(
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
) : Parcelable {
    fun getAvailableNutritionFacts(): Map<String, String> {
        return listOfNotNull(
            "Calories" to calories,
            "Vitamin C" to vitaminC,
            "Fiber" to fiber,
            "Vitamin A" to vitaminA,
            "Potassium" to potassium,
            "Iron" to iron,
            "Vitamin K" to vitaminK,
            "Folate" to folate,
            "Water Content" to waterContent,
            "Quercetin" to quercetin,
            "Vitamin B6" to vitaminB6,
            "Antioxidants" to antioxidants,
            "Citric Acid" to citricAcid
        ).filter { it.second != null }.associate { it.first to it.second!! }
    }
}

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
    val ratings: List<Double>,
    val categories: List<Category>
) : Parcelable

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable, val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

interface ApiCallback<T> {
    fun onSuccess(data: T)
    fun onError(error: String)
}

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

class ApiHelper(private val context: Context) {

    companion object {
        private const val TAG = "ApiHelper"
        private const val JSON_FILE_NAME = "products_api.json"
        private const val DEFAULT_FALLBACK_IMAGE = "ic_menu_gallery"

        private val imageMappings = mapOf(
            "tomato" to "ic_tomatoes",
            "carrot" to "ic_carrot",
            "apple" to "ic_apple",
            "banana" to "ic_banana",
            "potato" to "ic_potato",
            "spinach" to "ic_spinach",
            "lettuce" to "ic_lettuce",
            "pepper" to "ic_bell_pepper",
            "cucumber" to "ic_cucumber",
            "broccoli" to "ic_broccoli",
            "corn" to "ic_corn",
            "onion" to "ic_onion",
            "strawberry" to "ic_strawberry",
            "lemon" to "ic_lemon",
            "green_beans" to "ic_green_beans"
        )

        /**
         * Safely gets drawable resource ID with fallback
         */
        fun getDrawableResourceId(context: Context, drawableName: String): Int {
            return try {
                Log.d(TAG, "Looking for drawable: $drawableName")

                val cleanName = drawableName.substringBeforeLast(".")

                // Try multiple variations
                val namesToTry = listOf(
                    drawableName,           // Original name
                    cleanName,              // Without extension
                    "ic_$cleanName",        // With ic_ prefix
                    "img_$cleanName"        // With img_ prefix
                )

                var resourceId = 0
                for (name in namesToTry) {
                    resourceId = context.resources.getIdentifier(
                        name,
                        "drawable",
                        context.packageName
                    )
                    if (resourceId != 0) {
                        Log.d(TAG, "Found drawable $name with ID: $resourceId")
                        break
                    }
                }

                // Final fallback to system drawable
                if (resourceId == 0) {
                    Log.w(TAG, "No drawable found for $drawableName, using fallback")
                    android.R.drawable.ic_menu_gallery
                } else {
                    resourceId
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting drawable ID for $drawableName", e)
                android.R.drawable.ic_menu_gallery
            }
        }

        /**
         * Gets the appropriate image resource name for a product
         */
        fun getImageResourceForProduct(productName: String, category: String): String {
            val lowerName = productName.lowercase(Locale.getDefault())
            return imageMappings.entries.firstOrNull { (key, _) ->
                lowerName.contains(key)
            }?.value ?: DEFAULT_FALLBACK_IMAGE
        }
    }

    private val gson = Gson()
    private var cachedApiResponse: ApiResponse? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getProducts(): List<Product> = try {
        val response = loadProductsFromJson()
        Log.d(TAG, "Loaded ${response.data.products.size} products from JSON")

        response.data.products.map { product ->
            val finalImageRes = when {
                product.imageRes.isNotBlank() && product.imageRes != DEFAULT_FALLBACK_IMAGE -> {
                    product.imageRes.substringBeforeLast(".") // Remove extension for resource lookup
                }
                else -> getImageResourceForProduct(product.name, product.category)
            }

            product.copy(
                imageRes = finalImageRes,
                imageUrl = "" // Clear URL since we're using local drawables
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading products", e)
        // Return some fallback data for testing
        createFallbackProducts()
    }

    fun getCategories(): List<Category> = try {
        val response = loadProductsFromJson()
        Log.d(TAG, "Loaded ${response.data.categories.size} categories from JSON")
        response.data.categories
    } catch (e: Exception) {
        Log.e(TAG, "Error loading categories", e)
        createFallbackCategories()
    }

    fun getFilterOptions(): FilterOptions = try {
        loadProductsFromJson().data.filters
    } catch (e: Exception) {
        Log.e(TAG, "Error loading filters", e)
        FilterOptions(emptyList(), emptyList(), emptyList(), emptyList())
    }

    fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) return getProducts()

        return try {
            val lowerQuery = query.lowercase(Locale.getDefault())
            getProducts().filter { product ->
                product.name.contains(lowerQuery, ignoreCase = true) ||
                        product.description.contains(lowerQuery, ignoreCase = true) ||
                        product.category.contains(lowerQuery, ignoreCase = true) ||
                        product.tags.any { it.contains(lowerQuery, ignoreCase = true) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching products", e)
            emptyList()
        }
    }

    fun getProductsByCategory(categoryId: Int): List<Product> = try {
        getCategories().find { it.id == categoryId }?.let { category ->
            getProducts().filter { it.category.equals(category.name, ignoreCase = true) }
        } ?: emptyList()
    } catch (e: Exception) {
        Log.e(TAG, "Error loading products by category", e)
        emptyList()
    }

    fun getProductById(productId: Int): Product? = try {
        getProducts().firstOrNull { it.id == productId }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading product by ID", e)
        null
    }

    fun loadProductsAsync(callback: ApiCallback<ApiResponse>) {
        coroutineScope.launch {
            try {
                val result = loadProductsFromJson()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Failed to load products: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    private fun loadProductsFromJson(): ApiResponse {
        return cachedApiResponse ?: try {
            Log.d(TAG, "Attempting to load products from $JSON_FILE_NAME")

            context.assets.open(JSON_FILE_NAME).use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    val jsonContent = reader.readText()
                    Log.d(TAG, "JSON file content length: ${jsonContent.length}")

                    gson.fromJson(jsonContent, ApiResponse::class.java).also {
                        cachedApiResponse = it
                        Log.d(TAG, "Successfully loaded ${it.data.products.size} products")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "JSON file not found or cannot be read", e)
            createFallbackResponse()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON", e)
            createFallbackResponse()
        }
    }

    fun clearCache() {
        cachedApiResponse = null
    }

    fun cleanup() {
        coroutineScope.cancel()
    }

    private fun createFallbackResponse(): ApiResponse {
        Log.w(TAG, "Creating fallback response with sample data")
        return ApiResponse(
            status = "fallback",
            message = "Using fallback data - JSON file not found",
            data = ProductsData(
                products = createFallbackProducts(),
                categories = createFallbackCategories(),
                filters = FilterOptions(emptyList(), emptyList(), emptyList(), emptyList())
            )
        )
    }

    private fun createFallbackProducts(): List<Product> {
        return listOf(
            Product(
                id = 1,
                name = "Organic Tomatoes",
                description = "Fresh organic produce from local farms. Rich in vitamins and perfect for cooking.",
                price = 2.99,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Organic",
                imageRes = "ic_tomatoes",
                rating = 4.8,
                reviewCount = 24,
                badge = "Organic",
                inStock = true,
                stockQuantity = 150,
                discount = 0,
                isFavorite = false,
                tags = listOf("organic", "fresh", "local", "vitamin-rich")
            ),
            Product(
                id = 2,
                name = "Sweet Carrots",
                description = "Sweet farm-fresh carrots, perfect for snacking and cooking. High in beta-carotene.",
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
                isFavorite = false,
                tags = listOf("sweet", "fresh", "beta-carotene", "crunchy")
            ),
            Product(
                id = 3,
                name = "Red Apples",
                description = "Juicy red apples, perfect for snacking. Crisp texture with natural sweetness.",
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
                discount = 0,
                isFavorite = true,
                tags = listOf("juicy", "sweet", "crisp", "premium")
            ),
            Product(
                id = 4,
                name = "Russet Potatoes",
                description = "Organic russet potatoes, great for cooking, baking, and making fries.",
                price = 1.29,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Root Vegetables",
                imageRes = "ic_potato",
                rating = 4.5,
                reviewCount = 32,
                badge = "Organic",
                inStock = true,
                stockQuantity = 300,
                discount = 5,
                isFavorite = false,
                tags = listOf("organic", "versatile", "cooking", "baking")
            ),
            Product(
                id = 5,
                name = "Baby Spinach",
                description = "Fresh baby spinach leaves, perfect for salads and smoothies. Packed with iron.",
                price = 2.49,
                priceUnit = "per bundle",
                category = "Vegetables",
                subcategory = "Leafy Greens",
                imageRes = "ic_spinach",
                rating = 4.7,
                reviewCount = 28,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 80,
                discount = 0,
                isFavorite = false,
                tags = listOf("baby", "fresh", "iron-rich", "salad")
            ),
            Product(
                id = 6,
                name = "Organic Lettuce",
                description = "Crisp fresh lettuce leaves, perfect for salads and sandwiches.",
                price = 1.99,
                priceUnit = "per head",
                category = "Vegetables",
                subcategory = "Leafy Greens",
                imageRes = "ic_lettuce",
                rating = 4.4,
                reviewCount = 15,
                badge = "Organic",
                inStock = true,
                stockQuantity = 60,
                discount = 0,
                isFavorite = false,
                tags = listOf("organic", "crisp", "fresh", "salad")
            ),
            Product(
                id = 7,
                name = "Bell Peppers Mix",
                description = "Colorful bell peppers mix - red, yellow, and green. Perfect for cooking and salads.",
                price = 3.49,
                priceUnit = "per pack",
                category = "Vegetables",
                subcategory = "Peppers",
                imageRes = "ic_bell_pepper",
                rating = 4.6,
                reviewCount = 21,
                badge = "Premium",
                inStock = true,
                stockQuantity = 90,
                discount = 15,
                isFavorite = true,
                tags = listOf("colorful", "mix", "vitamin-c", "premium")
            ),
            Product(
                id = 8,
                name = "Green Cucumbers",
                description = "Fresh green cucumbers, perfect for salads, sandwiches, and healthy snacking.",
                price = 1.79,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Vine Vegetables",
                imageRes = "ic_cucumber",
                rating = 4.3,
                reviewCount = 19,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 140,
                discount = 0,
                isFavorite = false,
                tags = listOf("fresh", "hydrating", "low-calorie", "crunchy")
            ),
            Product(
                id = 9,
                name = "Fresh Broccoli",
                description = "Fresh green broccoli crowns, packed with nutrients and perfect for steaming or stir-frying.",
                price = 2.29,
                priceUnit = "per head",
                category = "Vegetables",
                subcategory = "Cruciferous",
                imageRes = "ic_broccoli",
                rating = 4.5,
                reviewCount = 26,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 75,
                discount = 0,
                isFavorite = false,
                tags = listOf("fresh", "nutritious", "vitamin-rich", "superfood")
            ),
            Product(
                id = 10,
                name = "Sweet Corn",
                description = "Fresh sweet corn on the cob, perfect for grilling, boiling, or roasting.",
                price = 0.79,
                priceUnit = "per ear",
                category = "Vegetables",
                subcategory = "Grain Vegetables",
                imageRes = "ic_corn",
                rating = 4.7,
                reviewCount = 33,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 180,
                discount = 0,
                isFavorite = false,
                tags = listOf("sweet", "fresh", "grilling", "summer")
            ),
            Product(
                id = 11,
                name = "Red Onions",
                description = "Fresh red onions with mild flavor, perfect for salads, cooking, and caramelizing.",
                price = 1.19,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Bulb Vegetables",
                imageRes = "ic_onion",
                rating = 4.2,
                reviewCount = 14,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 220,
                discount = 0,
                isFavorite = false,
                tags = listOf("red", "mild", "cooking", "versatile")
            ),
            Product(
                id = 12,
                name = "Green Beans",
                description = "Tender green beans, perfect for steaming, sautéing, or adding to casseroles.",
                price = 2.99,
                priceUnit = "per kg",
                category = "Vegetables",
                subcategory = "Pod Vegetables",
                imageRes = "ic_green_beans",
                rating = 4.4,
                reviewCount = 22,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 110,
                discount = 0,
                isFavorite = false,
                tags = listOf("tender", "green", "fresh", "versatile")
            ),
            Product(
                id = 13,
                name = "Golden Bananas",
                description = "Ripe golden bananas, perfect for snacking, smoothies, and baking.",
                price = 1.89,
                priceUnit = "per bunch",
                category = "Fruits",
                subcategory = "Tropical Fruits",
                imageRes = "ic_banana",
                rating = 4.8,
                reviewCount = 67,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 95,
                discount = 0,
                isFavorite = true,
                tags = listOf("golden", "ripe", "potassium", "energy")
            ),
            Product(
                id = 14,
                name = "Organic Strawberries",
                description = "Sweet organic strawberries, perfect for desserts, smoothies, and snacking.",
                price = 4.99,
                priceUnit = "per basket",
                category = "Fruits",
                subcategory = "Berries",
                imageRes = "ic_strawberry",
                rating = 4.9,
                reviewCount = 89,
                badge = "Organic",
                inStock = true,
                stockQuantity = 45,
                discount = 20,
                isFavorite = true,
                tags = listOf("organic", "sweet", "antioxidants", "premium")
            ),
            Product(
                id = 15,
                name = "Fresh Lemons",
                description = "Juicy fresh lemons, perfect for cooking, drinks, and adding zest to dishes.",
                price = 2.49,
                priceUnit = "per kg",
                category = "Fruits",
                subcategory = "Citrus Fruits",
                imageRes = "ic_lemon",
                rating = 4.6,
                reviewCount = 31,
                badge = "Fresh",
                inStock = true,
                stockQuantity = 160,
                discount = 0,
                isFavorite = false,
                tags = listOf("juicy", "citrus", "vitamin-c", "zesty")
            )
        )
    }

    private fun createFallbackCategories(): List<Category> {
        return listOf(
            Category(1, "Vegetables", 15),
            Category(2, "Fruits", 12),
            Category(3, "Dairy", 8),
            Category(4, "Meat", 6)
        )
    }
}