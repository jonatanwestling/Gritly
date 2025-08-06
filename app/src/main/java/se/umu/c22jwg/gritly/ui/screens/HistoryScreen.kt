package se.umu.c22jwg.gritly.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(taskViewModel: TaskViewModel) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Create a file in /files/Pictures/
    val imageFile = remember {
        context.createImageFileInPicturesDir()
    }

    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        imageFile
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = uri
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }) {
                Icon(Icons.Default.Person, contentDescription = "Take Picture")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Today Screen", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUri),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("No image captured yet.")
            }
        }
    }
}

// ✅ Fix: Save to /files/Pictures/ since your file_paths.xml uses <files-path path="Pictures/">
fun Context.createImageFileInPicturesDir(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val fileName = "JPEG_${timeStamp}.jpg"
    val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    if (storageDir != null && !storageDir.exists()) {
        storageDir.mkdirs()
    }
    return File(storageDir, fileName)
}