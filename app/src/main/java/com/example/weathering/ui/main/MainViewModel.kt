package com.example.weathering.ui.main

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.weathering.API_KEY
import com.example.weathering.City
import com.example.weathering.WeatherData
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val OPENWEATHERAPI: String = API_KEY.openWeather
    private val PREFS_NAME = "weather_prefs"
    private val CITIES = arrayListOf(
        City("New York, US", 40.7128, -74.0060),
        City("Singapore, SG", 1.3521, 103.8198),
        City("Mumbai, IN", 19.0760, 72.8777),
        City("Delhi, IN", 28.7041, 77.1025),
        City("Sydney, AU", -33.8688, 151.2093),
        City("Melbourne, AU", -37.8136, 144.9631)
    )
    private lateinit var sharedPreferences: SharedPreferences

    var weatherListMutableLiveData = MutableLiveData<List<WeatherData>?>()
    val currentLocationWeatherData = MutableLiveData<WeatherData?>()

    val weatherList: LiveData<List<WeatherData>?>
        get() {
            return try {
                loadWeatherData()
                weatherListMutableLiveData
            } catch (e: Exception) {
                weatherListMutableLiveData = MutableLiveData(emptyList())
                weatherListMutableLiveData
            }
        }

    fun loadWeatherData() {
        val newWeatherList = ArrayList<WeatherData>()
        val weatherMap = LinkedHashMap<City, WeatherData>()

        sharedPreferences =
            getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val executor = Executors.newFixedThreadPool(CITIES.size)
        val completionService = ExecutorCompletionService<WeatherData>(executor)

        for (city in CITIES) {
            val savedResponse: String? = sharedPreferences.getString(city.name, null)

            completionService.submit {
                val response: String? = try {
                    URL("https://api.openweathermap.org/data/2.5/weather?q=${city.name}&units=metric&appid=$OPENWEATHERAPI")
                        .readText(Charsets.UTF_8)
                } catch (e: Exception) {
                    savedResponse
                }

                try {
                    with(sharedPreferences.edit()) {
                        putString(city.name, response)
                        putLong("last_updated", System.currentTimeMillis())
                        apply()
                    }
                } catch (e: Exception) {
                    throw Exception("No internet connection and no cached data")
                }

                parseResponse(response)
            }
        }

        for (i in 1..CITIES.size) {
            val future = completionService.take()
            val city = CITIES[i - 1]
            val weatherData = future.get()
            weatherMap[city] = weatherData
        }

        executor.shutdown()

        for (city in CITIES) {
            val weatherData = weatherMap[city] ?: continue
            newWeatherList.add(weatherData)
        }

        weatherListMutableLiveData.value = newWeatherList.sortedWith(compareBy { it.name })
    }


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
                currentLocationWeatherData.value = parseResponse(response)
            }
        }
    }

    fun getSavedResponseNoLocation() {
        sharedPreferences =
            getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedResponse: String? = sharedPreferences.getString("response", null)
        if (savedResponse != null) {
            currentLocationWeatherData.value = parseResponse(savedResponse)
        }
    }

    private fun parseResponse(response: String?): WeatherData {
        val jsonObj = JSONObject(response!!)
        val main = jsonObj.getJSONObject("main")
        val sys = jsonObj.getJSONObject("sys")
        val wind = jsonObj.getJSONObject("wind")
        val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
        val name = jsonObj.getString("name")
        val country = sys.getString("country")
        val timeZone = jsonObj.getInt("timezone")
        val updatedTime = jsonObj.getLong("dt") * 1000
        val temp = main.getString("temp")
        val tempMin = main.getString("temp_min")
        val tempMax = main.getString("temp_max")
        val pressure = main.getString("pressure")
        val humidity = main.getString("humidity")
        val sunrise = sys.getLong("sunrise") * 1000
        val sunset = sys.getLong("sunset") * 1000
        val windSpeed = wind.getString("speed")
        val weatherDescription = weather.getString("description")
        return WeatherData(
            name,
            country,
            timeZone,
            updatedTime,
            temp,
            tempMin,
            tempMax,
            pressure,
            humidity,
            sunrise,
            sunset,
            windSpeed,
            weatherDescription
        )
    }
}