package com.example.triviaapp.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

class DatabaseHelper(private val context: Context?): SQLiteOpenHelper(context, "QuestionDB.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        try {
            db?.execSQL("CREATE TABLE questions(id INTEGER PRIMARY KEY AUTOINCREMENT, question TEXT, answer TEXT)")
            db?.execSQL("CREATE TABLE settings(id INTEGER PRIMARY KEY AUTOINCREMENT, setting TEXT, value TEXT)")
            val contentValues = ContentValues()
            contentValues.put("setting", "online")
            contentValues.put("value", "true")
            db?.insert("settings", null, contentValues)

            Toast.makeText(
                context, "Tables created",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context, e.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS questions")
        onCreate(db)
    }

}