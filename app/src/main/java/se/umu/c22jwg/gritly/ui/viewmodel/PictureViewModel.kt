package se.umu.c22jwg.gritly.ui.viewmodel
// File: PictureViewModel.kt
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class PictureViewModel(app: Application, private val state: SavedStateHandle) : AndroidViewModel(app) {

    var file: File = state["file"] ?: File(app.filesDir, "mypic.jpg")
        set(value) {
            state["file"] = value
            field = value
            viewModelScope.launch {
                bitmap.value = rotatedBitmap(value)
            }
        }

    val bitmap: MutableState<Bitmap> = mutableStateOf(
        if (file.isFile) {
            rotatedBitmap(file)
        } else {
            BitmapFactory.decodeResource(app.resources, android.R.drawable.ic_menu_gallery)
        }
    )

    private var takePictureLauncher: (() -> Unit)? = null

    fun setTakePictureLauncher(launcher: () -> Unit) {
        takePictureLauncher = launcher
    }

    fun takePicture() {
        takePictureLauncher?.invoke()
    }
    fun hasPicture(): Boolean {
        return file.exists() && file.length() > 0
    }

    fun onPhotoTaken(onPathAvailable: (String) -> Unit) {
        if (file.exists()) {
            onPathAvailable(file.absolutePath)
        }
    }

    private fun rotatedBitmap(file: File): Bitmap {
        var bm = BitmapFactory.decodeFile(file.absolutePath)
        val orientation = getExifRotation(file)
        val matrix = Matrix()
        if (orientation != 0 && bm != null) {
            matrix.postRotate(orientation.toFloat())
            bm = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
        }
        return bm
    }

    private fun getExifRotation(imageFile: File): Int {
        return try {
            val exif = ExifInterface(imageFile)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            0
        }
    }

    // Call this to remove the photo file and reset the state
    fun clearImage() {
        file.let {
            if (it.exists()) {
                it.delete()
            }
        }
    }
}