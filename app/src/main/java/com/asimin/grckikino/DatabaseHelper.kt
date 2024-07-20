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
        const val COLUMN_ID = "id"
        const val COLUMN_TALON_TIME = "talonTime"
        const val COLUMN_DRAW_ID = "drawId"
        const val COLUMN_DRAW_TIME = "drawTime"
        const val COLUMN_SELECTED_NUMBERS = "selectedNumbers"
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

}