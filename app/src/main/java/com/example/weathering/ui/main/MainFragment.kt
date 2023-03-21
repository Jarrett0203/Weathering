package com.example.weathering.ui.main

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weathering.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("dd/MM/yyyy hh:mm a z", Locale.ENGLISH)
    private val timeFormat: SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    private lateinit var mainViewModel: MainViewModel

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                Toast.makeText(
                    requireContext(),
                    "Permission was granted successfully.",
                    Toast.LENGTH_SHORT
                ).show()
                Toast.makeText(
                    requireContext(),
                    "Please refresh to view current location weather.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Weathering needs location permission to function properly!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[MainViewModel::class.java]

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val root: View = binding.root

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        getCurrentLocation()

        binding.swipeRefresh.setOnRefreshListener {
            getCurrentLocation()
            binding.swipeRefresh.isRefreshing = false
        }

        mainViewModel.weatherData.observe(viewLifecycleOwner) {
            parseAndDisplayResponse(it)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getCurrentLocation() {
        if (!checkPermission()) {
            requestPermission()
            return
        }
        if (!isLocationEnabled()) {
            Toast.makeText(
                requireContext(),
                "The app requires location to run properly.",
                Toast.LENGTH_SHORT
            ).show()
        }
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val location: Location? = it.result

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                mainViewModel.getCurrentWeather(lat, lon)
            } else {
                mainViewModel.getSavedResponseNoInternet()
            }
        }
    }

    private fun parseAndDisplayResponse(response: String?) {
        try {
            val jsonObj = JSONObject(response!!)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val wind = jsonObj.getJSONObject("wind")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
            val timeZone = jsonObj.getInt("timezone")
            var gmt = "GMT"
            if (timeZone > 0) {
                gmt += "+"
            }
            val timeZoneFormat = gmt + timeZone / 3600
            dateFormat.timeZone = TimeZone.getTimeZone(timeZoneFormat)
            timeFormat.timeZone = TimeZone.getTimeZone(timeZoneFormat)
            val updatedTime = Date(jsonObj.getLong("dt") * 1000)
            val updatedTimeText = String.format(
                "Updated at %s",
                dateFormat.format(updatedTime)
            )
            val temp = String.format("%s°C", main.getString("temp"))
            val tempMin = String.format("Low Temp: %s°C", main.getString("temp_min"))
            val tempMax = String.format("High Temp: %s°C", main.getString("temp_max"))
            val pressure = String.format("%s hPa", main.getString("pressure"))
            val humidity = String.format("%s%%", main.getString("humidity"))
            val sunrise: Long = sys.getLong("sunrise") * 1000
            val sunset: Long = sys.getLong("sunset") * 1000
            val windSpeed = String.format("%sm/s", wind.getString("speed"))
            val weatherDescription = weather.getString("description")
            val location =
                String.format("%s, %s", jsonObj.getString("name"), sys.getString("country"))

            binding.currentLocation.text = location
            binding.updatedTime.text = updatedTimeText
            binding.status.text = weatherDescription.uppercase()
            binding.currentTemp.text = temp
            binding.tempMin.text = tempMin
            binding.tempMax.text = tempMax
            binding.sunrise.text = timeFormat.format(Date(sunrise))
            binding.sunset.text = timeFormat.format(Date(sunset))
            binding.wind.text = windSpeed
            binding.pressure.text = pressure
            binding.humidity.text = humidity

            binding.progressBar.visibility = View.GONE
            binding.mainContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            binding.mainContainer.visibility = View.GONE
            binding.errorText.visibility = View.VISIBLE
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}