package com.example.nbacktaskapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nbacktaskapp.ui.theme.NBackTaskAppTheme
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.random.Random

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val accelerometerData = StringBuilder()
    private val random = Random
    private var nBackNumber = 0
    private val sequenceLength = 20 // Number of trials in the n-back task

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            NBackTaskAppTheme {
                NBackTaskScreen(
                    onStartTask = { startNBackTask() },
                    onEndTask = { endNBackTask() }
                )
            }
        }
    }

    private fun startNBackTask() {
        nBackNumber = random.nextInt(10) // Random number for n-back task
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        // Simulate the n-back task sequence (for simplicity, just a delay)
        Thread {
            try {
                Thread.sleep(5000) // Wait for 5 seconds (simulate task duration)
                runOnUiThread {
                    endNBackTask()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun endNBackTask() {
        sensorManager.unregisterListener(this)
        // Save accelerometer data to storage
        saveAccelerometerData()
    }

    private fun saveAccelerometerData() {
        val fileName = "accelerometer_data.txt"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            FileWriter(file).use { writer ->
                writer.append(accelerometerData.toString())
                Log.d("NBackTaskApp", "Accelerometer data saved to ${file.absolutePath}")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            accelerometerData.append("x: $x, y: $y, z: $z\n")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this example
    }
}

@Composable
fun NBackTaskScreen(onStartTask: () -> Unit, onEndTask: () -> Unit) {
    var isTaskRunning by remember { mutableStateOf(false) }
    var response by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("Press Start to begin the n-back task") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = instructions,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (isTaskRunning) {
            TextField(
                value = response,
                onValueChange = { response = it },
                label = { Text("Your response") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onEndTask()
                    isTaskRunning = false
                    instructions = "Task completed. Your response: $response"
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("End")
            }
        } else {
            Button(
                onClick = {
                    onStartTask()
                    isTaskRunning = true
                    instructions = "Remember this number: ${Random.nextInt(10)}"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NBackTaskScreenPreview() {
    NBackTaskAppTheme {
        NBackTaskScreen(
            onStartTask = {},
            onEndTask = {}
        )
    }
}