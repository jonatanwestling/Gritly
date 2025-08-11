package se.umu.c22jwg.gritly.ui.viewmodel
// File: PictureViewModel.kt
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import se.umu.c22jwg.gritly.data.DailyEntry
import se.umu.c22jwg.gritly.data.DailyEntryRepository
import se.umu.c22jwg.gritly.data.DatabaseProvider
import java.io.File
import java.io.IOException
import java.time.LocalDate
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.exifinterface.media.ExifInterface
import java.io.FileInputStream

class PictureViewModel(app: Application, private val state: SavedStateHandle) : AndroidViewModel(app) {

    private val database = DatabaseProvider.getDatabase(app)
    private val dailyEntryRepository = DailyEntryRepository(database.dailyEntryDao())
    var currentDate =  LocalDate.now().toString()
    var file: File = state["file"] ?: File(app.filesDir, "dayPhoto$currentDate.jpg")
        set(value) {
            state["file"] = value
            field = value
            viewModelScope.launch {
                if (rotatedBitmap(value) != null) {
                    bitmap.value = rotatedBitmap(value)
                }
            }
        }

    val bitmap: MutableState<Bitmap?> = mutableStateOf(
        if (file.isFile) {
            rotatedBitmap(file)
        } else {
            null
        }
    )

    private var takePictureLauncher: (() -> Unit)? = null

    //For the notes:
    private val _noteForToday = mutableStateOf<String>("")
    val noteForToday: State<String> = _noteForToday

    fun setTakePictureLauncher(launcher: () -> Unit) {
        takePictureLauncher = launcher
    }

    fun takePicture() {
        takePictureLauncher?.invoke()
    }
    fun hasPicture(): Boolean {
        return file.exists() && file.length() > 0
    }

    fun onPhotoTaken() {
        viewModelScope.launch {
            Log.d("DEBUG", "HEJ")
            if (file.exists()) {
                Log.d("DEBUG", "SAN")
                // reload bitmap
                val rotated = rotatedBitmap(file)
                bitmap.value = rotated
                // Get existing note so we can save it again
                val existingEntry = dailyEntryRepository.getEntryByDate(currentDate)
                dailyEntryRepository.insertEntry( DailyEntry(
                    date = LocalDate.now().toString(),
                    note = existingEntry?.note ?: "",
                    file.absolutePath
                ))
            }
        }
    }

    private fun rotatedBitmap(file: File): Bitmap? {
        if (!file.exists()) return null
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
            FileInputStream(imageFile).use { inputStream ->
                val exif = ExifInterface(inputStream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: IOException) {
            0
        }
    }
    fun loadNoteForToday() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val entry = dailyEntryRepository.getEntryByDate(today)
            _noteForToday.value = entry?.note ?: ""
        }
    }

    fun updateNoteForToday(note: String) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existingEntry = dailyEntryRepository.getEntryByDate(today)
            dailyEntryRepository.insertEntry(
                DailyEntry(
                    date = today,
                    note = note,
                    imagePath =  existingEntry?.imagePath ?: ""
                )
            )
            Log.d("PictureViewModel", "Note updated for $today: $note")
        }
    }

    // Call this to remove the photo file and reset the state
    fun clearImage() {
        file.let {
            if (it.exists()) {
                it.delete()
            }
        }
        bitmap.value = null
        // Delete the image path
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existingEntry = dailyEntryRepository.getEntryByDate(today)
            dailyEntryRepository.insertEntry(
                DailyEntry(
                    date = today,
                    note = existingEntry?.note ?: "",
                    imagePath = "No Image"
                )
            )
        }

    }
}