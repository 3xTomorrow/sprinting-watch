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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {


    @SuppressLint("SetTextI18n", "MissingInflatedId", "ServiceCast")
    @RequiresPermission(allOf= [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.title_screen)

        //Creates variables for the button
        val beginButton: Button = findViewById(R.id.begin_button)

        val PERMISSION_REQUEST_CODE = 1

        //Function that requests user for these permissions (Find devices, connect to devices, and location)
        fun requestBluetoothPermissions() {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }

        //Checks if the permissions have already been granted
        fun hasBluetoothPermissions(): Boolean {
            return run {
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            }
        }

        //Function that combines the the previous ones for permissions (Used before bluetooth is granted)
        fun startBluetoothDiscovery() {
            if(!hasBluetoothPermissions()) {
                requestBluetoothPermissions()
                return
            }
        }

        beginButton.setOnClickListener {
            startBluetoothDiscovery()

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

                Log.d("Bluetooth", BluetoothAdapter.ACTION_DISCOVERY_STARTED); //Logs that device started looking for bluetooth devices
            }
            pauseButton.setOnClickListener {
                gettingRSSI = false
                paused = true

                Log.d("Bluetooth", BluetoothAdapter.ACTION_DISCOVERY_FINISHED) //Logs that device stopped looking for bluetooth devices
            }
            resetButton.setOnClickListener {
                if(paused) {
                    elapsedTime = 0.0
                    runOnUiThread { elapsedTimeText.text = "$elapsedTime" }
                }
            }




            runnable = Runnable {
                if (gettingRSSI) {

                    //Changes the time displayed and formats it
                    elapsedTimeText.text = String.format("%.2f", elapsedTime)


                    //Creates the adapter on the watch
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

                    //Creates a filter for any Bluetooth devices that is found
                    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    //Runs the receiver function when the filter device is found
                    registerReceiver(receiver, filter)

                    //If the bluetooth adapter is enabled, start looking for other bluetooth devices
                    if(bluetoothAdapter?.isEnabled == true) {
                        bluetoothAdapter.startDiscovery()
                    }


                    val rssi: Int = 0


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
                handler.postDelayed(runnable, 1000)
            }
            handler.post(runnable)
        }



    }

    //Receiver function that is used when a bluetooth device is found
    private val receiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device?.name
                val deviceAddress = device?.address
                Log.d("Bluetooth", "Found device: $deviceName - $deviceAddress")
            }
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
