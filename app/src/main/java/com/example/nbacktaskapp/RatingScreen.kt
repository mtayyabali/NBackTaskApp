package com.example.nbacktaskapp

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun RatingScreen(currentTask: Int, onSubmitRating: (Int) -> Unit) {
    var rating by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Rate the Difficulty of the ${currentTask}-back Task",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 1..7) {
                Text(text = i.toString(), modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
        Slider(
            value = rating.toFloat(),
            onValueChange = { rating = it.toInt() },
            valueRange = 1f..7f,
            steps = 6,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Very Easy", modifier = Modifier.padding(start = 8.dp))
            Text(text = "Very Difficult", modifier = Modifier.padding(end = 8.dp))
        }
        Button(onClick = { onSubmitRating(rating) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Submit Rating")
        }
    }
}

fun saveRating(context: Context, currentTask: Int, rating: Int) {
    val fileName = "ratings.csv"
    val csvHeader = "n-back Task,Difficulty rating\n"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
    }

    val resolver = context.contentResolver
    val existingUri = getExistingFileUri(resolver, fileName)

    val uri = existingUri ?: resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

    uri?.let {
        resolver.openOutputStream(it, if (existingUri == null) "wa" else "wa").use { outputStream ->
            if (outputStream != null) {
                if (existingUri == null || fileIsEmpty(context, it)) {
                    // Write the header if the file is new or empty
                    outputStream.write(csvHeader.toByteArray())
                }
                // Write the task and rating under the header
                outputStream.write("$currentTask,$rating\n".toByteArray())
            }
        } ?: run {
            Toast.makeText(context, "Failed to save file", Toast.LENGTH_LONG).show()
        }
    } ?: run {
        Toast.makeText(context, "Failed to create file", Toast.LENGTH_LONG).show()
    }
}

private fun getExistingFileUri(resolver: android.content.ContentResolver, fileName: String): android.net.Uri? {
    val projection = arrayOf(MediaStore.MediaColumns._ID)
    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
    val selectionArgs = arrayOf(fileName)

    val cursor = resolver.query(
        MediaStore.Files.getContentUri("external"),
        projection,
        selection,
        selectionArgs,
        null
    )

    return cursor?.use {
        if (it.moveToFirst()) {
            val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            android.net.Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())
        } else {
            null
        }
    }
}

private fun fileIsEmpty(context: Context, uri: android.net.Uri): Boolean {
    return context.contentResolver.openInputStream(uri).use { inputStream ->
        inputStream?.bufferedReader()?.use { reader ->
            reader.readLine() == null
        }
    } ?: true
}
