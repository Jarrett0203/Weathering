package com.example.weathering.ui.main

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weathering.WeatherData
import com.example.weathering.WeatherListAdapter
import com.example.weathering.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
        getWeatherData()
        if (isInternetConnected()) {
            mainViewModel.loadWeatherData()
        }

        binding.sampleCountries.layoutManager = LinearLayoutManager(context)
        binding.sampleCountries.adapter = WeatherListAdapter(requireContext(), emptyList())

        binding.swipeRefresh.setOnRefreshListener {
            if (isInternetConnected()) {
                mainViewModel.loadWeatherData()
            }
            getWeatherData()
            binding.swipeRefresh.isRefreshing = false
        }

        mainViewModel.currentLocationWeatherData.observe(viewLifecycleOwner) {
            displayResponse(it)
        }

        mainViewModel.weatherList.observe(viewLifecycleOwner) { weatherList ->
            weatherList?.let { list ->
                Log.d(TAG, "Weather list updated: ${list.size}")
                if (list.isNotEmpty()) {
                    binding.sampleError.visibility = View.GONE
                    binding.sampleCountries.visibility = View.VISIBLE
                    binding.sampleCountries.adapter = WeatherListAdapter(requireContext(), list)
                } else {
                    binding.sampleError.visibility = View.VISIBLE
                    binding.sampleCountries.visibility = View.GONE
                }
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getWeatherData() {
        if (!checkPermission()) {
            requestPermission()
            return
        }

        if (!isInternetConnected()) {
            Toast.makeText(
                requireContext(),
                "Could not connect to the internet.",
                Toast.LENGTH_SHORT
            ).show()
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
                mainViewModel.getSavedResponseNoLocation()
            }
        }
    }

    private fun displayResponse(weatherData: WeatherData?) {
        try {
            var gmt = "GMT"
            if (weatherData!!.timeZone!! > 0) {
                gmt += "+"
            }
            val timeZoneFormat = gmt + weatherData.timeZone!! / 3600
            dateFormat.timeZone = TimeZone.getTimeZone(timeZoneFormat)
            timeFormat.timeZone = TimeZone.getTimeZone(timeZoneFormat)
            val updatedTimeText = String.format(
                "Updated at %s",
                dateFormat.format(Date(weatherData.updatedTime!!))
            )
            val temp = String.format("%s°C", weatherData.temp)
            val tempMin = String.format("Low Temp: %s°C", weatherData.tempMin)
            val tempMax = String.format("High Temp: %s°C", weatherData.tempMax)
            val pressure = String.format("%s hPa", weatherData.pressure)
            val humidity = String.format("%s%%", weatherData.humidity)
            val sunrise = timeFormat.format(Date(weatherData.sunrise!!))
            val sunset = timeFormat.format(Date(weatherData.sunset!!))
            val windSpeed = String.format("%sm/s", weatherData.wind)
            val weatherDescription = weatherData.weatherDescription!!.uppercase()
            val location = String.format("%s, %s", weatherData.name, weatherData.country)

            binding.currentLocation.text = location
            binding.updatedTime.text = updatedTimeText
            binding.status.text = weatherDescription
            binding.currentTemp.text = temp
            binding.tempMin.text = tempMin
            binding.tempMax.text = tempMax
            binding.sunrise.text = sunrise
            binding.sunset.text = sunset
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

    private fun isInternetConnected(): Boolean {
        val connectivityManager =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}