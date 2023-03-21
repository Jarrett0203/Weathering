package com.example.weathering.ui.main

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.weathering.API_KEY
import java.net.URL
import java.util.concurrent.Executors

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val OPENWEATHERAPI: String = API_KEY.openWeather
    private val PREFS_NAME = "weather_prefs"
    private lateinit var sharedPreferences: SharedPreferences

    val weatherData = MutableLiveData<String?>()
    fun getCurrentWeather(lat: Double, lon: Double) {
        sharedPreferences =
            getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedResponse: String? = sharedPreferences.getString("response", null)

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val response: String? = try {
                URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$OPENWEATHERAPI")
                    .readText(Charsets.UTF_8)
            } catch (e: Exception) {
                savedResponse
            }

            if (response != null) {
                with(sharedPreferences.edit()) {
                    putString("response", response)
                    putLong("last_updated", System.currentTimeMillis())
                    apply()
                }
            }
            handler.post {
                weatherData.value = response
            }
        }
    }

    fun getSavedResponseNoInternet() {
        sharedPreferences =  getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedResponse: String? = sharedPreferences.getString("response", null)
        if (savedResponse != null) {
            weatherData.value = savedResponse
        }
    }
}