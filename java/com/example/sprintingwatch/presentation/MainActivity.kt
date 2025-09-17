package com.example.sprintingwatch.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.sprintingwatch.R
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Runnable
import kotlin.arrayOf

enum class TimerStates {
    INITIAL,
    RUNNING,
    PAUSED,
    CANCELLED
}

class MainActivity : ComponentActivity() {


    @SuppressLint("SetTextI18n", "MissingInflatedId", "ServiceCast")
    @RequiresPermission(allOf= [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_layout)

        //Creates variables for the button and the RSSI
        val startButton: Button = findViewById<Button>(R.id.start_button)
        val resetButton: Button = findViewById<Button>(R.id.reset_button)
        val pauseButton: Button = findViewById<Button>(R.id.pause_button)
        val rssiText: TextView = findViewById<TextView>(R.id.rssiText)

        //Event Loop Variables
        val handler = Handler(Looper.getMainLooper())
        lateinit var runnable: Runnable

        //WiFi Variables
        var wifiInfo: WifiInfo? = null

        //Other Variables
        var gettingRSSI: Boolean = false

        //Button Functions that change whether the program should be gathering RSSI or not.
        startButton.setOnClickListener { gettingRSSI = true }
        pauseButton.setOnClickListener { gettingRSSI = false }


        runnable = Runnable {
            if (gettingRSSI) {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

                if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                }

                val rssi: Int = wifiInfo?.rssi ?: 10  // in dBm (10 for null value rather than -1 because -1 is a possible value of RSSI while 10 is not)
                val frequency: Int = wifiInfo?.frequency ?: -1
                val ssid = wifiInfo?.ssid ?: "Unknown"
                val linkSpeed: Int = wifiInfo?.linkSpeed ?: -1

                if(rssi != 10)
                    Log.d("WifiInfo", "RSSI: $rssi")
                else
                    Log.d("WifiInfo", "No Wi-Fi connection")

                if(rssi != 10)
                    rssiText.text = "$rssi"
                else
                    rssiText.text = "No Wi-Fi signal"

            } else {
                rssiText.text = "Not getting RSSI"
        }

            //rssiText.text = "$rssi dBm"
            handler.postDelayed(runnable, 1000)
        }
        handler.post(runnable)



//        var states: TimerStates = TimerStates.INITIAL
//        val timer = HandlerTimer()
//
//        startButton.setOnClickListener {
//            if (states == TimerStates.INITIAL || states == TimerStates.CANCELLED) {
//                timer.start(rssiText)
//                states = TimerStates.RUNNING
//            } else if (states == TimerStates.PAUSED) {
//                timer.resume()
//                states = TimerStates.RUNNING
//            }
//        }
//
//        pauseButton.setOnClickListener {
//            if (states == TimerStates.RUNNING) {
//                timer.pause()
//                states = TimerStates.PAUSED
//            }
//        }
//
//        resetButton.setOnClickListener {
//            if (states == TimerStates.RUNNING || states == TimerStates.PAUSED) {
//                rssiText.text = "0"
//                timer.reset()
//                states = TimerStates.CANCELLED
//            }
//        }
    }
}