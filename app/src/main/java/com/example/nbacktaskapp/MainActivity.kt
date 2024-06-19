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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nbacktaskapp.ui.theme.NBackTaskAppTheme
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.random.Random
import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast


class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val accelerometerData = StringBuilder()
    private val random = Random
    private var nBackNumber = 0
    private val sequenceLength = 30
    private lateinit var nBackSequence: List<Int>
    private var currentIndex = 0
    private var matchCount = 0
    private var currentTaskIndex = 0
    private lateinit var taskLevels: MutableList<Int>
    private val reactionTimeData = StringBuilder()
    private var numberDisplayedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Initialize and shuffle task levels
        taskLevels = mutableListOf(1, 2, 3)
        taskLevels.shuffle()

        setContent {
            NBackTaskAppTheme {
                MainScreen(onStartTask = { startNBackTask() })
            }
        }
    }

    private fun startNBackTask() {
        nBackNumber = taskLevels[currentTaskIndex]
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        generateNBackSequence()
        matchCount = 0
        currentIndex = 0
        displayNextNumber()
    }

    private fun nextNBackTask() {
        setContent {
            NBackTaskAppTheme {
                RatingScreen(currentTask = taskLevels[currentTaskIndex], onSubmitRating = { rating ->
                    saveRating(applicationContext, taskLevels[currentTaskIndex], rating)
                    currentTaskIndex = (currentTaskIndex + 1) % taskLevels.size
                    startNBackTask()
                })
            }
        }
    }

    private fun handleMatchPress() {
        val reactionTime = System.currentTimeMillis() - numberDisplayedTime
        reactionTimeData.append("Reaction Time: $reactionTime ms\n")

        if (currentIndex >= nBackNumber && nBackSequence[currentIndex] == nBackSequence[currentIndex - nBackNumber]) {
            matchCount++
        }
    }

    private fun endNBackTask() {
        sensorManager.unregisterListener(this)
        saveAccelerometerData()
        saveReactionTimeData(this)
        val accuracy = if (sequenceLength > 0) {
            val calculatedAccuracy = (matchCount.toDouble() / sequenceLength) * 100
            if (calculatedAccuracy > 100.0) 100.0 else calculatedAccuracy
        } else {
            0.0
        }
        saveAccuracyData(this, accuracy)

        setContent {
            NBackTaskAppTheme {
                NBackTaskScreen(
                    onStartTask = { startNBackTask() },
                    onNextTask = { nextNBackTask() },
                    onMatchPress = { handleMatchPress() },
                    currentNumber = null,
                    isTaskRunning = false,
                    showNumber = false,
                    showAccuracy = true,
                    accuracy = accuracy,
                    currentTask = taskLevels[currentTaskIndex]
                )
            }
        }
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

    private fun saveReactionTimeData(context: Context) {
        val fileName = "reaction_time_data.txt"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")  // Use text/plain MIME type for .txt files
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val resolver = context.contentResolver
        val existingUri = getExistingFileUri(resolver, fileName)

        val uri = existingUri ?: resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        try {
            uri?.let {
                resolver.openOutputStream(it, if (existingUri == null) "wa" else "wa").use { outputStream ->
                    if (outputStream != null) {
                        outputStream.write(reactionTimeData.toString().toByteArray())
                        Log.d("NBackTaskApp", "Reaction time data saved to ${uri.path}")
                    } else {
                        Log.e("NBackTaskApp", "Failed to open output stream for $uri")
                        Toast.makeText(context, "Failed to save reaction time data", Toast.LENGTH_LONG).show()
                    }
                }
            } ?: run {
                Log.e("NBackTaskApp", "Failed to create file")
                Toast.makeText(context, "Failed to create file for reaction time data", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Log.e("NBackTaskApp", "IOException while saving reaction time data", e)
            Toast.makeText(context, "Error saving reaction time data", Toast.LENGTH_LONG).show()
        }
    }


    private fun saveAccuracyData(context: Context, accuracy: Double) {
        val fileName = "accuracy_data.csv"
        val csvHeader = "Accuracy (%)\n"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val resolver = context.contentResolver
        val existingUri = getExistingFileUri(resolver, fileName)

        val uri = existingUri ?: resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        try {
            uri?.let {
                resolver.openOutputStream(it, if (existingUri == null) "wa" else "wa").use { outputStream ->
                    if (outputStream != null) {
                        if (existingUri == null || fileIsEmpty(context, it)) {
                            // Write the header if the file is new or empty
                            outputStream.write(csvHeader.toByteArray())
                        }
                        // Write the accuracy under the header
                        outputStream.write("${"%.2f".format(accuracy)}%\n".toByteArray())
                        Log.d("NBackTaskApp", "Accuracy data saved to ${uri.path}")
                    } else {
                        Log.e("NBackTaskApp", "Failed to open output stream for $uri")
                        Toast.makeText(context, "Failed to save accuracy data", Toast.LENGTH_LONG).show()
                    }
                }
            } ?: run {
                Log.e("NBackTaskApp", "Failed to create file")
                Toast.makeText(context, "Failed to create file for accuracy data", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Log.e("NBackTaskApp", "IOException while saving accuracy data", e)
            Toast.makeText(context, "Error saving accuracy data", Toast.LENGTH_LONG).show()
        }
    }

    private fun getExistingFileUri(resolver: android.content.ContentResolver, fileName: String): android.net.Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
        val selectionArgs = arrayOf(fileName)
        val queryUri = MediaStore.Files.getContentUri("external")

        resolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return android.net.Uri.withAppendedPath(queryUri, id.toString())
            }
        }
        return null
    }

    private fun fileIsEmpty(context: Context, uri: android.net.Uri): Boolean {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { inputStream ->
            return inputStream.available() == 0
        }
        return false
    }


    private fun generateNBackSequence() {
        val targetNumbers = mutableListOf<Int>()
        var matchCount = 0
        val requiredMatches = 15  // Ensure 15 matches for each n-back task

        while (matchCount < requiredMatches) {
            // Generate 4 to 7 random numbers before each match
            val randomCount = random.nextInt(4, 8)
            for (i in 1..randomCount) {
                targetNumbers.add(random.nextInt(10))
            }

            // Ensure the list is long enough to insert a match
            if (targetNumbers.size >= nBackNumber) {
                // Add a match by repeating a number from nBackNumber positions earlier
                targetNumbers.add(targetNumbers[targetNumbers.size - nBackNumber])
                matchCount++
            }
        }

        // After all matches are added, add a final set of random numbers
        val finalRandomCount = random.nextInt(4, 8)
        for (i in 1..finalRandomCount) {
            targetNumbers.add(random.nextInt(10))
        }

        nBackSequence = targetNumbers
        Log.d("NBackTaskApp", "Generated n-back sequence: $nBackSequence")
    }

    private fun displayNextNumber() {
        if (currentIndex >= nBackSequence.size) {
            endNBackTask()
            return
        }

        numberDisplayedTime = System.currentTimeMillis()  // Set the display time

        runOnUiThread {
            setContent {
                NBackTaskAppTheme {
                    NBackTaskScreen(
                        onStartTask = { startNBackTask() },
                        onNextTask = { nextNBackTask() },
                        onMatchPress = { handleMatchPress() },
                        currentNumber = nBackSequence[currentIndex],
                        isTaskRunning = true,
                        showNumber = true,
                        showAccuracy = false,
                        accuracy = 0.0,
                        currentTask = taskLevels[currentTaskIndex]
                    )
                }
            }
        }




        Thread {
            try {
                Thread.sleep(1000L)
                runOnUiThread {
                    setContent {
                        NBackTaskAppTheme {
                            NBackTaskScreen(
                                onStartTask = { startNBackTask() },
                                onNextTask = { nextNBackTask() },
                                onMatchPress = { handleMatchPress() },
                                currentNumber = null,
                                isTaskRunning = true,
                                showNumber = false,
                                showAccuracy = false,
                                accuracy = 0.0,
                                currentTask = taskLevels[currentTaskIndex]
                            )
                        }
                    }
                }
                Thread.sleep(2000L)
                currentIndex++
                displayNextNumber()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
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
fun MainScreen(onStartTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Press Start to begin the n-back task",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onStartTask,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start")
        }
    }
}

@Composable
fun NBackTaskScreen(
    onStartTask: () -> Unit,
    onNextTask: () -> Unit,
    onMatchPress: () -> Unit,
    currentNumber: Int?,
    isTaskRunning: Boolean,
    showNumber: Boolean,
    showAccuracy: Boolean,
    accuracy: Double,
    currentTask: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$currentTask-back task",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (showAccuracy) {
            Text(
                text = "Task completed. Your accuracy: ${"%.2f".format(accuracy)}%",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onNextTask,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Next")
            }
        } else {
            if (isTaskRunning) {
                if (showNumber && currentNumber != null) {
                    Text(
                        text = currentNumber.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(72.dp)) // Placeholder for the number
                }
                Button(
                    onClick = onMatchPress,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Match")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NBackTaskAppTheme {
        MainScreen(onStartTask = {})
    }
}

@Preview(showBackground = true)
@Composable
fun NBackTaskScreenPreview() {
    NBackTaskAppTheme {
        NBackTaskScreen(
            onStartTask = {},
            onNextTask = {},
            onMatchPress = {},
            currentNumber = 5,
            isTaskRunning = true,
            showNumber = true,
            showAccuracy = false,
            accuracy = 0.0,
            currentTask = 1
        )
    }
}