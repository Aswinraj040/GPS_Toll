package com.aswin.gps_toll

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.cache.DiskLruCache
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var textView: TextView
    private var ipAddressFinal = ""
    private val client = OkHttpClient()
    private lateinit var progressDialog: ProgressDialog
    private val locationRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.button)
        stopButton = findViewById(R.id.stop)
        textView = findViewById(R.id.textView)
        try {
            val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            if (sharedPreferences.contains("Vehicleno")) {
            } else {
                findViewById<EditText>(R.id.editTextTextPersonName).visibility = View.VISIBLE
                findViewById<Button>(R.id.button2).visibility = View.VISIBLE
                findViewById<TextView>(R.id.textView2).visibility = View.GONE
                stopButton.visibility = View.GONE
                textView.visibility = View.GONE
                startButton.visibility = View.GONE
            }
        }catch (e : Exception){
            Toast.makeText(this, "Error ", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.button2).setOnClickListener {
            // Get the input from the EditText
            val vehicleNo = findViewById<EditText>(R.id.editTextTextPersonName).text.toString()
            // Save the input to SharedPreferences
            val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("Vehicleno", vehicleNo)
            editor.apply()

            // Optionally, show a message to the user
            Toast.makeText(this, "Vehicle number saved", Toast.LENGTH_SHORT).show()
            findViewById<EditText>(R.id.editTextTextPersonName).visibility = View.GONE
            findViewById<Button>(R.id.button2).visibility = View.GONE
            findViewById<TextView>(R.id.textView2).visibility = View.VISIBLE
            stopButton.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            startButton.visibility = View.VISIBLE
        }
        startButton.setOnClickListener {
            progressDialog = ProgressDialog(this@MainActivity).apply {
                setMessage("Scanning subnet, please wait...")
                setCancelable(false)
                show()
            }

            CoroutineScope(Dispatchers.IO).launch {
                val localIp = getLocalIPAddress()
                if (localIp != null) {
                    val subnet = localIp.substringBeforeLast(".")
                    val discoveredIp = scanSubnet(subnet)
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        if (discoveredIp != null) {
                            textView.text = "Server IP Address: $discoveredIp"
                            ipAddressFinal = discoveredIp
                            saveServerIpAddress(discoveredIp)
                            startLocationService(discoveredIp)
                        } else {
                            textView.text = "No server found."
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        textView.text = "Failed to get local IP address."
                    }
                }
            }
        }

        stopButton.setOnClickListener {
            stopLocationService()
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationRequestCode)
        }
    }
    private fun startLocationService(ipAddress: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                try {
                    val sharedPreferences: SharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                    val vehicleNo = sharedPreferences.getString("Vehicleno", "")
                    val url = "http://$ipAddress:5005/vehicle_command?Vehicleno=$vehicleNo"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    response.isSuccessful && responseBody == "Success"
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            if (result) {
                val serviceIntent = Intent(this@MainActivity, LocationService::class.java).apply {
                    putExtra("ipAddress", ipAddress)
                }
                ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
            } else {

            }
        }
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }

    private suspend fun getLocalIPAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val inetAddress = addresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress) {
                        val ipAddress = inetAddress.hostAddress
                        if (ipAddress.indexOf(':') < 0) {
                            Log.d("Tag", ipAddress)
                            return ipAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private suspend fun scanSubnet(subnet: String): String? {
        val maxConcurrentRequests = 150
        val dispatcher = Dispatchers.IO.limitedParallelism(maxConcurrentRequests)

        return withContext(dispatcher) {
            val jobs = (1..254).map { i ->
                async {
                    val ipAddress = "$subnet.$i"
                    Log.d("Tag", ipAddress)
                    if (isServerResponding(ipAddress)) {
                        Log.d("MainActivity", "Found server at: $ipAddress")
                        ipAddress
                    } else {
                        null
                    }
                }
            }
            jobs.awaitAll().firstOrNull { it != null }
        }
    }

    private suspend fun isServerResponding(ip: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Construct the URL with Vehicleno as a query parameter
                val url = "http://$ip:5005/testing_command"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                response.isSuccessful && responseBody == "Success"
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun saveServerIpAddress(ipAddress: String) {
        val sharedPref = getSharedPreferences("com.aswin.gps_toll.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("SERVER_IP_ADDRESS", ipAddress)
            apply()
        }
    }
}
