package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getCompanyWhatsAppNumbers(): List<String> {
        val numbersString = prefs.getString("company_whatsapp_numbers", "") ?: ""
        return if (numbersString.isBlank()) emptyList() else numbersString.split(",")
    }

    fun saveCompanyWhatsAppNumbers(numbers: List<String>) {
        prefs.edit().putString("company_whatsapp_numbers", numbers.joinToString(",")).apply()
    }

    fun getWhatsAppIdleOpacity(): Float {
        return prefs.getFloat("whatsapp_idle_opacity", 0.4f)
    }

    fun saveWhatsAppIdleOpacity(opacity: Float) {
        prefs.edit().putFloat("whatsapp_idle_opacity", opacity).apply()
    }

    fun getRegisteredCustomers(): List<RegisteredCustomer> {
        val jsonStr = prefs.getString("registered_customers", "") ?: ""
        return if (jsonStr.isBlank()) emptyList() else {
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<RegisteredCustomer>>(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun saveRegisteredCustomers(customers: List<RegisteredCustomer>) {
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(customers)
        prefs.edit().putString("registered_customers", jsonStr).apply()
    }

    fun getSavedManagementEmail(): String? {
        return prefs.getString("mgmt_email", null)
    }

    fun isSavedUserAdmin(): Boolean {
        return prefs.getBoolean("mgmt_is_admin", false)
    }

    fun isRememberMeActive(): Boolean {
        return prefs.getBoolean("mgmt_remember_me", false)
    }

    fun saveManagementSession(email: String, isAdmin: Boolean, rememberMe: Boolean) {
        prefs.edit()
            .putString("mgmt_email", email)
            .putBoolean("mgmt_is_admin", isAdmin)
            .putBoolean("mgmt_remember_me", rememberMe)
            .apply()
    }

    fun clearManagementSession() {
        prefs.edit()
            .remove("mgmt_email")
            .remove("mgmt_is_admin")
            .remove("mgmt_remember_me")
            .apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    fun getThemeName(): String {
        return prefs.getString("theme_name", "ROYAL_BLUE") ?: "ROYAL_BLUE"
    }

    fun saveThemeName(themeName: String) {
        prefs.edit().putString("theme_name", themeName).apply()
    }

    fun getDashboardLayout(key: String = "dashboard_layout"): DashboardLayout? {
        val jsonStr = prefs.getString(key, null) ?: return null
        return try {
            kotlinx.serialization.json.Json.decodeFromString<DashboardLayout>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    fun saveDashboardLayout(layout: DashboardLayout, key: String = "dashboard_layout") {
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(layout)
        prefs.edit().putString(key, jsonStr).apply()
    }
}
