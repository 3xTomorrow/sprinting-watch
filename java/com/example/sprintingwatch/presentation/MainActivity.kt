package com.example.sprintingwatch.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var isScanning = false

    private val deviceList = ArrayList<BluetoothDevice>()
    private var finishLineBeaconRSSI: Int? = null

    //Buttons
    private lateinit var beginButton: Button
    private lateinit var startButton: Button
    private lateinit var resetButton: Button
    private lateinit var pauseButton: Button
    private lateinit var scanButton: Button

    //Text
    private lateinit var reachGoalText: TextView
    private lateinit var rssiText: TextView
    private lateinit var elapsedTimeText: TextView

    var reachedFinishLine: Boolean = false

    // BLE Scan Callback
    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("SetTextI18n")
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val deviceName = device.name ?: "Unknown"

            // Check if this is the Finish Line Beacon
            if (deviceName == "Finish Line Beacon") {
                finishLineBeaconRSSI = result.rssi
                Log.d("BLE_RSSI", "Finish Line Beacon RSSI: $finishLineBeaconRSSI")

                // Update UI on main thread
                runOnUiThread {
                    rssiText.text = "$finishLineBeaconRSSI dBm"

                    finishLineBeaconRSSI?.let {
                        if(it > -50) {
                            reachGoalText.text = "You crossed the finish line!"
                            reachedFinishLine = true
                        }
                    }

                }
            }

            // Avoid duplicates in device list
            if (!deviceList.contains(device)) {
                deviceList.add(device)
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
                reachGoalText.text = "Scan failed with error: $errorCode"
                rssiText.text = "Scan Failed"
                isScanning = false
                startButton.isEnabled = true
            }
        }
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ])
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.title_screen)

        //Creates variables for the button
        beginButton = findViewById(R.id.begin_button)

        beginButton.setOnClickListener {
            setContentView(R.layout.main_layout)

            startButton = findViewById(R.id.start_button)
            resetButton = findViewById(R.id.reset_button)
            pauseButton = findViewById(R.id.pause_button)

            //Creates variables for the texts
            reachGoalText = findViewById(R.id.reachGoalText)
            rssiText = findViewById(R.id.rssiText)
            elapsedTimeText = findViewById(R.id.elapsedTimeText)

            //Event Loop Variables
            val handler = Handler(Looper.getMainLooper())
            lateinit var runnable: Runnable

            //Other Variables
            var gettingRSSI: Boolean = false
            var paused: Boolean = true
            var elapsedTime: Double = 0.0

            // Initialize Bluetooth
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            bleScanner = bluetoothAdapter?.bluetoothLeScanner

            //Button Functions
            startButton.setOnClickListener {
                if(!gettingRSSI) {
                    gettingRSSI = true
                    paused = false
                    startBleScan()
                }
            }

            pauseButton.setOnClickListener {
                gettingRSSI = false
                paused = true
                stopBleScan()
            }

            resetButton.setOnClickListener {
                if(paused) {
                    elapsedTime = 0.0
                    runOnUiThread {
                        elapsedTimeText.text = "$elapsedTime"
                        rssiText.text = "Not scanning"
                        reachGoalText.text = "Sprint Timer"
                    }
                }
            }

            runnable = Runnable {
                if (gettingRSSI) {
                    //Changes the time displayed and formats it
                    elapsedTimeText.text = String.format("%.2f", elapsedTime)

                    if(reachedFinishLine) {
                        stopBleScan()
                        gettingRSSI = false
                        paused = true
                        reachedFinishLine = false
                    }
                }

                //Increases the time if the user is not paused
                if(!paused) {
                    elapsedTime += 0.01
                }

                handler.postDelayed(runnable, 10)
            }
            handler.post(runnable)
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

        // Clear previous RSSI value
        finishLineBeaconRSSI = null

        isScanning = true
        startButton.text = "Scanning..."
        rssiText.text = "Searching..."

        // Scan settings for continuous scanning
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0L) // Report immediately
            .build()

        // Filter for Finish Line Beacon
        val scanFilters = ScanFilter.Builder()
            .setDeviceName("Finish Line Beacon")
            .build()

        val filters = listOf(scanFilters)

        bleScanner?.startScan(filters, scanSettings, leScanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopBleScan() {
        if (!isScanning) return

        isScanning = false
        startButton.text = "Start"
        bleScanner?.stopScan(leScanCallback)
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
}



// ---------- EXTRA (unneeded) CODE --------------
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

