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

import kotlin.math.*

//IMPORTS SENSOR API
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

//IMPORTS VIBRATOR API
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.VibratorManager

class MainActivity : ComponentActivity() {

    ///BLE Variables
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

    //TIMER AND RSSI VARIABLES
    var gettingRSSI: Boolean = false
    var paused: Boolean = true
    var elapsedTime: Double = 0.0

    //OTHER
    var reachedFinishLine: Boolean = false
    var stopwatch: Stopwatch = Stopwatch()

    //SENSOR VARIABLES
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var startedRace: Boolean = false
    private var sensorThreshold: Float = 3.0f

    //VIBRATOR VARIABLES
    private lateinit var vibratorManager: VibratorManager
    private var vibrator: Vibrator? = null
    private var buttonVibration: VibrationEffect? = null
    private var finishVibration: VibrationEffect? = null


    //EVENT FUNCTIONS

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
                        if(it > -66) { //RSSI to reach finish line
                            vibrator?.vibrate(finishVibration)
                            reachGoalText.text = "Finish!"
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

    //Sensor Event
    private val accelerometerListener = object : SensorEventListener {
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = sqrt(x*x + y*y + z*z)
            val motion = abs(magnitude - SensorManager.GRAVITY_EARTH)

            if(motion > sensorThreshold) {
                if(!gettingRSSI) {
                    Log.d("LOG", "RACE STARTED!!!!")
                    stopwatch.start { millis ->
                        val seconds = millis / 1000
                        val milliseconds = millis % 1000
                        elapsedTimeText.text = String.format("%02d.%03d", seconds, milliseconds)
                    }
                    gettingRSSI = true
                    paused = false
                    startBleScan()
                }

                //Stops listening once detected
                sensorManager.unregisterListener(this)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
    }


    //RUNNING APP FUNCTION
    @SuppressLint("SetTextI18n", "MissingInflatedId")
    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.VIBRATE
    ])
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.title_screen)

        setupVibrator()

        //Creates variables for the button
        beginButton = findViewById(R.id.begin_button)

        beginButton.setOnClickListener {
            setContentView(R.layout.main_layout)
            vibrator?.vibrate(buttonVibration)

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

            // Initialize Bluetooth
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            bleScanner = bluetoothAdapter?.bluetoothLeScanner

            //Initialize Accelerometer
            setupSensors()
            accelerometer?.let {
                sensorManager.registerListener(
                    accelerometerListener,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

            pauseButton.setOnClickListener {
                vibrator?.vibrate(buttonVibration)
                stopwatch.pause()
                gettingRSSI = false
                paused = true
                stopBleScan()
            }

            resetButton.setOnClickListener {
                if(paused) {
                    vibrator?.vibrate(buttonVibration)
                    stopwatch.reset()
                    accelerometer?.let {
                        sensorManager.registerListener(
                            accelerometerListener,
                            it,
                            SensorManager.SENSOR_DELAY_NORMAL
                        )
                    }
                    elapsedTime = 0.00
                    runOnUiThread {
                        elapsedTimeText.text = "$elapsedTime"
                        rssiText.text = "Not scanning"
                        reachGoalText.text = "Sprint Timer"
                    }
                }
            }

            runnable = Runnable {
                if (gettingRSSI) {

                    if(reachedFinishLine) {
                        stopwatch.pause()
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

    //Function that sets up the sensors for the device
    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            Toast.makeText(this, "Your device doesn't support the accelerometer", Toast.LENGTH_SHORT).show()
            Log.e("SENSOR ERROR", "Accelerometer not detected")
        }

    }

    //Function that sets up the vibrators for the device
    private fun setupVibrator() {
        vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vibratorManager.defaultVibrator
        buttonVibration = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        finishVibration = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
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
        //startButton.text = "S\nC\nA\nN\nN\nI\nN\nG\n.\n.\n."
        //rssiText.text = "Searching..."

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
        startButton.text = "S\nT\nA\nR\nT"
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
        sensorManager.unregisterListener(accelerometerListener)
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

