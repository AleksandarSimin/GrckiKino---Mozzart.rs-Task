package com.asimin.grckikino

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class MainViewModel : ViewModel() {

    private val _upcomingDraws = MutableStateFlow<List<Draw>>(emptyList())
    val upcomingDraws: StateFlow<List<Draw>> = _upcomingDraws
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private var context: Context by Delegates.notNull()

    init {
        fetchUpcomingDraws()
    }

    private val _selectedNumbers = MutableStateFlow(listOf<Int>())
    val selectedNumbers: StateFlow<List<Int>> = _selectedNumbers.asStateFlow()

    private fun fetchUpcomingDraws() {
        viewModelScope.launch {
            _isLoading.value = true // Start loading
            try {
                val draws = GrckiKinoAPIHandler.service.getUpcomingDraws()
                _upcomingDraws.value = draws
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Došlo je do greške prilikom učitavanja podataka.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                _isLoading.value = false // End loading
            }
        }
    }

    fun toggleNumber(number: Int, onError: (String) -> Unit) {
        viewModelScope.launch {
            val currentNumbers = _selectedNumbers.value.toMutableList()
            if (currentNumbers.contains(number)) {
                currentNumbers.remove(number)
            } else {
                if (currentNumbers.size < 5) {
                    currentNumbers.add(number)
                } else {
                    onError("Možete odabrati maksimalno 5 brojeva.")
                }
            }
            _selectedNumbers.value = currentNumbers
        }
    }

    fun clearNumbers() {
        _selectedNumbers.value = emptyList()
    }
}