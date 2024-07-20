package com.asimin.grckikino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _selectedNumbers = MutableStateFlow(listOf<Int>())
    val selectedNumbers: StateFlow<List<Int>> = _selectedNumbers

    private val _upcomingDraws = MutableStateFlow(listOf<Draw>())
    val upcomingDraws: StateFlow<List<Draw>> = _upcomingDraws

    private val _talon = MutableStateFlow<List<Draw>>(emptyList())
    val talon: StateFlow<List<Draw>> = _talon

    private val _history = MutableStateFlow<List<Talon>>(emptyList())
    val history: StateFlow<List<Talon>> = _history

    init {
        getUpcomingDrawsSafe(5, 1000)
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
}
