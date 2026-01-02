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
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcelable
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var isScanning = false

    private val deviceList = ArrayList<BluetoothDevice>()
    private val deviceNameList = ArrayList<String>()
    private lateinit var arrayAdapter: ArrayAdapter<String>

    private lateinit var btnScan: Button
    private lateinit var lvDevices: ListView
    private lateinit var tvStatus: TextView

    // BLE Scan Callback
    private val leScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device

            // Avoid duplicates
            if (!deviceList.contains(device)) {
                deviceList.add(device)
                val deviceInfo = "${device.name ?: "Unknown"}\n${device.address}\nRSSI: ${result.rssi}"
                deviceNameList.add(deviceInfo)

                runOnUiThread {
                    arrayAdapter.notifyDataSetChanged()
                    tvStatus.text = "Found ${deviceList.size} BLE device(s)"
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                onScanResult(SCAN_FAILED_ALREADY_STARTED, result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            runOnUiThread {
                tvStatus.text = "Scan failed with error: $errorCode"
                isScanning = false
                btnScan.isEnabled = true
            }
        }
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId", "ServiceCast")
    @RequiresPermission(allOf= [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onCreate(savedInstanceState: Bundle?) {
//        installSplashScreen()
//
//        super.onCreate(savedInstanceState)
//
//        setContentView(R.layout.title_screen)
//
//        //Creates variables for the button
//        val beginButton: Button = findViewById(R.id.begin_button)
//
//        //Function that requests user for these permissions (Find devices, connect to devices, and location)
//        fun requestBluetoothPermissions() {
//            val permissions = arrayOf(
//                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            )
//            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
//        }
//
//        //Checks if the permissions have already been granted
//        fun hasBluetoothPermissions(): Boolean {
//            return run {
//                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
//                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
//                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
//            }
//        }
//
//        //Function that combines the the previous ones for permissions (Used before bluetooth is granted)
//        fun startBluetoothDiscovery() {
//            if(!hasBluetoothPermissions()) {
//                requestBluetoothPermissions()
//                return
//            }
//        }
//
//        beginButton.setOnClickListener {
//            startBluetoothDiscovery()
//
//            setContentView(R.layout.main_layout)
//
//            val startButton: Button = findViewById(R.id.start_button)
//            val resetButton: Button = findViewById(R.id.reset_button)
//            val pauseButton: Button = findViewById(R.id.pause_button)
//
//            //Creates variables for the texts
//            val reachGoalText: TextView = findViewById(R.id.reachedGoalText)
//            val rssiText: TextView = findViewById(R.id.rssiText)
//            val elapsedTimeText: TextView = findViewById(R.id.elapsedTimeText)
//
//            //Event Loop Variables
//            val handler = Handler(Looper.getMainLooper())
//            lateinit var runnable: Runnable
//
//            //WiFi Variables
//            var wifiInfo: WifiInfo? = null
//
//            //Other Variables
//            var gettingRSSI: Boolean = false
//            var reachedFinishLine: Boolean = false
//            var paused: Boolean = true
//            var elapsedTime: Double = 0.0
//
//            //Button Functions that change whether the program should be gathering RSSI or not.
//            startButton.setOnClickListener {
//                gettingRSSI = true
//                paused = false
//
//                Log.d("Bluetooth", BluetoothAdapter.ACTION_DISCOVERY_STARTED); //Logs that device started looking for bluetooth devices
//            }
//            pauseButton.setOnClickListener {
//                gettingRSSI = false
//                paused = true
//
//                Log.d("Bluetooth", BluetoothAdapter.ACTION_DISCOVERY_FINISHED) //Logs that device stopped looking for bluetooth devices
//            }
//            resetButton.setOnClickListener {
//                if(paused) {
//                    elapsedTime = 0.0
//                    runOnUiThread { elapsedTimeText.text = "$elapsedTime" }
//                }
//            }
//
//
//
//
//            runnable = Runnable {
//                if (gettingRSSI) {
//
//                    //Changes the time displayed and formats it
//                    elapsedTimeText.text = String.format("%.2f", elapsedTime)
//
//
//                    //Creates the adapter on the watch
//                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//
//                    //Creates a filter for any Bluetooth devices that is found
//                    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//                    //Runs the receiver function when the filter device is found
//                    registerReceiver(receiver, filter)
//
//                    //If the bluetooth adapter is enabled, start looking for other bluetooth devices
//                    if(bluetoothAdapter?.isEnabled == true) {
//                        bluetoothAdapter.startDiscovery()
//                    }
//
//
//                    val rssi: Int = 0
//
//
//                    //Logs for RSSI if needed
////                if(rssi != 10)
////                    Log.d("WifiInfo", "RSSI: $rssi")
////                else
////                    Log.d("WifiInfo", "No Wi-Fi connection")
//
//                    //Changes the display on the screen depending on if there is a connection
//                    if(rssi != 10)
//                        rssiText.text = "$rssi dBm"
//                    else
//                        rssiText.text = "No Wi-Fi signal"
//
//                    //Variable for seeing if crossed finish line
//                    reachedFinishLine = rssi > -20
//
//                    //Conditional for crossing finish line
//                    if(reachedFinishLine) {
//                        reachGoalText.text = "Yes"
//                        paused = true
//                    } else {
//                        reachGoalText.text = "No"
//                        paused = false
//                    }
//
//                } else {
//                    //Displayed when user isn't getting the RSSI
//                    rssiText.text = "Not getting RSSI"
//                }
//
//                //Increases the time if the user is not paused
//                if(!paused) {
//                    elapsedTime += .01
//                }
//
//                //rssiText.text = "$rssi dBm"
//                handler.postDelayed(runnable, 1000)
//            }
//            handler.post(runnable)
//        }
//
//

        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_devices)

        btnScan = findViewById(R.id.btnScan)
        lvDevices = findViewById(R.id.lvDevices)
        tvStatus = findViewById(R.id.tvStatus)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter?.bluetoothLeScanner

        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNameList)
        lvDevices.adapter = arrayAdapter

        lvDevices.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            Toast.makeText(this, "Selected: ${device.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()
        }

        btnScan.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startBleScan() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear previous results
        deviceList.clear()
        deviceNameList.clear()
        arrayAdapter.notifyDataSetChanged()

        isScanning = true
        btnScan.text = "Stop Scan"
        tvStatus.text = "Scanning for BLE devices..."

        // Optional: Add scan filters
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanFilters = ScanFilter.Builder()
            .setDeviceName("Finish Line Beacon")
            .build()

        val filters = listOf(scanFilters)

        bleScanner?.startScan(filters, scanSettings, leScanCallback)

        // Auto-stop after 10 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (isScanning) {
                stopBleScan()
            }
        }, 10000)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopBleScan() {
        if (!isScanning) return

        isScanning = false
        btnScan.text = "Scan for BLE Devices"
        btnScan.isEnabled = true

        bleScanner?.stopScan(leScanCallback)

        if (deviceList.isEmpty()) {
            tvStatus.text = "No BLE devices found"
        } else {
            tvStatus.text = "Found ${deviceList.size} BLE device(s)"
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBleScan()
            } else {
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()
        stopBleScan()
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
