package com.asimin.grckikino

import android.Manifest
import android.content.Context
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    val context: Context
        get() = this
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
                MainScreen(context)
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
fun MainScreen(context: Context) {
    val viewModel = remember { MainViewModel(context) }
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
                title = { Text("Grčki Kino") },
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
    var showDialogTalon by remember { mutableStateOf(false) }
    var showDialogHistory by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Blue)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Istorija", color = Color.White, modifier = Modifier
            .padding(8.dp)
            .clickable {
                showDialogHistory = true
            })
        Text("Talon", color = Color.White, modifier = Modifier
            .padding(8.dp)
            .clickable {
                showDialogTalon = true
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
        Text("Rezultati", color = Color.White, modifier = Modifier
            .padding(8.dp)
            .clickable {
                Toast
                    .makeText(context, "Rezultati izvlačenja selected", Toast.LENGTH_SHORT)
                    .show()
            })
        if (showDialogTalon) {
            ShowTalonDialog(talon = talon, onDismiss = { showDialogTalon = false }, viewModel = viewModel)
        }
        if (showDialogHistory) {
            var showOverwriteDialog by remember { mutableStateOf(false) }
            var selectedTalonForOverwrite by remember { mutableStateOf<Talon?>(null) }

            AlertDialog(
                onDismissRequest = { showDialogHistory = false },
                title = {
                    Column {
                        Text(text = "Istorija", style = MaterialTheme.typography.titleLarge)
                        Text(text = "pregled uplata sa mogućnošću učitavanja talona", style = MaterialTheme.typography.bodySmall)
                    }
                },
                text = {
                    LazyColumn {
                        itemsIndexed(viewModel.history.value) { index, talon ->
                            val formattedTime = Instant.ofEpochMilli(talon.talonPaymentTime)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd.MMM.yy HH:mm:ss"))
                            val lineNumber = (index + 1).toString().padStart(2, ' ')
                            Column(modifier = Modifier.clickable {
                                if (viewModel.talon.value.isNotEmpty()) {
                                    selectedTalonForOverwrite = talon
                                    showOverwriteDialog = true
                                } else {
                                    viewModel.addToTalonFromHistory(talon)
                                    Toast.makeText(context, "Učitan Talon iz Istorija", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "$lineNumber. Uplata: $formattedTime")
                                    Text(text = "Izvlačenja: ${talon.talonNumberOfDraws}")
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
                    Button(onClick = { showDialogHistory = false }) {
                        Text("Zatvori")
                    }
                }
            )

            if (showOverwriteDialog) {
                AlertDialog(
                    onDismissRequest = { showOverwriteDialog = false },
                    title = { Text("Taloni iz History") },
                    text = { Text("Trenutni Talon nije prazan. Želite li da prepisujete postojeći talon?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.addToTalonFromHistory(selectedTalonForOverwrite!!)
                            showOverwriteDialog = false
                            Toast.makeText(context, "Učitan Talon iz History", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Da")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showOverwriteDialog = false }) {
                            Text("Ne")
                        }
                    }
                )
            }
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
    Column(modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
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
    val currentTime = System.currentTimeMillis()

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
                    val drawTimePassed = draw.drawTime < currentTime
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
                            if (drawTimePassed) {
                                Text(text = "$lineNumber. Vreme: $formattedTime", color = Color(0xFFFFA500)) // Orange if time passed
                            } else {
                                Text(text = "$lineNumber. Vreme: $formattedTime")
                            }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    viewModel.clearTalon() // Clear the talon
                    onDismiss() // Close the dialog after clearing
                }) {
                    Text("Obriši Talon")
                }
                Button(onClick = onDismiss) {
                    Text("Zatvori")
                }
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CountdownToDraw(viewModel = viewModel)
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
                                showDuplicateDialog = true
                            },
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Kolo dodato u Talon",
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.selectedNumbersClear()
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
                    if (viewModel.talon.value.isEmpty()) {
                        Toast.makeText(context, "Talon je prazan.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val currentTime = System.currentTimeMillis()
                    if (viewModel.talon.value.any { it.drawTime < currentTime }) {
                        showInvalidTalonDialog = true
                    } else {
                        makePayment(context, viewModel)
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
                            // Proceed with the payment
                            makePayment(context, viewModel)
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
                                    viewModel.selectedNumbersClear()
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

fun makePayment(context: Context, viewModel: MainViewModel) {
    val currentTime = System.currentTimeMillis()
    Toast.makeText(context, "Uplata uspešna!", Toast.LENGTH_SHORT).show()
    val dbHelper = DatabaseHelper(context)
    viewModel.talon.value.forEach { draw ->
        dbHelper.insertTalon(draw, currentTime)
    }
    viewModel.addToHistory(Talon(currentTime, viewModel.talon.value.size, viewModel.talon.value))
    viewModel.clearTalon()
}

@Composable
fun CountdownToDraw(viewModel: MainViewModel) {
    val talon by viewModel.talon.collectAsState()
    var timeLeft by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    // Restart the timer every time the size of the Talon list changes
    LaunchedEffect(key1 = talon.size) {
        // Only start the timer if Talon is not empty
        if (talon.isNotEmpty()) {
            timeLeft = viewModel.timeToClosestDraw()
            if (timeLeft > 0) {
                coroutineScope.launch {
                    while (timeLeft > 0) {
                        timeLeft = viewModel.timeToClosestDraw()
                        delay(1000)
                    }
                }
            }
        }
    }

    val currentTime = System.currentTimeMillis()
    val color = when {
        talon.isEmpty() -> Color.Cyan // Talon is empty, default to Cyan as per previous logic
        talon.any { it.drawTime <= currentTime } -> Color.Red // At least one draw has an invalid time
        else -> Color.Cyan // Talon exists and all draws have valid times
    }
    val displayText = if (talon.isNotEmpty() && timeLeft > 0) {
        val minutes = (timeLeft / 60000).toInt()
        val seconds = ((timeLeft % 60000) / 1000).toInt()
        "Vreme do najbližeg izvačenja: ${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else if (talon.isEmpty()) {
        "Talon je prazan."
    } else {
        "Talon ima nevažeće izvlačenje!"
    }

    Text(
        text = displayText,
        color = color,
        fontSize = 16.sp
    )

}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val context = LocalContext.current
    GrckiKinoTheme {
        MainScreen(context)
    }
}

