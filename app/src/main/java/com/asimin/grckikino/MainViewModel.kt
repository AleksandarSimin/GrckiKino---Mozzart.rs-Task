package com.asimin.grckikino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _selectedNumbers = MutableStateFlow(listOf<Int>())
    val selectedNumbers: StateFlow<List<Int>> = _selectedNumbers

    private val _upcomingDraws = MutableStateFlow(listOf<Draw>())
    val upcomingDraws: StateFlow<List<Draw>> = _upcomingDraws

    init {
        fetchUpcomingDraws()
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

    private fun fetchUpcomingDraws() {
        viewModelScope.launch {
            try {
                val draws = GrckiKinoAPIHandler.service.getUpcomingDraws()
                _upcomingDraws.value = draws
            } catch (e: Exception) {
                // Error handling
                println("Error fetching upcoming draws: ${e.message}")
            }
        }
    }
}
