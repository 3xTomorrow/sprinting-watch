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

        val handler = Handler(Looper.getMainLooper())
        lateinit var runnable: Runnable


        //Logs to test wifi
//        Log.d("WifiInfo", "RSSI: $rssi dBm")
//        Log.d("WifiName", "SSID: $ssid, Frequency: $frequency MHz, Link Speed: $linkSpeed")


        var wifiInfo: WifiInfo? = null



        runnable = Runnable {

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

//class HandlerTimer {
//    private val handler = Handler(Looper.getMainLooper())
//    private lateinit var runnable: Runnable
//    private var isPaused = false
//    private var time = 0
//
//    @SuppressLint("SetTextI18n")
//    fun start(text: TextView) {
//        runnable = Runnable {
//            if(!isPaused) {
//                text.text = "$time"
//                time++
//            }
//            handler.postDelayed(runnable, 1000)
//        }
//        handler.post(runnable )
//    }
//
//    fun pause() {
//        isPaused = true
//    }
//
//    fun resume() {
//        isPaused = false
//    }
//
//    fun reset() {
//        handler.removeCallbacks(runnable)
//        time = 0
//        isPaused = false
//    }
//
//}