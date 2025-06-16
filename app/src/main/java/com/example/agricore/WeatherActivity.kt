package com.example.agricore

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WeatherActivity"
    }

    // Views
    private lateinit var currentLocationText: TextView
    private lateinit var currentTempText: TextView
    private lateinit var currentConditionText: TextView
    private lateinit var currentHumidityText: TextView
    private lateinit var currentWindText: TextView
    private lateinit var lastUpdatedText: TextView
    private lateinit var forecastRecyclerView: RecyclerView
    private lateinit var loadingView: View
    private lateinit var contentView: View

    // Data
    private lateinit var forecastAdapter: WeatherForecastAdapter
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FIX: Use the correct layout file for the activity
        setContentView(R.layout.activity_weather) // Changed from item_weather_forecast

        try {
            setupToolbar()
            initializeViews()
            setupRecyclerView()
            loadWeatherData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Failed to initialize weather page")
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Weather Update"
        }
    }

    private fun initializeViews() {
        currentLocationText = findViewById(R.id.tv_current_location)
        currentTempText = findViewById(R.id.tv_current_temp)
        currentConditionText = findViewById(R.id.tv_current_condition)
        currentHumidityText = findViewById(R.id.tv_current_humidity)
        currentWindText = findViewById(R.id.tv_current_wind)
        lastUpdatedText = findViewById(R.id.tv_last_updated)
        forecastRecyclerView = findViewById(R.id.rv_weather_forecast)
        loadingView = findViewById(R.id.layout_loading)
        contentView = findViewById(R.id.layout_content)
    }

    private fun setupRecyclerView() {
        forecastAdapter = WeatherForecastAdapter()
        forecastRecyclerView.apply {
            adapter = forecastAdapter
            layoutManager = LinearLayoutManager(this@WeatherActivity)
            setHasFixedSize(true)
        }
    }

    private fun loadWeatherData() {
        showLoading(true)

        activityScope.launch {
            try {
                // Simulate API call - replace with actual weather API
                delay(1500) // Simulate network delay

                val currentWeather = getCurrentWeatherData()
                val forecast = getWeatherForecast()

                withContext(Dispatchers.Main) {
                    displayCurrentWeather(currentWeather)
                    forecastAdapter.submitList(forecast)
                    showLoading(false)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading weather data", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to load weather data")
                    showLoading(false)
                }
            }
        }
    }

    private suspend fun getCurrentWeatherData(): WeatherData {
        // Replace with actual API call
        return withContext(Dispatchers.IO) {
            WeatherData(
                location = "Abuja, FCT",
                temperature = 28,
                condition = "Partly Cloudy",
                humidity = 65,
                windSpeed = 12,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private suspend fun getWeatherForecast(): List<WeatherForecast> {
        // Replace with actual API call
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            List(7) { index ->
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                WeatherForecast(
                    date = calendar.timeInMillis,
                    dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time),
                    condition = getRandomCondition(),
                    highTemp = (25..35).random(),
                    lowTemp = (18..25).random(),
                    humidity = (40..80).random(),
                    chanceOfRain = (0..100).random()
                )
            }
        }
    }

    private fun getRandomCondition(): String {
        val conditions = arrayOf("Sunny", "Partly Cloudy", "Cloudy", "Light Rain", "Heavy Rain", "Thunderstorm")
        return conditions.random()
    }

    private fun displayCurrentWeather(weather: WeatherData) {
        currentLocationText.text = weather.location
        currentTempText.text = "${weather.temperature}°C"
        currentConditionText.text = weather.condition
        currentHumidityText.text = "Humidity: ${weather.humidity}%"
        currentWindText.text = "Wind: ${weather.windSpeed} km/h"

        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        lastUpdatedText.text = "Last updated: ${formatter.format(Date(weather.lastUpdated))}"
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
        } else {
            loadingView.visibility = View.GONE
            contentView.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}

// Data classes
data class WeatherData(
    val location: String,
    val temperature: Int,
    val condition: String,
    val humidity: Int,
    val windSpeed: Int,
    val lastUpdated: Long
)

data class WeatherForecast(
    val date: Long,
    val dayName: String,
    val condition: String,
    val highTemp: Int,
    val lowTemp: Int,
    val humidity: Int,
    val chanceOfRain: Int
)