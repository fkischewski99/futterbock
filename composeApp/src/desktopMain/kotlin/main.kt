import android.app.Application
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.andreasgift.kmpweatherapp.BuildKonfig
import com.google.firebase.FirebasePlatform
import com.google.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.persistentCacheSettings
import dev.gitlive.firebase.initialize
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import services.pdfService.PdfServiceImpl
import java.awt.*
import java.io.PrintWriter
import java.io.StringWriter


fun main() = application {
    try {
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
    } catch (e: Exception) {
        errorDialog(e)
    }

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        errorDialog(e)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Futterbock_App",
        state = WindowState(width = 1000.dp, height = 700.dp)
    ) {
        DevelopmentEntryPoint {
            App(PdfServiceImpl())
        }
    }
}

private fun errorDialog(e: Throwable) {
    Dialog(Frame(), "Error", true).apply {
        layout = BorderLayout()

        // Exception summary panel
        val topPanel = Panel(FlowLayout()).apply {
            add(Label("Exception: ${e::class.qualifiedName}"))
            add(Label("Message: ${e.message ?: "No message"}"))
        }
        add(topPanel, BorderLayout.NORTH)

        // Stack trace in a scrollable text area
        val stackTraceText = StringWriter().also { sw ->
            e.printStackTrace(PrintWriter(sw))
        }.toString()

        val textArea = TextArea(stackTraceText, 30, 100, TextArea.SCROLLBARS_BOTH).apply {
            isEditable = false
        }
        add(textArea, BorderLayout.CENTER)

        // OK button to close
        val bottomPanel = Panel(FlowLayout()).apply {
            add(Button("OK").apply {
                addActionListener { dispose() }
            })
        }
        add(bottomPanel, BorderLayout.SOUTH)

        setSize(800, 600)
        setLocationRelativeTo(null)
        isVisible = true
    }
}