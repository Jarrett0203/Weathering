package com.example.weathering

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weathering.databinding.ActivityMainBinding
import com.example.weathering.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, MainFragment())
            .addToBackStack(null)
            .commit()
    }
}