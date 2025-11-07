package com.group8.focuslock_application

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class TimeLimitDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "focuslock_timelimit.db"
        const val DATABASE_VERSION = 1
        const val TABLE_APP_LIMITS = "app_limits"
        const val COLUMN_ID = "id"
        const val COLUMN_PACKAGE_NAME = "package_name"
        const val COLUMN_APP_NAME = "app_name"
        const val COLUMN_DAILY_LIMIT_MINUTES = "daily_limit_minutes"
        const val COLUMN_CHILD_ID = "child_id"
        const val COLUMN_CREATED_DATE = "created_date"
        const val COLUMN_ENABLED = "enabled"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_APP_LIMITS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PACKAGE_NAME TEXT NOT NULL UNIQUE,
                $COLUMN_APP_NAME TEXT NOT NULL,
                $COLUMN_DAILY_LIMIT_MINUTES INTEGER NOT NULL,
                $COLUMN_CHILD_ID TEXT NOT NULL,
                $COLUMN_CREATED_DATE TEXT NOT NULL,
                $COLUMN_ENABLED INTEGER DEFAULT 1
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APP_LIMITS")
        onCreate(db)
    }

    fun saveAppLimit(packageName: String, appName: String, limitMinutes: Int, childId: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_PACKAGE_NAME, packageName)
            put(COLUMN_APP_NAME, appName)
            put(COLUMN_DAILY_LIMIT_MINUTES, limitMinutes)
            put(COLUMN_CHILD_ID, childId)
            put(COLUMN_CREATED_DATE, System.currentTimeMillis().toString())
            put(COLUMN_ENABLED, 1)
        }

        val result = db.insertWithOnConflict(
            TABLE_APP_LIMITS,
            null,
            contentValues,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return result != -1L
    }

    fun getAppLimit(packageName: String): AppLimit? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_APP_LIMITS,
            null,
            "$COLUMN_PACKAGE_NAME = ?",
            arrayOf(packageName),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            AppLimit(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                packageName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PACKAGE_NAME)),
                appName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_NAME)),
                dailyLimitMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAILY_LIMIT_MINUTES)),
                childId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHILD_ID)),
                enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1
            ).also { cursor.close() }
        } else {
            cursor.close()
            null
        }
    }

    fun getChildAppLimits(childId: String): List<AppLimit> {
        val db = this.readableDatabase
        val limits = mutableListOf<AppLimit>()
        val cursor = db.query(
            TABLE_APP_LIMITS,
            null,
            "$COLUMN_CHILD_ID = ?",
            arrayOf(childId),
            null,
            null,
            null
        )

        while (cursor.moveToNext()) {
            limits.add(
                AppLimit(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    packageName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PACKAGE_NAME)),
                    appName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APP_NAME)),
                    dailyLimitMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAILY_LIMIT_MINUTES)),
                    childId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHILD_ID)),
                    enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1
                )
            )
        }
        cursor.close()
        return limits
    }

    fun deleteAppLimit(packageName: String): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_APP_LIMITS, "$COLUMN_PACKAGE_NAME = ?", arrayOf(packageName)) > 0
    }

    fun toggleAppLimit(packageName: String, enabled: Boolean): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_ENABLED, if (enabled) 1 else 0)
        }
        return db.update(TABLE_APP_LIMITS, contentValues, "$COLUMN_PACKAGE_NAME = ?", arrayOf(packageName)) > 0
    }
}

data class AppLimit(
    val id: Int,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val childId: String,
    val enabled: Boolean
)