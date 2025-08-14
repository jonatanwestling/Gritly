package se.umu.c22jwg.gritly

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import se.umu.c22jwg.gritly.ui.screens.MainScreen
import se.umu.c22jwg.gritly.ui.theme.GritlyTheme
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import se.umu.c22jwg.gritly.ui.viewmodel.DailyViewModel
import java.io.File
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private lateinit var file: File
    private val viewModel: DailyViewModel by viewModels()

    private val launcher=registerForActivityResult(
        //Use the predefined contract to take the picture
        ActivityResultContracts.TakePicture()) {
        if(it) {
            viewModel.file=file
            // Insert path to DB
            viewModel.onPhotoTaken()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // Inform ViewModel how to launch the camera
        viewModel.setTakePictureLauncher {
            // Compose the URI from the file inside the ViewModel
            val uri = FileProvider.getUriForFile(
                applicationContext,
                "$packageName.fileprovider",
                viewModel.file
            )
            launcher.launch(uri)
        }

        setContent {
            GritlyTheme {
                val taskViewModel: TaskViewModel = viewModel()
                MainScreen(taskViewModel = taskViewModel, dailyViewModel = viewModel)
            }
        }

        //A storage location for the image
        val dateString = java.time.LocalDate.now().toString()
        file = File(filesDir, "dayPhoto$dateString.jpg")

        viewModel.file = file
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GritlyTheme {
        Text("MainScreen Preview - Requires TaskViewModel")
    }
}