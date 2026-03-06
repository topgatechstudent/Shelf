package com.example.redditshelf.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.redditshelf.data.model.AppConfig
import com.example.redditshelf.data.model.ShelfData
import com.example.redditshelf.data.model.TokenBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "reddit_shelf")

class AppStorage(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val config = stringPreferencesKey("config")
        val tokens = stringPreferencesKey("tokens")
        val shelf = stringPreferencesKey("shelf")
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        prefs[Keys.config]?.let { json.decodeFromString<AppConfig>(it) } ?: AppConfig()
    }

    val tokenFlow: Flow<TokenBundle?> = context.dataStore.data.map { prefs ->
        prefs[Keys.tokens]?.let { json.decodeFromString<TokenBundle>(it) }
    }

    val shelfFlow: Flow<ShelfData> = context.dataStore.data.map { prefs ->
        prefs[Keys.shelf]?.let { json.decodeFromString<ShelfData>(it) } ?: ShelfData()
    }

    suspend fun saveConfig(config: AppConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.config] = json.encodeToString(AppConfig.serializer(), config)
        }
    }

    suspend fun saveTokens(tokens: TokenBundle?) {
        context.dataStore.edit { prefs ->
            if (tokens == null) {
                prefs.remove(Keys.tokens)
            } else {
                prefs[Keys.tokens] = json.encodeToString(TokenBundle.serializer(), tokens)
            }
        }
    }

    suspend fun saveShelf(shelfData: ShelfData) {
        context.dataStore.edit { prefs ->
            prefs[Keys.shelf] = json.encodeToString(ShelfData.serializer(), shelfData)
        }
    }
}
