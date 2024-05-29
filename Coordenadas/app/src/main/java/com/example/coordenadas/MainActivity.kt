package com.example.coordenadas

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.example.coordenadas.databinding.ActivityMainBinding
import android.Manifest
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGeo.setOnClickListener {
            getLocation()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }
        val locationProvider : FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationProvider.lastLocation.addOnSuccessListener {
            location: Location? ->
                location?.let {
                    val latitude = it.latitude
                    val longitude = it.longitude
                    binding.txtGeo.text = "Cood:\n"+latitude.toString() +"\n"+longitude.toString()
                }
        }
    }
}