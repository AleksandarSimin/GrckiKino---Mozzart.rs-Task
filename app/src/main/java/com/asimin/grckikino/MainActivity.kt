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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.LaunchedEffect
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    var selectedDraw by remember { mutableStateOf<Draw?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Automatically select the first incoming draw
    LaunchedEffect(upcomingDraws) {
        if (upcomingDraws.isNotEmpty()) {
            selectedDraw = upcomingDraws.first()
        }
    }

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
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    NavigationBar(viewModel = viewModel)
                    selectedDraw?.let {
                        DrawCurrentRoundInfo(it, onButtonClick = { showDialog = true })
                    }
                    TalonTable(
                        selectedNumbers = selectedNumbers,
                        onNumberToggle = { number ->
                            viewModel.toggleNumber(number) { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    DrawBottomSection(viewModel.selectedNumbers, viewModel, selectedDraw)
                }
                if (showDialog) {
                    showUpcomingDrawsDialog(
                        upcomingDraws = upcomingDraws,
                        onDismiss = { showDialog = false },
                        onSelect = { draw ->
                            selectedDraw = draw
                            showDialog = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun NavigationBar(viewModel: MainViewModel) {
    val context = LocalContext.current
    val talon by viewModel.talon.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Blue)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Talon", color = Color.White, modifier = Modifier
            .padding(8.dp)
            .clickable {
                showDialog = true
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
        Text("Rezultati izvlačenja", color = Color.White, modifier = Modifier
            .padding(8.dp)
            .clickable {
                Toast
                    .makeText(context, "Rezultati izvlačenja selected", Toast.LENGTH_SHORT)
                    .show()
            })
        if (showDialog) {
            ShowTalonDialog(talon = talon, onDismiss = { showDialog = false }, viewModel = viewModel)
        }
    }
}

@Composable
fun DrawCurrentRoundInfo(selectedDraw: Draw, onButtonClick: () -> Unit) {
    if(selectedDraw.drawTime == null) {
        Toast.makeText(LocalContext.current, "Nema informacija o izvlačenju", Toast.LENGTH_SHORT).show()
        return
    }
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    val drawTime = Instant.ofEpochMilli(selectedDraw.drawTime.toLong()).atZone(ZoneId.systemDefault()).toLocalTime().format(formatter)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Vreme izvlačenja: $drawTime",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "Kolo: ${selectedDraw.drawId}",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
            Button(
                onClick = onButtonClick,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Izaberi kolo")
            }
        }
    }
}

@Composable
fun showUpcomingDrawsDialog(upcomingDraws: List<Draw>, onDismiss: () -> Unit, onSelect: (Draw) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Izaberite kolo") },
        text = {
            LazyColumn {
                itemsIndexed(upcomingDraws) { index, draw ->
                    val formattedTime = Instant.ofEpochMilli(draw.drawTime.toLong()).atZone(ZoneId.systemDefault()).toLocalTime().format(formatter)
                    val lineNumber = (index + 1).toString().padStart(2, ' ')
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(draw) }
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "$lineNumber. Vreme: $formattedTime")
                            Text(text = "Kolo: ${draw.drawId}")
                        }
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.Gray)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Zatvori")
            }
        }
    )
}

@Composable
fun TalonTable(selectedNumbers: List<Int>, onNumberToggle: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
fun ShowTalonDialog(talon: List<Draw>, onDismiss: () -> Unit, viewModel: MainViewModel) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    var showDialog by remember { mutableStateOf(false) }
    var selectedDrawForDeletionIndex by remember { mutableStateOf(-1) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Da li ste sigurni?") },
            text = { Text("Da li želite da obrišete izabrano kolo iz talona?") },
            confirmButton = {
                Button(onClick = {
                    if (selectedDrawForDeletionIndex >= 0) {
                        viewModel.removeFromTalon(selectedDrawForDeletionIndex)
                        selectedDrawForDeletionIndex = -1 // Reset the index after deletion
                    }
                    showDialog = false
                }) {
                    Text("Da")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Otkaži")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Talon") },
        text = {
            LazyColumn {
                itemsIndexed(talon) { index, draw ->
                    val formattedTime = Instant.ofEpochMilli(draw.drawTime.toLong()).atZone(ZoneId.systemDefault()).toLocalTime().format(formatter)
                    val lineNumber = (index + 1).toString().padStart(2, ' ')
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDrawForDeletionIndex = index
                                    showDialog = true
                                }
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "$lineNumber. Vreme: $formattedTime")
                            Text(text = "Kolo: ${draw.drawId}")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            draw.selectedNumbers.take(5).forEach { number ->
                                Text(text = number.toString(), modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.Gray)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Zatvori")
            }
        }
    )
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
fun DrawBottomSection(
    selectedNumbers: StateFlow<List<Int>>,
    viewModel: MainViewModel,
    selectedDraw: Draw?
) {
    val context = LocalContext.current
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showInvalidTalonDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Preostalo vreme za uplatu: 02:08",
                color = Color.White,
                fontSize = 18.sp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    selectedDraw?.let {
                        viewModel.addToTalon(
                            it.copy(selectedNumbers = selectedNumbers.value),
                            onInsufficientNumbers = {
                                Toast.makeText(
                                    context,
                                    "Morate odabrati najmanje 3 broja.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onDuplicateFound = {
                                // Show a dialog here asking the user to continue or cancel
                                // If continue, handle accordingly
                                showDuplicateDialog = true
                            },
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Kolo dodato u Talon",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Dodaj u Talon")
            }
            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (viewModel.talon.value.any { it.drawTime < currentTime }) {
                        showInvalidTalonDialog = true
                    } else {
                        Toast.makeText(
                            context,
                            "Uplata uspešna!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Uplati")
            }

            if (showInvalidTalonDialog) {
                AlertDialog(
                    onDismissRequest = { showInvalidTalonDialog = false },
                    title = { Text("Talon je nevažeći") },
                    text = { Text("Talon je nevažeći, postoji kolo koje je izvučeno. Želite li automatsko uklanjanje kola?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.removeInvalidDrawsFromTalon()
                            showInvalidTalonDialog = false
                            Toast.makeText(context, "Nevažeća kola su uklonjena.", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Da")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showInvalidTalonDialog = false }) {
                            Text("Ne")
                        }
                    }
                )
            }
        }
        if (showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = { showDuplicateDialog = false },
                title = { Text("Kolo već postoji u Talonu") },
                text = { Text("Želite li da nastavite?") },
                confirmButton = {
                    Button(onClick = {
                        showDuplicateDialog = false
                        // Add to Talon without additional checks since it's a duplicate confirmation
                        selectedDraw?.let {
                            viewModel.addToTalon(
                                it.copy(selectedNumbers = selectedNumbers.value),
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Kolo dodato u Talon",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }) {
                        Text("Da")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDuplicateDialog = false }) {
                        Text("Ne")
                    }
                }
            )
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

