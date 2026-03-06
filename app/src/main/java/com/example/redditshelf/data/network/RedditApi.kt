package com.example.redditshelf.data.network

import com.example.redditshelf.data.model.OAuthTokenResponse
import com.example.redditshelf.data.model.SavedListingResponse
import com.example.redditshelf.data.model.UserProfile
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

class RedditApi {
    private val json = Json { ignoreUnknownKeys = true }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun exchangeCode(
        clientId: String,
        authorizationHeader: String,
        code: String,
        redirectUri: String,
        userAgent: String
    ): OAuthTokenResponse {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .build()

        val request = Request.Builder()
            .url("https://www.reddit.com/api/v1/access_token")
            .header("Authorization", authorizationHeader)
            .header("User-Agent", userAgent)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Token exchange failed: ${response.code} $payload" }
            return json.decodeFromString(OAuthTokenResponse.serializer(), payload)
        }
    }

    fun refreshAccessToken(
        authorizationHeader: String,
        refreshToken: String,
        userAgent: String
    ): OAuthTokenResponse {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url("https://www.reddit.com/api/v1/access_token")
            .header("Authorization", authorizationHeader)
            .header("User-Agent", userAgent)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Token refresh failed: ${response.code} $payload" }
            return json.decodeFromString(OAuthTokenResponse.serializer(), payload)
        }
    }

    fun getMe(accessToken: String, userAgent: String): UserProfile {
        val request = Request.Builder()
            .url("https://oauth.reddit.com/api/v1/me")
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", userAgent)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Profile fetch failed: ${response.code} $payload" }
            return json.decodeFromString(UserProfile.serializer(), payload)
        }
    }

    fun getSaved(
        accessToken: String,
        userAgent: String,
        username: String,
        after: String? = null,
        limit: Int = 50
    ): SavedListingResponse {
        val base = "https://oauth.reddit.com/user/$username/saved".toHttpUrl()
        val urlBuilder = base.newBuilder()
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("raw_json", "1")
        if (!after.isNullOrBlank()) urlBuilder.addQueryParameter("after", after)

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", userAgent)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Saved listing fetch failed: ${response.code} $payload" }
            return json.decodeFromString(SavedListingResponse.serializer(), payload)
        }
    }
}
