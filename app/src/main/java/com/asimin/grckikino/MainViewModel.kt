package com.asimin.grckikino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _selectedNumbers = MutableStateFlow(listOf<Int>())
    val selectedNumbers: StateFlow<List<Int>> = _selectedNumbers.asStateFlow()

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
}