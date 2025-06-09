package com.example.agricore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

/**
 * Represents a product in the agricultural catalog.
 *
 * @property id Unique identifier for the product
 * @property name Name of the product
 * @property description Detailed description of the product
 * @property price Current price of the product
 * @property priceUnit Unit of measurement for the price (default: "per kg")
 * @property category Main category the product belongs to
 * @property subcategory Subcategory of the product (default: empty string)
 * @property imageUrl URL of the product image (default: empty string)
 * @property imageRes Resource identifier for the product image (default: "ic_menu_gallery")
 * @property rating Average rating of the product (0.0-5.0)
 * @property reviewCount Number of reviews for the product
 * @property badge Special badge for the product (e.g., "Organic", "Premium")
 * @property inStock Whether the product is currently in stock
 * @property stockQuantity Current stock quantity
 * @property discount Current discount percentage (0-100)
 * @property isFavorite Whether the product is marked as favorite
 * @property nutritionInfo Nutritional information about the product
 * @property tags List of tags associated with the product
 */
@Parcelize
data class Product(
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
) : Parcelable {

    /**
     * Calculates the discounted price of the product.
     * @return The price after applying discount, or original price if no discount
     */
    fun getDiscountedPrice(): Double {
        return if (discount > 0) {
            price * (100 - discount) / 100
        } else {
            price
        }
    }

    /**
     * Checks if the product is low in stock.
     * @return true if stock is less than or equal to 10 units
     */
    fun isLowStock(): Boolean {
        return inStock && stockQuantity > 0 && stockQuantity <= 10
    }

    /**
     * Checks if the product is new (based on review count).
     * @return true if review count is less than 5
     */
    fun isNew(): Boolean {
        return reviewCount < 5
    }

    /**
     * Gets the first available image source (URL or resource).
     * @return The image URL if available, otherwise the resource identifier
     */
    fun getPrimaryImageSource(): String {
        return if (imageUrl.isNotEmpty()) imageUrl else imageRes
    }
}

/**
 * Represents nutritional information for a product.
 *
 * @property calories Calorie information string
 * @property vitaminC Vitamin C content (nullable)
 * @property fiber Fiber content (nullable)
 * @property vitaminA Vitamin A content (nullable)
 * @property potassium Potassium content (nullable)
 * @property iron Iron content (nullable)
 * @property vitaminK Vitamin K content (nullable)
 * @property folate Folate content (nullable)
 * @property waterContent Water content percentage (nullable)
 * @property quercetin Quercetin content (nullable)
 * @property vitaminB6 Vitamin B6 content (nullable)
 * @property antioxidants Antioxidants content (nullable)
 * @property citricAcid Citric acid content (nullable)
 */
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

    /**
     * Gets all available nutrition facts as a map.
     * @return Map of nutrition facts where value is not null
     */
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
        ).filter { it.second != null }
            .associate { it.first to it.second!! }
    }
}

/**
 * Extension function to get a displayable price string.
 */
fun Product.getDisplayPrice(): String {
    return if (discount > 0) {
        "$${"%.2f".format(getDiscountedPrice())} (${discount}% off)"
    } else {
        "$${"%.2f".format(price)} $priceUnit"
    }
}

/**
 * Extension function to get a displayable rating string.
 */
fun Product.getDisplayRating(): String {
    return "%.1f".format(rating)
}

/**
 * Extension function to get stock status string.
 */
fun Product.getStockStatus(): String {
    return when {
        !inStock -> "Out of stock"
        stockQuantity <= 0 -> "Out of stock"
        isLowStock() -> "Low stock ($stockQuantity left)"
        else -> "In stock"
    }
}