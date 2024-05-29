package com.example.api

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.api.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var client : OkHttpClient
    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        client = OkHttpClient()

        binding.btnGet.setOnClickListener {
            fetchLocations()
        }
    }

    private fun fetchLocations() {
        val request: Request = Request.Builder().url("https://www.slmm.com.br/pdm/api/locations").get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread{
                    Toast.makeText(this@MainActivity, "Falha no get", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body!!.string()


                val jsonArray = JSONArray(body)
                val locationList = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val location = jsonArray.getJSONObject(i)

                    var ra = location.getString("ra")
                    var ra2 = location.getString("ra2")
                    var qrCode = location.getString("qrCode")
                    var latitude = location.getString("latitude")
                    var longitude = location.getString("longitude")
                    val createdAt = location.getString("createdAt")

                    locationList.add("RA $ra  RA: $ra2  \nQrCode: $qrCode \nLat: $latitude  Long: $longitude \nCreated: $createdAt")
                }
                runOnUiThread{
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, locationList)
                    binding.listView.adapter = adapter
                }
            }
        })
    }
}