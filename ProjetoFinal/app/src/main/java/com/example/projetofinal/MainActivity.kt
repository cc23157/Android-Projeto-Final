// Sofia Tasselli Kawamura - 23157

package com.example.projetofinal

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.projetofinal.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var integrator: IntentIntegrator
    private lateinit var client : OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarBotoes()
    }

    private fun inicializarBotoes() {
        // configurações para leitura do QR code
        integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a Qr code")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(true)
        integrator.setOrientationLocked(false)

        binding.btnQrCode.setOnClickListener {
            integrator.initiateScan()
        }

        client = OkHttpClient()
        binding.btnListar.setOnClickListener {
            listarAPI()
        }
    }

    // faz requisição get e lista os dados da API
    private fun listarAPI() {
        val request: Request = Request.Builder().url("https://www.slmm.com.br/pdm/api/pdm").get().build()

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

                    val ra = location.getString("ra")
                    val latitude = location.getString("latitude")
                    val longitude = location.getString("longitude")

                    locationList.add("RA: $ra\nLatitude: $latitude\nLongitude: $longitude")
                }
                runOnUiThread{
                    val adapter = ArrayAdapter(this@MainActivity, com.example.projetofinal.R.layout.list_item, locationList)
                    binding.listDados.adapter = adapter // exibe dados obtidos da API no listView
                }
            }
        })
    }

    // leitura do QR Code
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelado!", Toast.LENGTH_LONG).show()
            }
            else {
                val qrCodeString = result.contents.toString().substring(0, 5)  // RA obtido do QR Code
                binding.txtRA.text = qrCodeString

                // depois de ler o QR Code, vai obter a localização e tentar fazer o POST
                try {
                    getLocation {   // espera obter a localização
                        try {
                            post()  // envia os dados para a API
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Falha ao enviar dados: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Erro ao obter localização: ${e.message}", Toast.LENGTH_LONG).show()
                }

            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // obtém a localização
    private fun getLocation(onLocationRetrieved: () -> Unit) {
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
                binding.txtLatitude.text = latitude.toString()
                binding.txtLongitude.text = longitude.toString()
                onLocationRetrieved() // faz com que o método espere a localização ser obtida para prosseguir
            }
        }
    }

    // envia os dados para a API
    private fun post() {
        // pega os dados já obtidos
        val ra = binding.txtRA.text.toString()
        val latitude = binding.txtLatitude.text.toString().toFloat()
        val longitude = binding.txtLongitude.text.toString().toFloat()

        val json = JSONObject()
        json.put("ra", ra)
        json.put("latitude", latitude)
        json.put("longitude", longitude)

        // faz a requisição post
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request: Request = Request.Builder()
            .url("https://www.slmm.com.br/pdm/api/pdm")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Falha ao enviar dados: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful){
                    val body = response.body!!.string()
                    val jsonResponse = JSONObject(body)

                    val status = jsonResponse.getString("status")
                    runOnUiThread {
                        if (status == "ok") {
                            Toast.makeText(this@MainActivity, "Dados enviados com sucesso!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Falha ao enviar dados", Toast.LENGTH_LONG).show()
                        }
                    }

                }
            }
        })

    }

}