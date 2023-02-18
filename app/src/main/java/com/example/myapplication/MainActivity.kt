package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.webkit.URLUtil
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.journeyapps.barcodescanner.CaptureActivity
import okhttp3.*
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val requestCodeForQRCode = 2
    private val requestCodeForPermission = 3
    private lateinit var deviceIDText: TextView
    private lateinit var longitudeText: TextView
    private lateinit var latitudeText: TextView
    private lateinit var thingsboardURL: TextView
    private lateinit var httpResponse: TextView
    private lateinit var locationManager: LocationManager

    data class ThingsBoardData (
        var lat: Double = 0.0,
        var long: Double = 0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // render layouts
        setContentView(R.layout.activity_main)

//        val btnGetGPS = findViewById<Button>(R.id.buttonGPS)
//        val btnHttpPost = findViewById<Button>(R.id.buttonPost)
        val btnScan = findViewById<Button>(R.id.buttonScan)

        longitudeText = findViewById(R.id.longitude)
        latitudeText = findViewById(R.id.latitude)
        deviceIDText = findViewById(R.id.deviceidvalue)
        thingsboardURL = findViewById(R.id.thingsboardURL)
        httpResponse = findViewById(R.id.httpResponse)

        val pref = getSharedPreferences("data", MODE_PRIVATE)
        mapOf(
            thingsboardURL to "thingsboardURL",
            deviceIDText to "deviceID",
            latitudeText to "latitude",
            longitudeText to "longitude"
        ).forEach { (s, p) ->
            s.text = pref.getString("Text${s.id}", p)
        }


        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // request permissions
        val permissionList = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (permissionList.any {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            requestPermissions(permissionList, requestCodeForPermission)
        }

        // get location service
        locationManager =
            applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager

//        // set buttons
//        btnGetGPS.setOnClickListener {
//            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//                // GPS provider is enabled, try getting the location
//                Toast.makeText(this, "GPS enabled", Toast.LENGTH_SHORT).show()
//                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//                location?.let {
//                    longitudeText.text = location.longitude.toString()
//                    latitudeText.text = location.latitude.toString()
//                }
//            } else {
//                // GPS provider is not enabled, prompt user to enable it
//                Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show()
//            }
//        }

        btnScan.setOnClickListener {
            // scan qr code
            val intent = Intent(applicationContext, CaptureActivity::class.java)
            intent.action = "com.google.zxing.client.android.SCAN"
            mapOf(
                "SAVE_HISTORY" to false,
                "SCAN_MODE" to "QR_CODE_MODE",
                "PROMPT_MESSAGE" to "Scan QR code for IoT",
                "BEEP_ENABLED" to false
            ).forEach { (s, p) ->
                intent.putExtra(s, p)
            }

            // get gps location
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // GPS provider is enabled, try getting the location
                // Toast.makeText(this, "GPS enabled", Toast.LENGTH_SHORT).show()
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                location?.let {
                    longitudeText.text = location.longitude.toString()
                    latitudeText.text = location.latitude.toString()
                    // Toast.makeText(this,  location.longitude.toString(), Toast.LENGTH_SHORT).show()
                }
            } else {
                // GPS provider is not enabled, prompt user to enable it
                Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show()
            }

            startActivityForResult(intent, requestCodeForQRCode)
        }

//        btnHttpPost.setOnClickListener {
//            val payload = Gson().toJson(ThingsBoardData(
//                latitudeText.text.toString().toDoubleOrNull() ?: 0.0,
//                longitudeText.text.toString().toDoubleOrNull() ?: 0.0
//            ))
//            httpResponse.text = "posting: $payload"
//            val rawURL1 = "http://frontgate.tplinkdns.com:8080/api/v1/"
//            val rawURL2 = "/attributes"
//            val tokenID = deviceIDText.text.toString()
//            val postURL = rawURL1 + tokenID +rawURL2
//            // val postURL = thingsboardURL.text.toString()
//            if (!URLUtil.isValidUrl(postURL)) {
//                httpResponse.text = "invalid URL"
//                return@setOnClickListener
//            }
//            val okHttpClient = OkHttpClient()
//            val requestBody: RequestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload)
//            val request = Request.Builder()
//                .method("POST", requestBody)
//                .url(postURL)
//                .addHeader("Content-Type", "application/json")
//                .build()
//            okHttpClient.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    runOnUiThread {
//                        httpResponse.text = e.message
//                    }
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    runOnUiThread {
//                        httpResponse.text = response.message() + ' ' + response.body()?.string()
//                    }
//                }
//            })
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) when (requestCode) {
            requestCodeForQRCode -> {
                    deviceIDText.text = data?.getStringExtra("SCAN_RESULT")
            }
        }


        // http post
        val payload = Gson().toJson(ThingsBoardData(
            latitudeText.text.toString().toDoubleOrNull() ?: 0.0,
            longitudeText.text.toString().toDoubleOrNull() ?: 0.0
        ))
        httpResponse.text = "posting: $payload"
        val rawURL1 = "http://frontgate.tplinkdns.com:8080/api/v1/"
        val rawURL2 = "/attributes"
        val tokenID = deviceIDText.text.toString()
        val postURL = rawURL1 + tokenID +rawURL2

        val okHttpClient = OkHttpClient()
        val requestBody: RequestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload)
        val request = Request.Builder()
            .method("POST", requestBody)
            .url(postURL)
            .addHeader("Content-Type", "application/json")
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    httpResponse.text = e.message
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    httpResponse.text = response.message() + ' ' + response.body()?.string()
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        val editor = getSharedPreferences("data", MODE_PRIVATE).edit()
        arrayOf(thingsboardURL, deviceIDText, latitudeText, longitudeText). forEach {
            editor.putString("Text${it.id}", it.text.toString())
        }
        editor.apply()
    }

}