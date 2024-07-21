package com.asimin.grckikino

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "grckiKino.db"
        const val DATABASE_VERSION = 2
        const val TABLE_TALON = "talon"
        const val COLUMN_ID = "id"                  // column index is 0
        const val COLUMN_TALON_TIME = "talonTime"   // column index is 1
        const val COLUMN_DRAW_ID = "drawId"         // column index is 2
        const val COLUMN_DRAW_TIME = "drawTime"     // column index is 3
        const val COLUMN_SELECTED_NUMBERS = "selectedNumbers"   // column index is 4
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TALON_TABLE = ("CREATE TABLE " + TABLE_TALON + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TALON_TIME + " INTEGER,"
                + COLUMN_DRAW_ID + " INTEGER,"
                + COLUMN_DRAW_TIME + " INTEGER,"
                + COLUMN_SELECTED_NUMBERS + " TEXT" + ")")
        db.execSQL(CREATE_TALON_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TALON")
        onCreate(db)
    }

    fun insertTalon(draw: Draw, talonPaymentTime: Long) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TALON_TIME, talonPaymentTime)
            put(COLUMN_DRAW_ID, draw.drawId)
            put(COLUMN_DRAW_TIME, draw.drawTime)
            put(COLUMN_SELECTED_NUMBERS, draw.selectedNumbers.joinToString(","))
        }
        db.insert(TABLE_TALON, null, values)
        db.close()
    }

    fun importTalonsFromDatabaseToHistory(): List<Talon> {
        val talons = mutableListOf<Talon>()
        val draws = mutableListOf<Draw>()
        val db = this.readableDatabase
        var previousTalonPaymentTime = -1L
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TALON ORDER BY $COLUMN_TALON_TIME ASC", null)
        if (cursor.moveToFirst()) {
            do {
                val talonPaymentTime = cursor.getLong(1)    //column name is COLUMN_TALON_TIME
                val drawId = cursor.getInt(2)               //column name is COLUMN_DRAW_ID
                val drawTime = cursor.getLong(3)            //column name is COLUMN_DRAW_TIME
                val selectedNumbers = cursor.getString(4)   //column name is COLUMN_SELECTED_NUMBERS
                    .split(",")
                    .map { it.trim() }
                    .map { it.toInt() }
                val draw = Draw(drawId, drawTime, selectedNumbers, null)
                if (previousTalonPaymentTime != talonPaymentTime && previousTalonPaymentTime != -1L) {
                    talons.add(Talon(previousTalonPaymentTime, draws.size, ArrayList(draws)))
                    draws.clear()
                }
                draws.add(draw)
                previousTalonPaymentTime = talonPaymentTime
            } while (cursor.moveToNext())
            // Add the last set of draws to talons list
            if (draws.isNotEmpty()) {
                talons.add(Talon(previousTalonPaymentTime, draws.size, ArrayList(draws)))
            }
        }
        cursor.close()
        db.close()
        return talons
    }

}