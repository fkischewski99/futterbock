import android.app.Application
import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.andreasgift.kmpweatherapp.BuildKonfig
import com.google.api.ResourceProto.resource
import com.google.firebase.FirebasePlatform
import com.google.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.persistentCacheSettings
import dev.gitlive.firebase.initialize
import org.json.JSONObject
import services.pdfService.PdfServiceImpl


fun main() = application {
    FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {

        val storage = mutableMapOf<String, String>()
        override fun clear(key: String) {
            storage.remove(key)
        }

        override fun log(msg: String) = println(msg)

        override fun retrieve(key: String) = storage[key]

        override fun store(key: String, value: String) = storage.set(key, value)

    })

    val options = FirebaseOptions(
        projectId = BuildKonfig.FIREBASE_PROJECT_ID,
        applicationId = BuildKonfig.FIREBASE_APPLICATION_ID,
        apiKey = BuildKonfig.FIREBASE_API_KEY
    )

    val app = Firebase.initialize(Application(), options)
    val db = Firebase.firestore(app)
    val settings = firestoreSettings {
        // Use memory cache
        cacheSettings = persistentCacheSettings { }
    }
    db.settings = settings
    FirebaseDatabase.getInstance().setPersistenceEnabled(true)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Futterbock_App",
        state = WindowState(width = 1000.dp, height = 700.dp)
    ) {
        App(PdfServiceImpl())
    }
}