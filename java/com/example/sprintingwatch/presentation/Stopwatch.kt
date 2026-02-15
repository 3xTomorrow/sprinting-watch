package com.example.sprintingwatch.presentation

import android.os.SystemClock
import kotlinx.coroutines.*

class Stopwatch {
    private var startTime = 0L
    private var pausedTime = 0L
    private var isRunning = false
    private var stopwatchJob: Job?  = null

    public fun start(onTick: (millis: Long) -> Unit) {
        if(!isRunning) {
            startTime = SystemClock.elapsedRealtime() - pausedTime
            isRunning = true

            stopwatchJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive && isRunning) {
                    val elapsed = SystemClock.elapsedRealtime() - startTime
                    onTick(elapsed)
                    delay(10)
                }
            }
        }
    }

    fun pause() {
        if(isRunning) {
            pausedTime = SystemClock.elapsedRealtime() - startTime
            isRunning = false
            stopwatchJob?.cancel()
        }
    }

    fun reset() {
        pause()
        startTime = 0L
        pausedTime = 0L


    }
}