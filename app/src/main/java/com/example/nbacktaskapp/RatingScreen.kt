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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nbacktaskapp.ui.theme.NBackTaskAppTheme
import java.io.IOException

@Composable
fun RatingScreen(onSubmitRating: (Int) -> Unit) {
    var rating by remember { mutableStateOf(4f) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Rate the difficulty of the task",
            style = MaterialTheme.typography.headlineMedium,
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
            value = rating,
            onValueChange = { rating = it },
            valueRange = 1f..7f,
            steps = 5,
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
        Button(
            onClick = {
                onSubmitRating(rating.toInt())
                Toast.makeText(context, "Your response has been submitted", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Submit")
        }
    }
}

fun saveRating(context: Context, rating: Int) {
    val fileName = "ratings.csv"
    val csvHeader = "Difficulty rating\n"
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
                // Write the rating under the header
                outputStream.write("$rating\n".toByteArray())
            }
        } ?: run {
            Toast.makeText(context, "Failed to save file", Toast.LENGTH_LONG).show()
        }
    } ?: run {
        Toast.makeText(context, "Failed to create file", Toast.LENGTH_LONG).show()
    }
}

fun getExistingFileUri(resolver: android.content.ContentResolver, fileName: String): android.net.Uri? {
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

fun fileIsEmpty(context: Context, uri: android.net.Uri): Boolean {
    val resolver = context.contentResolver
    resolver.openInputStream(uri)?.use { inputStream ->
        return inputStream.available() == 0
    }
    return false
}

@Preview(showBackground = true)
@Composable
fun RatingScreenPreview() {
    NBackTaskAppTheme {
        RatingScreen(onSubmitRating = {})
    }
}