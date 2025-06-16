package com.example.agricore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class WeatherForecastAdapter : ListAdapter<WeatherForecast, WeatherForecastAdapter.ForecastViewHolder>(ForecastDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayText: TextView = itemView.findViewById(R.id.tv_day)
        private val conditionIcon: ImageView = itemView.findViewById(R.id.iv_condition_icon)
        private val conditionText: TextView = itemView.findViewById(R.id.tv_condition)
        private val highTempText: TextView = itemView.findViewById(R.id.tv_high_temp)
        private val lowTempText: TextView = itemView.findViewById(R.id.tv_low_temp)
        private val humidityText: TextView = itemView.findViewById(R.id.tv_humidity)
        private val rainChanceText: TextView = itemView.findViewById(R.id.tv_rain_chance)

        fun bind(forecast: WeatherForecast) {
            dayText.text = forecast.dayName
            conditionText.text = forecast.condition
            highTempText.text = "${forecast.highTemp}°"
            lowTempText.text = "${forecast.lowTemp}°"
            humidityText.text = "${forecast.humidity}%"
            rainChanceText.text = "${forecast.chanceOfRain}%"

            // Set weather icon based on condition
            val iconRes = when (forecast.condition.lowercase()) {
                "sunny" -> android.R.drawable.ic_dialog_info // Use system icons as placeholder
                "partly cloudy" -> android.R.drawable.ic_partial_secure
                "cloudy" -> android.R.drawable.ic_dialog_alert
                "light rain", "heavy rain" -> android.R.drawable.ic_dialog_email
                "thunderstorm" -> android.R.drawable.ic_dialog_dialer
                else -> android.R.drawable.ic_dialog_info
            }
            conditionIcon.setImageResource(iconRes)

            // Set rain chance color
            val rainColor = when {
                forecast.chanceOfRain >= 70 -> ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                forecast.chanceOfRain >= 40 -> ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                else -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
            }
            rainChanceText.setTextColor(rainColor)
        }
    }
}

class ForecastDiffCallback : DiffUtil.ItemCallback<WeatherForecast>() {
    override fun areItemsTheSame(oldItem: WeatherForecast, newItem: WeatherForecast): Boolean {
        return oldItem.date == newItem.date
    }

    override fun areContentsTheSame(oldItem: WeatherForecast, newItem: WeatherForecast): Boolean {
        return oldItem == newItem
    }
}