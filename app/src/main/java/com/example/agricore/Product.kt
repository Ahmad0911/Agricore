package com.example.agricore

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
data class ProductsNew(
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
    var isFavorite: Boolean = false,
    val nutritionInfo: NutritionInfo = NutritionInfo("0 per 100g"),
    val tags: List<String> = emptyList()
) : Parcelable {

    fun getDisplayRating(): String {
        return if (rating > 0) "%.1f ★ (%d)".format(rating, reviewCount) else "No rating"
    }

    fun getDiscountedPrice(): Double = if (discount > 0) price * (100 - discount) / 100 else price

    fun getDisplayPrice(): String {
        return if (discount > 0) {
            "$${"%.2f".format(getDiscountedPrice())} (${discount}% off)"
        } else {
            "$${"%.2f".format(price)} $priceUnit"
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

    fun getFormattedPrice(): String {
        return String.format(Locale.getDefault(), "%.2f", price)
    }

    fun matchesSearch(query: String): Boolean {
        val searchQuery = query.lowercase(Locale.getDefault()).trim()
        return name.contains(searchQuery, ignoreCase = true) ||
                description.contains(searchQuery, ignoreCase = true) ||
                category.contains(searchQuery, ignoreCase = true) ||
                getFormattedPrice().contains(searchQuery)
    }
}

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