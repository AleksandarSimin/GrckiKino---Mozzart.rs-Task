package com.asimin.grckikino

import java.time.Instant
import java.time.ZoneId

class MyUtility {

    companion object {

        fun getDateAndTime(): Long {        // This function returns the current date and time in milliseconds
            return Instant.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

    }

}