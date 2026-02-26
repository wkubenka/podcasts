package com.astute.podcasts.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val playbackSpeedKey = floatPreferencesKey("playback_speed")

    val playbackSpeed: Flow<Float> = dataStore.data.map { prefs ->
        prefs[playbackSpeedKey] ?: 1.0f
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { prefs ->
            prefs[playbackSpeedKey] = speed
        }
    }
}
