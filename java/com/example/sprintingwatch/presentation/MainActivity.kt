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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.arrayOf


class MainActivity : ComponentActivity() {


    @SuppressLint("SetTextI18n", "MissingInflatedId", "ServiceCast")
    @RequiresPermission(allOf= [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.title_screen)

        //Creates variables for the button
        val beginButton: Button = findViewById(R.id.begin_button)

        beginButton.setOnClickListener {
            setContentView(R.layout.main_layout)

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
            resetButton.setOnClickListener {
                if(paused) {
                    elapsedTime = 0.0
                    runOnUiThread { elapsedTimeText.text = "$elapsedTime" }
                }
            }

            // Bluetooth


            runnable = Runnable {
                if (gettingRSSI) {

                    //Changes the time displayed and formats it
                    elapsedTimeText.text = String.format("%.2f", elapsedTime)

                    //Uses connectivity manager API to gather the WiFi information and use for the RSSI
                    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val activeNetwork = connectivityManager.activeNetwork
                    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

                    if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                    }


                    // WiFi info data
                    val rssi: Int = wifiInfo?.rssi ?: 10  // in dBm (10 for null value rather than -1 because -1 is a possible value of RSSI while 10 is not)
                    val frequency: Int = wifiInfo?.frequency ?: -1
                    val ssid = wifiInfo?.ssid ?: "Unknown"
                    val linkSpeed: Int = wifiInfo?.linkSpeed ?: -1


                    //Logs for RSSI if needed
//                if(rssi != 10)
//                    Log.d("WifiInfo", "RSSI: $rssi")
//                else
//                    Log.d("WifiInfo", "No Wi-Fi connection")

                    //Changes the display on the screen depending on if there is a connection
                    if(rssi != 10)
                        rssiText.text = "$rssi dBm"
                    else
                        rssiText.text = "No Wi-Fi signal"

                    //Variable for seeing if crossed finish line
                    reachedFinishLine = rssi > -20

                    //Conditional for crossing finish line
                    if(reachedFinishLine) {
                        reachGoalText.text = "Yes"
                        paused = true
                    } else {
                        reachGoalText.text = "No"
                        paused = false
                    }

                } else {
                    //Displayed when user isn't getting the RSSI
                    rssiText.text = "Not getting RSSI"
                }

                //Increases the time if the user is not paused
                if(!paused) {
                    elapsedTime += .01
                }

                //rssiText.text = "$rssi dBm"
                handler.postDelayed(runnable, 10)
            }
            handler.post(runnable)
        }



    }

    fun rssiParse(cmdOutput: String): String {
        val rssiStringIndex: Int = cmdOutput.indexOf("RSSI: ")
        val cmdParsed: String = cmdOutput.substring(rssiStringIndex+6, rssiStringIndex+9)
        return cmdParsed
    }

    //Stores the code for gathering RSSI with SuperUser commands
    fun commandRSSI() {
        //My own command which uses a super-user shell console
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd wifi status"))

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        //val stderr = process.errorStream.bufferedReader().use { it.readText() }
        //val exitCode = process.waitFor()

        val rssiString: String = rssiParse(stdout)
        Log.d("RSSI", rssiString)

        val rssi: Int = rssiString.toInt()
    }
}
