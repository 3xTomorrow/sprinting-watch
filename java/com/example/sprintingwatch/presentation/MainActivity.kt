package com.example.sprintingwatch.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.test.isEnabled
import com.example.sprintingwatch.R
import com.example.sprintingwatch.presentation.theme.SprintingWatchTheme
import kotlin.random.Random
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresPermission
import org.w3c.dom.Text
import java.util.Timer
import java.util.TimerTask


class MainActivity : ComponentActivity() {

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_layout)

        //Creates variables for the button and the RSSI
        val startButton: Button = findViewById<Button>(R.id.button)
        val resetButton: Button = findViewById<Button>(R.id.button2)
        val rssiText: TextView = findViewById<TextView>(R.id.rssiText)

//       startButton.setOnClickListener {
//           runOnUiThread {
//               var wifiInfo: WifiInfo? = null
//
//               val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//               val activeNetwork = connectivityManager.activeNetwork
//               val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
//
//               if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//                   wifiInfo = networkCapabilities.transportInfo as? WifiInfo
//               }
//
//               val rssi: Int = wifiInfo?.rssi ?: Log.d("ERROR", "Can't find RSSI")  // in dBm
//               val frequency: Int = wifiInfo?.frequency ?: Log.d("ERROR", "Can't find Frequency")
//               val ssid = wifiInfo?.ssid ?: Log.d("ERROR", "Can't find SSID")
//               val linkSpeed: Int = wifiInfo?.linkSpeed ?: Log.d("ERROR", "Can't find Link Speed")
//
//
//               //Logs to test wifi
//               Log.d("WifiInfo", "RSSI: $rssi dBm")
//               Log.d("WifiName", "SSID: $ssid, Frequency: $frequency MHz, Link Speed: $linkSpeed")
//
//
//               rssiText.text = "$rssi dBm"
//           }
//       }


        val timer = Timer()

        startButton.setOnClickListener {
            val beginningTime: Long = System.currentTimeMillis() /1000

            val task = object : TimerTask() {
                override fun run() {

                    val endTime: Long = System.currentTimeMillis() / 1000
                    runOnUiThread {
                        val timeElapsed: Long = endTime - beginningTime
                        rssiText.text = "$timeElapsed"
                    }
                }
            }

            timer.schedule(task, 0, 1000)
        }

        resetButton.setOnClickListener {
            timer.cancel()
        }





    }
}