package com.group8.focuslock_application

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ParentAppLimitsActivity : AppCompatActivity() {

    private lateinit var dbHelper: TimeLimitDatabase
    private lateinit var appNameEditText: EditText
    private lateinit var packageNameEditText: EditText
    private lateinit var limitMinutesEditText: EditText
    private lateinit var childSpinner: Spinner
    private lateinit var appLimitsList: ListView
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button
    private lateinit var enableSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.parent_app_limits_page)

        dbHelper = TimeLimitDatabase(this)
        initializeViews()
        setupListeners()
        loadAppLimits()
    }

    private fun initializeViews() {
        appNameEditText = findViewById(R.id.appNameInput)
        packageNameEditText = findViewById(R.id.packageNameInput)
        limitMinutesEditText = findViewById(R.id.limitMinutesInput)
        childSpinner = findViewById(R.id.childSpinner)
        appLimitsList = findViewById(R.id.appLimitsList)
        saveButton = findViewById(R.id.saveLimitButton)
        deleteButton = findViewById(R.id.deleteLimitButton)
        enableSwitch = findViewById(R.id.enableLimitSwitch)
    }

    private fun setupListeners() {
        saveButton.setOnClickListener { saveAppLimit() }
        deleteButton.setOnClickListener { deleteSelectedLimit() }

        appLimitsList.setOnItemClickListener { _, _, position, _ ->
            val item = appLimitsList.getItemAtPosition(position) as HashMap<String, String>
            loadLimitIntoForm(item)
        }

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            val packageName = packageNameEditText.text.toString()
            if (packageName.isNotEmpty()) {
                dbHelper.toggleAppLimit(packageName, isChecked)
            }
        }
    }

    private fun saveAppLimit() {
        val appName = appNameEditText.text.toString().trim()
        val packageName = packageNameEditText.text.toString().trim()
        val limitMinutes = limitMinutesEditText.text.toString().toIntOrNull() ?: 0
        val childId = "current_child"

        if (appName.isEmpty() || packageName.isEmpty() || limitMinutes <= 0) {
            showMessage("Please fill all fields with valid values")
            return
        }

        val success = dbHelper.saveAppLimit(packageName, appName, limitMinutes, childId)
        if (success) {
            showMessage("App limit saved: $appName - $limitMinutes min/day")
            clearForm()
            loadAppLimits()
        } else {
            showMessage("Failed to save app limit")
        }
    }

    private fun deleteSelectedLimit() {
        val packageName = packageNameEditText.text.toString().trim()
        if (packageName.isEmpty()) {
            showMessage("Select an app limit to delete")
            return
        }

        val success = dbHelper.deleteAppLimit(packageName)
        if (success) {
            showMessage("App limit deleted")
            clearForm()
            loadAppLimits()
        } else {
            showMessage("Failed to delete app limit")
        }
    }

    private fun loadAppLimits() {
        val childId = "current_child"
        val limits = dbHelper.getChildAppLimits(childId)

        val data = limits.map { limit ->
            hashMapOf(
                "app_name" to limit.appName,
                "limit" to "${limit.dailyLimitMinutes} min",
                "status" to if (limit.enabled) "Active" else "Inactive",
                "package" to limit.packageName
            )
        }

        val adapter = SimpleAdapter(
            this,
            data,
            android.R.layout.simple_list_item_2,
            arrayOf("app_name", "limit"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        appLimitsList.adapter = adapter
    }

    private fun loadLimitIntoForm(item: HashMap<String, String>) {
        val packageName = item["package"] ?: return
        val limit = dbHelper.getAppLimit(packageName)

        if (limit != null) {
            appNameEditText.setText(limit.appName)
            packageNameEditText.setText(limit.packageName)
            limitMinutesEditText.setText(limit.dailyLimitMinutes.toString())
            enableSwitch.isChecked = limit.enabled
        }
    }

    private fun clearForm() {
        appNameEditText.text.clear()
        packageNameEditText.text.clear()
        limitMinutesEditText.text.clear()
        enableSwitch.isChecked = true
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}