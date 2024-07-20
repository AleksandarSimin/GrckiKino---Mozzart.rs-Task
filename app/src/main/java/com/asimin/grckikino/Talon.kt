package com.asimin.grckikino

data class Talon(
    val talonPaymentTime: Long,
    val talonNumberOfDraws: Int,
    val talonDraws: List<Draw>,
)
