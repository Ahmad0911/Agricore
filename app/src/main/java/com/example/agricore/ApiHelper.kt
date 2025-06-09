package com.example.agricore

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStream

class JsonParser {
    companion object {
        private const val TAG = "JsonParser"
        private val gson = Gson()

        /**
         * Parse products from JSON file in assets folder
         */
        fun parseProductsFromAssets(context: Context, fileName: String): ProductsData? {
            return try {
                val jsonString = loadJsonFromAssets(context, fileName)
                parseProductsFromJson(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing products from assets", e)
                null
            }
        }

        /**
         * Parse products from JSON string
         */
        fun parseProductsFromJson(jsonString: String): ProductsData? {
            return try {
                val productResponse = gson.fromJson(jsonString, ProductResponse::class.java)

                val products = productResponse.products.map { it.toProduct() }
                val categories = productResponse.categories?.map { it.toCategory() } ?: getDefaultCategories()
                val filters = productResponse.filters?.toFilterOptions() ?: getDefaultFilterOptions()

                ProductsData(
                    products = products,
                    categories = categories,
                    filters = filters
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON", e)
                null
            }
        }

        /**
         * Load JSON file from assets folder
         */
        private fun loadJsonFromAssets(context: Context, fileName: String): String {
            return try {
                val inputStream: InputStream = context.assets.open(fileName)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                String(buffer, Charsets.UTF_8)
            } catch (e: IOException) {
                Log.e(TAG, "Error loading JSON from assets", e)
                throw e
            }
        }

        /**
         * Parse single product from JSON string
         */
        fun parseProductFromJson(jsonString: String): Product? {
            return try {
                val productJson = gson.fromJson(jsonString, ProductJson::class.java)
                productJson.toProduct()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing single product", e)
                null
            }
        }

        /**
         * Parse list of products from JSON string
         */
        fun parseProductListFromJson(jsonString: String): List<Product>? {
            return try {
                val type = object : TypeToken<List<ProductJson>>() {}.type
                val productJsonList: List<ProductJson> = gson.fromJson(jsonString, type)
                productJsonList.map { it.toProduct() }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing product list", e)
                null
            }
        }

        /**
         * Convert Product object to JSON string
         */
        fun productToJson(product: Product): String {
            return gson.toJson(product)
        }

        /**
         * Convert list of products to JSON string
         */
        fun productsToJson(products: List<Product>): String {
            return gson.toJson(products)
        }

        /**
         * Default categories if not provided in JSON
         */
        private fun getDefaultCategories(): List<Category> {
            return listOf(
                Category(1, "All", 0, true),
                Category(2, "Vegetables", 0, false),
                Category(3, "Fruits", 0, false),
                Category(4, "Organic", 0, false)
            )
        }

        /**
         * Default filter options if not provided in JSON
         */
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
    }
}

// Helper class for managing product data
class ProductDataManager(private val context: Context) {
    private var cachedProductsData: ProductsData? = null

    /**
     * Load products from JSON file with caching
     */
    fun loadProducts(fileName: String = "products.json", forceReload: Boolean = false): ProductsData? {
        return if (cachedProductsData != null && !forceReload) {
            cachedProductsData
        } else {
            val data = JsonParser.parseProductsFromAssets(context, fileName)
            cachedProductsData = data
            data
        }
    }

    /**
     * Get products by category
     */
    fun getProductsByCategory(category: String): List<Product> {
        val data = cachedProductsData ?: return emptyList()
        return if (category == "All") {
            data.products
        } else {
            data.products.filter {
                it.category.equals(category, ignoreCase = true) ||
                        it.subcategory.equals(category, ignoreCase = true)
            }
        }
    }

    /**
     * Search products
     */
    fun searchProducts(query: String): List<Product> {
        val data = cachedProductsData ?: return emptyList()
        val searchQuery = query.lowercase().trim()

        return data.products.filter { product ->
            product.name.lowercase().contains(searchQuery) ||
                    product.description.lowercase().contains(searchQuery) ||
                    product.category.lowercase().contains(searchQuery) ||
                    product.subcategory.lowercase().contains(searchQuery) ||
                    product.tags.any { tag -> tag.lowercase().contains(searchQuery) }
        }
    }

    /**
     * Get products by price range
     */
    fun getProductsByPriceRange(minPrice: Double, maxPrice: Double): List<Product> {
        val data = cachedProductsData ?: return emptyList()
        return data.products.filter { it.price >= minPrice && it.price <= maxPrice }
    }

    /**
     * Get products by rating
     */
    fun getProductsByMinRating(minRating: Double): List<Product> {
        val data = cachedProductsData ?: return emptyList()
        return data.products.filter { it.rating >= minRating }
    }

    /**
     * Clear cached data
     */
    fun clearCache() {
        cachedProductsData = null
    }
}