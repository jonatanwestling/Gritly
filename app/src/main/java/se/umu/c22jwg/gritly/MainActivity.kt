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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import se.umu.c22jwg.gritly.ui.screens.MainScreen
import se.umu.c22jwg.gritly.ui.theme.GritlyTheme
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import se.umu.c22jwg.gritly.ui.viewmodel.PictureViewModel
import java.io.File
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private lateinit var file: File
    private val viewModel: PictureViewModel by viewModels()

    private val launcher=registerForActivityResult(
        //Use the predefined contract to take the picture
        ActivityResultContracts.TakePicture()) {
        if(it) {
            viewModel.file=file
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
                MainScreen(taskViewModel = taskViewModel, pictureViewModel = viewModel)
            }
        }

        //A storage location for the image
        file=File(filesDir, "mypic.jpg")
    }

    private fun takePicture() {

        /* Setup an uri to the file. A File provider is used so that the Camera app can have access
         * to the location (in our local storage)we provide
         */
        val uri: Uri

        uri = FileProvider.getUriForFile(applicationContext, "$packageName.fileprovider", file)

        //Start the camera Activity
        launcher.launch(uri)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GritlyTheme {
        Text("MainScreen Preview - Requires TaskViewModel")
    }
}