package com.example.streamguidemobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.streamGuideSettings by preferencesDataStore(name = "streamguide_settings")

data class AppSettings(
    val showLogos: Boolean = true,
    val compactList: Boolean = false,
    val autoResumeLastChannel: Boolean = true,
    val hardwareDecoding: Boolean = true
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.streamGuideSettings.data.map { values ->
        AppSettings(
            showLogos = values[Keys.showLogos] ?: true,
            compactList = values[Keys.compactList] ?: false,
            autoResumeLastChannel = values[Keys.autoResumeLastChannel] ?: true,
            hardwareDecoding = values[Keys.hardwareDecoding] ?: true
        )
    }

    suspend fun setShowLogos(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.showLogos] = value }
    }

    suspend fun setCompactList(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.compactList] = value }
    }

    suspend fun setAutoResumeLastChannel(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.autoResumeLastChannel] = value }
    }

    suspend fun setHardwareDecoding(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.hardwareDecoding] = value }
    }

    private object Keys {
        val showLogos = booleanPreferencesKey("show_logos")
        val compactList = booleanPreferencesKey("compact_list")
        val autoResumeLastChannel = booleanPreferencesKey("auto_resume_last_channel")
        val hardwareDecoding = booleanPreferencesKey("hardware_decoding")
    }
}