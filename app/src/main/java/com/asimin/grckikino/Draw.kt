package com.asimin.grckikino

data class Draw(
    val drawId: Int,
    val drawTime: Long,
    val selectedNumbers: List<Int>,
    val winningNumbers: List<Int>? // nullable property waiting for the winning numbers to be fetched
)