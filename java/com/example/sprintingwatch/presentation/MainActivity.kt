package com.example.sprintingwatch.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.sprintingwatch.R
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Runnable


enum class TimerStates {
    INITIAL,
    RUNNING,
    PAUSED,
    CANCELLED
}

class MainActivity : ComponentActivity() {

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_layout)

        //Creates variables for the button and the RSSI
        val startButton: Button = findViewById<Button>(R.id.start_button)
        val resetButton: Button = findViewById<Button>(R.id.reset_button)
        val pauseButton: Button = findViewById<Button>(R.id.pause_button)
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

        var states: TimerStates = TimerStates.INITIAL
        val timer = HandlerTimer()

        startButton.setOnClickListener {
            if (states == TimerStates.INITIAL || states == TimerStates.CANCELLED) {
                timer.start(rssiText)
                states = TimerStates.RUNNING
            } else if (states == TimerStates.PAUSED) {
                timer.resume()
                states = TimerStates.RUNNING
            }
        }

        pauseButton.setOnClickListener {
            if (states == TimerStates.RUNNING) {
                timer.pause()
                states = TimerStates.PAUSED
            }
        }

        resetButton.setOnClickListener {
            if(states == TimerStates.RUNNING || states == TimerStates.PAUSED) {
                rssiText.text = "0"
                timer.reset()
                states = TimerStates.CANCELLED
            }
        }
    }
}

class HandlerTimer {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isPaused = false
    private var time = 0

    @SuppressLint("SetTextI18n")
    fun start(text: TextView) {
        runnable = Runnable {
            if(!isPaused) {
                text.text = "$time"
                time++
            }
            handler.postDelayed(runnable, 1000)
        }
        handler.post(runnable )
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun reset() {
        handler.removeCallbacks(runnable)
        time = 0
        isPaused = false
    }

}