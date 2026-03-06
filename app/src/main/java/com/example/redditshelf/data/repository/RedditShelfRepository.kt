package com.example.redditshelf.data.repository

import com.example.redditshelf.data.local.AppStorage
import com.example.redditshelf.data.model.AppConfig
import com.example.redditshelf.data.model.ItemAnnotation
import com.example.redditshelf.data.model.SavedItem
import com.example.redditshelf.data.model.SavedThingData
import com.example.redditshelf.data.model.ShelfData
import com.example.redditshelf.data.model.ShelfFolder
import com.example.redditshelf.data.model.TokenBundle
import com.example.redditshelf.data.model.UserProfile
import com.example.redditshelf.data.model.toSavedItem
import com.example.redditshelf.data.network.RedditApi
import com.example.redditshelf.util.OAuthUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class RedditShelfRepository(
    private val storage: AppStorage,
    private val api: RedditApi = RedditApi()
) {
    val configFlow: Flow<AppConfig> = storage.configFlow
    val tokenFlow: Flow<TokenBundle?> = storage.tokenFlow
    val shelfFlow: Flow<ShelfData> = storage.shelfFlow

    suspend fun saveConfig(config: AppConfig) = storage.saveConfig(config)

    suspend fun clearSession() = storage.saveTokens(null)

    suspend fun exchangeCodeForTokens(code: String): TokenBundle {
        val config = storage.configFlow.first()
        require(config.clientId.isNotBlank()) { "Client ID is required before login." }
        val response = api.exchangeCode(
            clientId = config.clientId,
            authorizationHeader = OAuthUtils.authorizationHeader(config.clientId),
            code = code,
            redirectUri = config.redirectUri,
            userAgent = config.userAgent
        )
        val bundle = TokenBundle(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken.orEmpty(),
            expiresAtEpochMillis = OAuthUtils.nowPlusSeconds(response.expiresIn),
            scope = response.scope
        )
        storage.saveTokens(bundle)
        return bundle
    }

    suspend fun getFreshToken(): TokenBundle {
        val config = storage.configFlow.first()
        val existing = storage.tokenFlow.first() ?: error("No active session")
        if (existing.expiresAtEpochMillis > System.currentTimeMillis() + 30_000) return existing
        require(existing.refreshToken.isNotBlank()) { "Session expired and no refresh token is available." }
        val response = api.refreshAccessToken(
            authorizationHeader = OAuthUtils.authorizationHeader(config.clientId),
            refreshToken = existing.refreshToken,
            userAgent = config.userAgent
        )
        val refreshed = existing.copy(
            accessToken = response.accessToken,
            expiresAtEpochMillis = OAuthUtils.nowPlusSeconds(response.expiresIn),
            scope = response.scope
        )
        storage.saveTokens(refreshed)
        return refreshed
    }

    suspend fun getMe(): UserProfile {
        val config = storage.configFlow.first()
        val token = getFreshToken()
        return api.getMe(token.accessToken, config.userAgent)
    }

    suspend fun getSavedPage(username: String, after: String? = null): Pair<List<SavedItem>, String?> {
        val config = storage.configFlow.first()
        val token = getFreshToken()
        val response = api.getSaved(
            accessToken = token.accessToken,
            userAgent = config.userAgent,
            username = username,
            after = after
        )
        val items = response.data.children.map { wrapper -> wrapper.data.toSavedItem() }
        return items to response.data.after
    }

    suspend fun addFolder(name: String) {
        val shelf = storage.shelfFlow.first()
        val id = OAuthUtils.stableFolderId(name)
        if (shelf.folders.any { it.id == id || it.name.equals(name, ignoreCase = true) }) return
        storage.saveShelf(shelf.copy(folders = shelf.folders + ShelfFolder(id = id, name = name.trim())))
    }

    suspend fun updateAnnotation(annotation: ItemAnnotation) {
        val shelf = storage.shelfFlow.first()
        val updated = shelf.annotations.filterNot { it.thingId == annotation.thingId } + annotation
        storage.saveShelf(shelf.copy(annotations = updated))
    }

    suspend fun getAnnotation(thingId: String): ItemAnnotation? {
        return storage.shelfFlow.first().annotations.firstOrNull { it.thingId == thingId }
    }
}
