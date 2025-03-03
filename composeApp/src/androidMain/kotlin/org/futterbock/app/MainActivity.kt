package org.futterbock.app

import App
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.FirebaseApp
import services.pdfService.PdfServiceImpl

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var appContext: Context
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContent {
            App(PdfServiceImpl(this))
        }
        appContext = applicationContext
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    //App(PdfServiceImpl(null))
}