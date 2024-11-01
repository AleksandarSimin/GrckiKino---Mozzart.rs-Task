package com.asimin.grckikino

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(context: Context) : ViewModel() {
    private val _selectedNumbers = MutableStateFlow(listOf<Int>())
    val selectedNumbers: StateFlow<List<Int>> = _selectedNumbers

    private val _upcomingDraws = MutableStateFlow(listOf<Draw>())
    val upcomingDraws: StateFlow<List<Draw>> = _upcomingDraws

    private val _talon = MutableStateFlow<List<Draw>>(emptyList())
    val talon: StateFlow<List<Draw>> = _talon

    private val _history = MutableStateFlow<List<Talon>>(emptyList())
    val history: StateFlow<List<Talon>> = _history

    private val _drawResults = MutableStateFlow<List<DrawResult>>(emptyList())
    val drawResults: StateFlow<List<DrawResult>> = _drawResults

    init {
        getUpcomingDrawsSafe(5, 1000)
        getHistoryFromDatabase(context)
        fetchResultsSafe()
    }

    fun selectedNumbersClear() {
        _selectedNumbers.value = emptyList()
    }

    fun addToTalon(draw: Draw, onSuccess: () -> Unit) { //without checking
        _talon.value += draw
        onSuccess()
    }

    fun addToTalonFromHistory(talon: Talon) {
        _talon.value = talon.talonDraws
    }

    fun addToTalon(draw: Draw, onInsufficientNumbers: () -> Unit, onDuplicateFound: () -> Unit, onSuccess: () -> Unit) { //with checking
        if (draw.selectedNumbers.size < 3) {
            onInsufficientNumbers()
            return
        }

        val exists = _talon.value.any { it.drawId == draw.drawId }
        if (exists) {
            onDuplicateFound()
            return
        }

        _talon.value += draw
        onSuccess()
    }

    fun removeFromTalon(index: Int) {
        _talon.value = _talon.value.filterIndexed { i, _ -> i != index }
    }

    fun removeInvalidDrawsFromTalon() {
        val currentTime = System.currentTimeMillis()
        _talon.value = _talon.value.filter { it.drawTime >= currentTime }
    }

    fun clearTalon() {
        _talon.value = emptyList()
    }

    fun addToHistory(talon: Talon) {
        _history.value += talon
    }

    private fun getHistoryFromDatabase(context: Context) {
        val dbHelper = DatabaseHelper(context)
        val talons = dbHelper.importTalonsFromDatabaseToHistory()
        _history.value = talons
    }

    fun toggleNumber(number: Int, onError: (String) -> Unit) {
        _selectedNumbers.value = if (_selectedNumbers.value.contains(number)) {
            _selectedNumbers.value - number
        } else {
            if (_selectedNumbers.value.size < 5) {
                _selectedNumbers.value + number
            } else {
                onError("Možete odabrati maksimalno 5 brojeva.")
                _selectedNumbers.value
            }
        }
    }

    private fun getUpcomingDrawsSafe(maxRetries: Int, delayMillis: Long) { //retry mechanism to ensure the upcoming draws are fetched
        viewModelScope.launch {
            var retries = 0
            var draws: List<Draw>
            do {
                draws = GrckiKinoAPIHandler.service.getUpcomingDraws()
                if (draws.isNotEmpty()) {
                    _upcomingDraws.value = draws
                    break
                } else {
                    delay(delayMillis)
                    retries++
                }
            } while (retries < maxRetries)
        }
    }

    fun fetchResultsSafe() { //retry mechanism to ensure the upcoming draws are fetched
        viewModelScope.launch {
            var retries = 0
            do {
                val results = fetchAndLogDrawResults()
                if (results.isNotEmpty()) {
                    _drawResults.value = results
                    break
                } else {
                    delay(1000)
                    retries++
                }
            } while (retries < 3)
            if (retries == 3) {
                println("Nema Rezultata ili je došlo do greške.")
            }
        }
    }

    private suspend fun fetchAndLogDrawResults() : List<DrawResult> {
        val drawResultsTemp = MutableStateFlow<List<DrawResult>>(emptyList())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateFrom = LocalDate.now().minusDays(1).format(formatter) // Query for the previous day
        val dateTo = LocalDate.now().format(formatter) // Query for the previous day

        try {
            val getResult = GrckiKinoAPIHandler.resultsService.getDrawResults(dateTo, dateTo)   // dateFrom replaced due to API restriction
            getResult.content.forEach { drawResult ->
                val winningNumbers = WinningNumbers(drawResult.winningNumbers.list, drawResult.winningNumbers.bonus)
                val drawResultFromGetResult = DrawResult(drawResult.drawId, drawResult.drawTime, winningNumbers)
                drawResultsTemp.value += drawResultFromGetResult
            }
            println("GET RESULT (_drawResults.value): ${_drawResults.value}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (drawResultsTemp.value.isNotEmpty()) {
            return drawResultsTemp.value
        }
        return _drawResults.value
    }

    fun timeToClosestDraw(): Long {
        val currentTime = MyUtility.getDateAndTime() // This function returns the current date and time in milliseconds
        return _talon.value
            .filter { it.drawTime > currentTime }
            .minByOrNull { it.drawTime }
            ?.let { it.drawTime - currentTime }
            ?: -1 // Return -1 if there are no future draws
    }
}
