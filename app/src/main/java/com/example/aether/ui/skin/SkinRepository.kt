package com.example.aether.ui.skin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.skinDataStore: DataStore<Preferences> by preferencesDataStore(name = "aether_skin")

class SkinRepository(context: Context) {

    private val dataStore = context.applicationContext.skinDataStore

    val selectedSkinId: Flow<SkinId> = dataStore.data.map { prefs ->
        SkinId.fromStorage(prefs[KEY] ?: SkinId.DEFAULT.name)
    }

    suspend fun setSkin(id: SkinId) {
        dataStore.edit { prefs ->
            prefs[KEY] = id.name
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("selected_skin_id")
    }
}
