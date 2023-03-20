package com.example.weathering.ui.main

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weathering.databinding.FragmentMainBinding
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainFragment : Fragment() {

    private val CITY: String = "Singapore"
    private val API: String = "5a5953b54684ec61924a024558f995b6"
    private val PREFS_NAME = "weather_prefs"

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val dateFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
    private val timeFormat: SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    private lateinit var mainViewModel: MainViewModel
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedResponse: String? = sharedPreferences.getString("response", null)

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        binding.progressBar.visibility = View.VISIBLE
        binding.mainContainer.visibility = View.GONE
        binding.errorText.visibility = View.GONE
        executor.execute {
            val response: String? = try {
                URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API")
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
                parseAndDisplayResponse(response)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun parseAndDisplayResponse(response: String?) {
        try {
            dateFormat.timeZone = TimeZone.getTimeZone(CITY)
            timeFormat.timeZone = TimeZone.getTimeZone(CITY)
            val jsonObj = JSONObject(response!!)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val wind = jsonObj.getJSONObject("wind")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
            val updatedTime = Date(jsonObj.getLong("dt")*1000)
            val updatedTimeText = String.format("Updated at %s",
                dateFormat.format(updatedTime))
            val temp = String.format("%s°C", main.getString("temp"))
            val tempMin = String.format("Low Temp: %s°C", main.getString("temp_min"))
            val tempMax = String.format("High Temp: %s°C", main.getString("temp_max"))
            val pressure = String.format("%s hPa", main.getString("pressure"))
            val humidity = String.format("%s%%", main.getString("humidity"))
            val sunrise:Long = sys.getLong("sunrise") * 1000
            val sunset:Long = sys.getLong("sunset") * 1000
            val windSpeed = String.format("%sm/s", wind.getString("speed"))
            val weatherDescription = weather.getString("description")
            val location = String.format("%s, %s", jsonObj.getString("name"), sys.getString("country"))

            binding.currentLocation.text = location
            binding.updatedTime.text = updatedTimeText
            binding.status.text = weatherDescription.uppercase()
            binding.currentTemp.text = temp
            binding.tempMin.text = tempMin
            binding.tempMax.text = tempMax
            binding.sunrise.text = timeFormat.format(sunrise)
            binding.sunset.text = timeFormat.format(sunset)
            binding.wind.text = windSpeed
            binding.pressure.text = pressure
            binding.humidity.text = humidity

            binding.progressBar.visibility = View.GONE
            binding.mainContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.errorText.visibility = View.VISIBLE
        }
    }

}