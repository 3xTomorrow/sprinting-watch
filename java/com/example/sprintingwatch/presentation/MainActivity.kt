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

        //Creates variables for the button
        val startButton: Button = findViewById(R.id.start_button)
        val resetButton: Button = findViewById(R.id.reset_button)
        val pauseButton: Button = findViewById(R.id.pause_button)

        //Creates variables for the texts
        val reachGoalText: TextView = findViewById(R.id.reachedGoalText)
        val rssiText: TextView = findViewById(R.id.rssiText)
        val elapsedTimeText: TextView = findViewById(R.id.elapsedTimeText)

        //Event Loop Variables
        val handler = Handler(Looper.getMainLooper())
        lateinit var runnable: Runnable

        //WiFi Variables
        var wifiInfo: WifiInfo? = null

        //Other Variables
        var gettingRSSI: Boolean = false
        var reachedFinishLine: Boolean = false
        var paused: Boolean = true
        var elapsedTime: Double = 0.0

        //Button Functions that change whether the program should be gathering RSSI or not.
        startButton.setOnClickListener {
            gettingRSSI = true
            paused = false
        }
        pauseButton.setOnClickListener {
            gettingRSSI = false
            paused = true
        }



        runnable = Runnable {
            if (gettingRSSI) {

                elapsedTimeText.text = String.format("%.2f", elapsedTime)

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

                //Logs for RSSI if needed
//                if(rssi != 10)
//                    Log.d("WifiInfo", "RSSI: $rssi")
//                else
//                    Log.d("WifiInfo", "No Wi-Fi connection")

                if(rssi != 10)
                    rssiText.text = "$rssi dBm"
                else
                    rssiText.text = "No Wi-Fi signal"

                reachedFinishLine = rssi > -30

                if(reachedFinishLine) {
                    reachGoalText.text = "Yes"
                    paused = true
                } else {
                    reachGoalText.text = "No"
                    paused = false
                }

            } else {
                rssiText.text = "Not getting RSSI"
            }

            if(!paused) {
                elapsedTime += 0.01
            }

            //rssiText.text = "$rssi dBm"
            handler.postDelayed(runnable, 10)
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