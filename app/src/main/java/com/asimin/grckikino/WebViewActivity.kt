package com.asimin.grckikino

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.asimin.grckikino.ui.theme.GrckiKinoTheme

class WebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrckiKinoTheme {
                WebViewScreen(url = "https://mozzartbet.com/sr/lotto-animation/26#")
            }
        }
    }
}

@Composable
fun WebViewScreen(url: String) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(factory = {
                WebView(it).apply {
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient() // Dodato za bolje rukovanje JavaScript-om
                    settings.javaScriptEnabled = true   // Mogućnost izvršavanja JavaScript-a, potencijalni rizik
                    settings.domStorageEnabled = true // Omogućavanje DOM storage
                    settings.loadWithOverviewMode = true // Omogućava bolje učitavanje stranica
                    settings.useWideViewPort = true // Omogućava prikaz cele stranice
                    loadUrl(url)
                }
            }, update = {
                it.loadUrl(url)
            })
        }
        Button(
            onClick = { (context as ComponentActivity).finish() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Zatvori")
        }
    }
}