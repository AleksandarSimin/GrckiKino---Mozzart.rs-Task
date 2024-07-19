package com.asimin.grckikino

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asimin.grckikino.ui.theme.GrckiKinoTheme
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue with the operation.
        } else {
            // Permission is denied. Inform the user that the permission is necessary.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check and request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            GrckiKinoTheme {
                MainScreen()
            }
        }
    }

    private fun enableEdgeToEdge() {
        // Implementacija rubnog do rubnog prikaza
        window?.apply {
            decorView.systemUiVisibility =
                decorView.systemUiVisibility or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel = remember { MainViewModel() }
    val context = LocalContext.current
    val selectedNumbers by viewModel.selectedNumbers.collectAsState()
    val upcomingDraws by viewModel.upcomingDraws.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grčki Tombo") },
                actions = {
                    Text("0,00 RSD", Modifier.padding(end = 16.dp))
                }
            )
        },
        content = { innerPadding ->
            Column {
//                NavigationBar()         // Pogledaj ako bude vremena.
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    NavigationBar()         // Neuspeo pokušaj da ne bude scrollable
                    DrawCurrentRoundInfo(upcomingDraws)
                    TalonTable(
                        selectedNumbers = selectedNumbers,
                        onNumberToggle = { number ->
                            viewModel.toggleNumber(number) { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    DrawBottomSection(viewModel.selectedNumbers)
                }
            }
        }
    )
}

@Composable
fun NavigationBar() {
    val context = LocalContext.current
    //Check if context is passed and Toast if it is null
    if(context == null){
        Toast.makeText(context, "Context is null", Toast.LENGTH_SHORT).show()

        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Blue)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Talon", color = Color.White, modifier = Modifier.padding(8.dp).clickable {
            Toast.makeText(context, "Talon selected", Toast.LENGTH_SHORT).show()
        })
        Text(
            "Izvlačenje uživo",
            color = Color.White,
            modifier = Modifier
                .padding(8.dp)
                .clickable {
                    context.startActivity(Intent(context, WebViewActivity::class.java))
                }
        )
        Text("Rezultati izvlačenja", color = Color.White, modifier = Modifier.padding(8.dp).clickable {
            Toast.makeText(context, "Rezultati izvlačenja selected", Toast.LENGTH_SHORT).show()
        })
    }
}

@Composable
fun DrawCurrentRoundInfo(upcomingDraws: List<Draw>) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    val nextDraw = upcomingDraws.firstOrNull { draw ->
        val drawTime = Instant.ofEpochMilli(draw.drawTime.toLong())
        val currentTime = Instant.now()
        drawTime.isAfter(currentTime)
    }

    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    val drawTimeText = nextDraw?.let { formatter.format(Instant.ofEpochMilli(it.drawTime.toLong())) } ?: "N/A"
    val drawIdText = nextDraw?.drawId?.toString() ?: "N/A"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Vreme izvlačenja: $drawTimeText",
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = "Kolo: $drawIdText",
                color = Color.White,
                fontSize = 16.sp
            )
        }
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Text("Promeni kolo")
        }
    }

    if (showDialog) {
        showUpcomingDrawsDialog(upcomingDraws = upcomingDraws, onDismiss = { showDialog = false })
    }
}


@Composable
fun showUpcomingDrawsDialog(upcomingDraws: List<Draw>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        text = {
            Column {
                upcomingDraws.forEachIndexed { index, draw ->
                    val formattedTime = try {
                        // Check if drawTime is numeric (timestamp in milliseconds)
                        val timestamp = draw.drawTime.toLong()
                        // Convert timestamp to Date
                        val date = Date(timestamp)
                        // Format Date to "HH:mm:ss"
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
                    } catch (e: Exception) {
                        // If not a valid timestamp or any other parsing error occurs
                        "Invalid time"
                    }

                    Text("${index + 1}. Draw ID: ${draw.drawId}, Time: $formattedTime")
                }
            }
        }
    )
}

@Composable
fun TalonTable(selectedNumbers: List<Int>, onNumberToggle: (Int) -> Unit) {
    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        for (i in 0 until 10) {
            Row {
                for (j in 1..8) {
                    val number = i * 8 + j
                    NumberBox(number = number, isSelected = selectedNumbers.contains(number)) {
                        onNumberToggle(number)
                    }
                }
            }
        }
    }
}

@Composable
fun NumberBox(number: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .background(color = if (isSelected) Color.Green else Color.Gray, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun DrawBottomSection(selectedNumbers: StateFlow<List<Int>>) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Preostalo vreme za uplatu: 02:08",
            color = Color.White,
            fontSize = 18.sp
        )
        Button(
            onClick = {
                Toast.makeText(
                    context,
                    "Uplata uspešna za ${selectedNumbers.value.joinToString()}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp)
                .background(Color.Green, shape = CircleShape)
        ) {
            Text("Uplati")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    GrckiKinoTheme {
        MainScreen()
    }
}
