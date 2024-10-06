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
import java.io.IOException
import kotlin.random.Random
import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.*

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
    private var showTutorial by mutableStateOf(true)
    private var tutorialExampleRunning by mutableStateOf(false)
    private var tutorialIndex by mutableStateOf(0)
    private val tutorialSequences = mapOf(
        1 to listOf(1, 2, 2, 3),  // 1-back: Match between second and third element (2)
        2 to listOf(2, 3, 2, 4, 5),  // 2-back: Match between first and third element (2)
        3 to listOf(3, 1, 4, 3, 2, 6)  // 3-back: Match between first and fourth element (3)
    )
    private var isMatchButtonEnabled by mutableStateOf(true) // Control match button availability
    private var feedbackMessage by mutableStateOf("")
    private var feedbackColor by mutableStateOf(Color.Green)
    private var currentTutorialLevel by mutableStateOf(1) // Start with 1-back tutorial

    // Predefined sequences for participants
    private val predefinedSequences = listOf(
        listOf(1, 2, 3),
        listOf(3, 2, 1),
        listOf(2, 3, 1),
        listOf(1, 3, 2),
        listOf(2, 1, 3),
        listOf(3, 1, 2)
    )
    // Set participant index (should be dynamic based on the participant)
    private val participantIndex = 0  // This can be updated based on the current participant

    // Coroutine job to manage the running task
    private var taskJob: Job? = null

    private fun startupScreen() {
        setContent {
            NBackTaskAppTheme {
                if (showTutorial) {
                    if (tutorialExampleRunning) {
                        TutorialExampleScreen(
                            currentNumber = tutorialSequences[currentTutorialLevel]?.get(tutorialIndex) ?: 0,
                            onNext = {
                                tutorialIndex++
                                if (tutorialIndex >= (tutorialSequences[currentTutorialLevel]?.size ?: 0)) {
                                    tutorialExampleRunning = false
                                    tutorialIndex = 0
                                }
                            },
                            tutorialNBackNumber = currentTutorialLevel, // Dynamically update based on the tutorial level
                            sequence = tutorialSequences[currentTutorialLevel] ?: listOf(),
                            index = tutorialIndex,
                            onCorrectAnswer = {
                                feedbackMessage = "Correct! You have made the right choice."
                                feedbackColor = Color.Green
                            },
                            onRestartTutorial = {
                                tutorialExampleRunning = true
                                tutorialIndex = 0
                            },
                            onStartTask = { startNBackTask() },
                            onFinishTutorial = {
                                showTutorial = false
                                setContent {
                                    MainScreen(
                                        onStartTask = { startNBackTask() },
                                        onRestartTutorial = {
                                            showTutorial = true
                                            tutorialExampleRunning = false
                                            tutorialIndex = 0
                                            currentTutorialLevel = 1
                                            startupScreen()
                                        }
                                    )
                                }
                            },
                            onExitTask = { exitToStartScreen() },
                            currentTutorialLevel = currentTutorialLevel, // Track the current tutorial level
                            onNextLevel = { // Move to the next tutorial level
                                currentTutorialLevel++
                                tutorialExampleRunning = true
                                tutorialIndex = 0
                            }
                        )
                    } else {
                        TutorialScreen(
                            onStartTutorial = {
                                tutorialExampleRunning = true
                            },
                            onSkipTutorial = {
                                showTutorial = false
                                startNBackTask()
                            },
                            onExitTask = { exitToStartScreen() }
                        )
                    }
                } else {
                    MainScreen(
                        onStartTask = { startNBackTask() },
                        onRestartTutorial = {
                            showTutorial = true
                            tutorialExampleRunning = false
                            tutorialIndex = 0
                            currentTutorialLevel = 1
                            startupScreen()
                        }
                    )
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSensors()

        // Use the predefined sequence for the current participant
        taskLevels = predefinedSequences[participantIndex].toMutableList()

        startupScreen()
    }

    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun startNBackTask() {
        feedbackMessage = ""
        nBackNumber = taskLevels[currentTaskIndex]
        accelerometerData.clear() // Clear previous accelerometer data
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        generateNBackSequence()
        matchCount = 0
        currentIndex = 0

        // Show the countdown before the task starts
        setContent {
            NBackTaskAppTheme {
                CountdownScreen(countdownTime = 3) {
                    // After countdown ends, start the actual task
                    taskJob = CoroutineScope(Dispatchers.Main).launch {
                        displayNextNumber()
                    }
                }
            }
        }
    }

    private fun exitToStartScreen() {
        // Cancel the current task if it's running
        taskJob?.cancel() // This ensures the coroutine running the task is stopped.
        taskJob = null

        accelerometerData.clear()
        setupSensors()

        // Reset taskLevels to the first predefined sequence: 1, 2, 3
        taskLevels = predefinedSequences[0].toMutableList()

        // Reset other parameters to initial state
        showTutorial = true
        tutorialExampleRunning = false
        tutorialIndex = 0
        currentTutorialLevel = 1
        startupScreen() // Go back to the initial screen.
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
        // Disable the match button to prevent multiple presses
        isMatchButtonEnabled = false

        // Check if the current number matches the one n-back positions earlier
        val isMatch = currentIndex >= nBackNumber && nBackSequence[currentIndex] == nBackSequence[currentIndex - nBackNumber]

        // Calculate reaction time and provide feedback
        val reactionTime = System.currentTimeMillis() - numberDisplayedTime
        reactionTimeData.append("Reaction Time: $reactionTime ms\n")

        if (isMatch) {
            matchCount++
            feedbackMessage = "Correct! This number matches the one from $nBackNumber steps earlier."
            feedbackColor = Color.Green
        } else {
            feedbackMessage = "Incorrect. Try to remember the sequence better."
            feedbackColor = Color.Red
        }

        // Update the UI to show feedback
        setContent {
            NBackTaskAppTheme {
                NBackTaskScreen(
                    onStartTask = { startNBackTask() },
                    onNextTask = { nextNBackTask() },
                    onMatchPress = { handleMatchPress() },
                    currentNumber = nBackSequence[currentIndex],
                    isTaskRunning = true,
                    showNumber = true,
                    showFeedback = true,
                    feedbackMessage = feedbackMessage,
                    feedbackColor = feedbackColor,
                    showAccuracy = false,
                    accuracy = 0.0,
                    currentTask = taskLevels[currentTaskIndex],
                    isMatchButtonEnabled = isMatchButtonEnabled,
                    onExitTask = { exitToStartScreen() }
                )
            }
        }

        // Cancel the current task and show feedback
        taskJob?.cancel()  // Cancel any existing task coroutine
        taskJob = null

        // Show feedback for 1 second, then proceed
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000L) // Pause for 1 second to display feedback

            feedbackMessage = "" // Clear feedback message
            currentIndex++  // Proceed to the next number in the sequence

            // Resume the task by displaying the next number
            taskJob = launch {
                displayNextNumber() // Show the next number in the sequence
            }
        }
    }


    private fun endNBackTask() {
        sensorManager.unregisterListener(this)
        saveAccelerometerData(this)
        saveReactionTimeData(this)
        val totalTargets = 15
        val accuracy = if (totalTargets > 0) {
            val calculatedAccuracy = (matchCount.toDouble() / totalTargets) * 100
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
                    showFeedback = false,
                    feedbackMessage = "",
                    feedbackColor = feedbackColor,
                    showAccuracy = true,
                    accuracy = accuracy,
                    currentTask = taskLevels[currentTaskIndex],
                    onExitTask = { exitToStartScreen() },
                    isMatchButtonEnabled = true
                )
            }
        }
    }

    private fun saveAccelerometerData(context: Context) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "accelerometer_data_$timestamp.txt"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val resolver = context.contentResolver
        val existingUri = getExistingFileUri(resolver, fileName)

        val uri = existingUri ?: resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        try {
            uri?.let {
                resolver.openOutputStream(it, if (existingUri == null) "w" else "wa").use { outputStream ->
                    if (outputStream != null) {
                        outputStream.write(accelerometerData.toString().toByteArray())
                        Log.d("NBackTaskApp", "Accelerometer data saved to ${uri.path}")
                    } else {
                        Log.e("NBackTaskApp", "Failed to open output stream for $uri")
                        Toast.makeText(context, "Failed to save accelerometer data", Toast.LENGTH_LONG).show()
                    }
                }
            } ?: run {
                Log.e("NBackTaskApp", "Failed to create file")
                Toast.makeText(context, "Failed to create file for accelerometer data", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Log.e("NBackTaskApp", "IOException while saving accelerometer data", e)
            Toast.makeText(context, "Error saving accelerometer data", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveReactionTimeData(context: Context) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "reaction_time_data_$timestamp.txt"
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
                resolver.openOutputStream(it, if (existingUri == null) "w" else "wa").use { outputStream ->
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
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "accuracy_data_$timestamp.csv"
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
                resolver.openOutputStream(it, if (existingUri == null) "w" else "wa").use { outputStream ->
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
        val requiredMatches = 15  // Ensure exactly 15 matches for each n-back task

        while (matchCount < requiredMatches) {
            // Ensure exactly 5 to 8 distinct random numbers before the next match
            val randomCount = random.nextInt(5, 8)

            // Add 5 to 8 random numbers excluding the match number
            for (i in 1..randomCount) {
                var randomNumber: Int
                do {
                    randomNumber = random.nextInt(10)
                } while (targetNumbers.size >= nBackNumber && randomNumber == targetNumbers[targetNumbers.size - nBackNumber])

                targetNumbers.add(randomNumber)
            }

            // Ensure we have enough numbers in the list to insert a match
            if (targetNumbers.size >= nBackNumber) {
                // Add a match by repeating a number from nBackNumber positions earlier
                targetNumbers.add(targetNumbers[targetNumbers.size - nBackNumber])
                matchCount++

                // Stop immediately if we have exactly 15 matches
                if (matchCount == requiredMatches) {
                    break
                }
            }
        }

        // Set the generated sequence to the nBackSequence (with exactly 15 matches)
        nBackSequence = targetNumbers
        Log.d("NBackTaskApp", "Generated n-back sequence: $nBackSequence")
    }

    private suspend fun displayNextNumber() {
        if (currentIndex >= nBackSequence.size) {
            endNBackTask()
            return
        }

        // Set the display time for the current number
        numberDisplayedTime = System.currentTimeMillis()

        // Enable the match button when displaying the next number
        isMatchButtonEnabled = true

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
                    currentTask = taskLevels[currentTaskIndex],
                    feedbackMessage = feedbackMessage,
                    feedbackColor = feedbackColor,
                    showFeedback = false, // Hide feedback while showing the next number
                    isMatchButtonEnabled = isMatchButtonEnabled,
                    onExitTask = { exitToStartScreen() }
                )
            }
        }

        delay(500L) // Show the current number for 500 ms

        setContent {
            NBackTaskAppTheme {
                NBackTaskScreen(
                    onStartTask = { startNBackTask() },
                    onNextTask = { nextNBackTask() },
                    onMatchPress = { handleMatchPress() },
                    currentNumber = null, // Hide the number after the delay
                    isTaskRunning = true,
                    showNumber = false,
                    showAccuracy = false,
                    accuracy = 0.0,
                    currentTask = taskLevels[currentTaskIndex],
                    feedbackMessage = feedbackMessage,
                    feedbackColor = feedbackColor,
                    showFeedback = false,
                    isMatchButtonEnabled = isMatchButtonEnabled,
                    onExitTask = { exitToStartScreen() }
                )
            }
        }

        delay(1500L) // Pause between numbers to maintain consistent timing
        currentIndex++
        displayNextNumber()  // Recursively call to display the next number
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
fun MainScreen(onStartTask: () -> Unit, onRestartTutorial: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Press Start to begin the n-back task or restart the tutorial",
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
        Spacer(modifier = Modifier.height(16.dp)) // Adds space between the buttons
        Button(
            onClick = onRestartTutorial,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restart Tutorial")
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
    currentTask: Int,
    showFeedback: Boolean,
    feedbackMessage: String,
    feedbackColor: Color,
    isMatchButtonEnabled: Boolean, // New parameter to control button enable/disable
    onExitTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,  // Align elements from the top down
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row for the Exit Task button at the top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onExitTask, colors = ButtonDefaults.buttonColors(Color.Red)) {
                Text("Exit Task")
            }
        }

        // Task text at the center
        Text(
            text = "$currentTask-back task",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display the accuracy if the task is completed
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
                // Show the number if the task is running
                if (showNumber && currentNumber != null) {
                    Text(
                        text = currentNumber.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                } else {
                    Spacer(modifier = Modifier.height(43.dp)) // Placeholder for the number
                }
                // Match button with dynamic enabled/disabled state
                Button(
                    onClick = onMatchPress,
                    modifier = Modifier.padding(top = 16.dp),
                    enabled = isMatchButtonEnabled // Control the button state
                ) {
                    Text("Match")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback message
        if (showFeedback) {
            Text(
                text = feedbackMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = feedbackColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun CountdownScreen(
    countdownTime: Int,
    onCountdownFinished: () -> Unit
) {
    var currentCount by remember { mutableStateOf(countdownTime) }

    // Launch a coroutine to count down
    LaunchedEffect(key1 = currentCount) {
        if (currentCount > 0) {
            delay(1000L) // 1-second delay for each count
            currentCount -= 1
        } else {
            onCountdownFinished() // Trigger when countdown finishes
        }
    }

    // Display the countdown
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = currentCount.toString(),
            style = MaterialTheme.typography.headlineLarge
        )
    }
}


@Composable
fun TutorialScreen(onStartTutorial: () -> Unit, onSkipTutorial: () -> Unit, onExitTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Welcome to the N-Back Task Tutorial",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "In this task, you will see a sequence of numbers. Your job is to identify when the current number matches the one presented n steps earlier.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "For example, in a 2-back task, you should press the 'Match' button if the current number is the same as the number shown 2 steps before.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onStartTutorial,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Start Tutorial")
        }
        Button(
            onClick = onSkipTutorial,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Skip Tutorial")
        }
    }
}

@Composable
fun TutorialExampleScreen(
    currentNumber: Int,
    onNext: () -> Unit,
    tutorialNBackNumber: Int,
    sequence: List<Int>,
    index: Int,
    onCorrectAnswer: () -> Unit,
    onRestartTutorial: () -> Unit,
    onStartTask: () -> Unit,
    onFinishTutorial: () -> Unit,
    onExitTask: () -> Unit,
    currentTutorialLevel: Int, // New parameter to manage tutorial level (1-back, 2-back, 3-back)
    onNextLevel: () -> Unit // Callback to proceed to the next tutorial level
) {
    val match = remember { mutableStateOf(false) }
    val incorrect = remember { mutableStateOf(false) }
    val loopIndex = remember { mutableStateOf(index) } // Use a mutable state to control the loop index

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onExitTask, colors = ButtonDefaults.buttonColors(Color.Red)) {
                Text("Exit")
            }
        }

        Text(
            text = "Tutorial Example: ${currentTutorialLevel}-back",
            color = MaterialTheme.colorScheme.inversePrimary,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Current Number: ${sequence[loopIndex.value]}",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Does this number match the one from $tutorialNBackNumber steps earlier?\n If yes, press 'Match'.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!match.value && !incorrect.value) {
            Button(
                onClick = {
                    // If the user presses Next, increase the index or reset it to loop over the sequence
                    loopIndex.value = (loopIndex.value + 1) % sequence.size // Loop through the sequence
                    incorrect.value = false // Reset incorrect flag on next
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Next")
            }
        }
        if (!match.value && !incorrect.value && loopIndex.value >= tutorialNBackNumber) {
            Button(
                onClick = {
                    // Check for match
                    if (loopIndex.value >= tutorialNBackNumber && sequence[loopIndex.value] == sequence[loopIndex.value - tutorialNBackNumber]) {
                        match.value = true
                        onCorrectAnswer() // Correct answer logic
                    } else {
                        incorrect.value = true
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Match")
            }
        }

        // When the user finds the correct match
        if (match.value) {
            Text(
                text = "Correct! You've completed the ${currentTutorialLevel}-back task.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Green,
                modifier = Modifier.padding(top = 16.dp)
            )
            if (currentTutorialLevel < 3) {
                Button(
                    onClick = {
                        match.value = false // Reset for next level
                        loopIndex.value = 0 // Reset index for next level
                        onNextLevel() // Proceed to the next level
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Next Level")
                }
            } else {
                Button(
                    onClick = onFinishTutorial,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Finish Tutorial")
                }
                Button(
                    onClick = onStartTask,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Start Task")
                }
            }
        }

        // When the user makes an incorrect match attempt
        if (incorrect.value) {
            Text(
                text = "Incorrect. Please try again.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = {
                    incorrect.value = false // Reset incorrect state
                    loopIndex.value = 0 // Restart the sequence loop
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Restart Level")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NBackTaskAppTheme {
        MainScreen(
            onStartTask = {},
            onRestartTutorial = {}
        )
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
            currentTask = 1,
            showFeedback = false,
            feedbackMessage = "Correct!",
            feedbackColor = Color.Green,
            onExitTask = {},
            isMatchButtonEnabled = true // Set to true to enable the button
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TutorialScreenPreview() {
    NBackTaskAppTheme {
        TutorialScreen(onStartTutorial = {}, onSkipTutorial = {}, onExitTask = {})
    }
}

@Preview(showBackground = true)
@Composable
fun TutorialExampleScreenPreview() {
    NBackTaskAppTheme {
        TutorialExampleScreen(
            currentNumber = 3,
            onNext = {},
            tutorialNBackNumber = 2,
            sequence = listOf(1, 2, 3, 1, 4),
            index = 2,
            onCorrectAnswer = {},
            onRestartTutorial = {},
            onStartTask = {},
            onFinishTutorial = {},
            onExitTask = {},
            currentTutorialLevel = 1,
            onNextLevel = {}
        )
    }
}
