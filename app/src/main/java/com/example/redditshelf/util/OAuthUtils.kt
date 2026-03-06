package com.example.redditshelf.util

import android.net.Uri
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

object OAuthUtils {
    fun randomState(): String = UUID.randomUUID().toString()

    fun authorizationHeader(clientId: String): String {
        val raw = "$clientId:"
        val base64 = Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
        return "Basic $base64"
    }

    fun buildAuthorizeUri(
        clientId: String,
        redirectUri: String,
        state: String,
        scope: String
    ): Uri {
        return Uri.Builder()
            .scheme("https")
            .authority("www.reddit.com")
            .path("/api/v1/authorize.compact")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", state)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("duration", "permanent")
            .appendQueryParameter("scope", scope)
            .build()
    }

    fun nowPlusSeconds(seconds: Long): Long = System.currentTimeMillis() + (seconds * 1000L)

    fun stableFolderId(name: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(name.lowercase().trim().toByteArray())
        return digest.take(10).joinToString("") { "%02x".format(it) }
    }
}
