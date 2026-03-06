package com.example.redditshelf.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val clientId: String = "",
    val redirectUri: String = DEFAULT_REDIRECT_URI,
    val userAgent: String = DEFAULT_USER_AGENT
) {
    companion object {
        const val DEFAULT_REDIRECT_URI = "redditshelf://auth"
        const val DEFAULT_USER_AGENT = "android:com.example.redditshelf:v1.0 (by /u/your_reddit_username)"
    }
}

@Serializable
data class TokenBundle(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAtEpochMillis: Long = 0L,
    val scope: String = ""
)

@Serializable
data class UserProfile(
    val name: String,
    @SerialName("icon_img") val iconImg: String? = null,
    @SerialName("total_karma") val totalKarma: Int = 0
)

@Serializable
data class SavedListingResponse(
    val kind: String,
    val data: ListingData
)

@Serializable
data class ListingData(
    val after: String? = null,
    val before: String? = null,
    val children: List<ThingWrapper> = emptyList()
)

@Serializable
data class ThingWrapper(
    val kind: String,
    val data: SavedThingData
)

@Serializable
data class SavedThingData(
    val id: String,
    val name: String,
    val title: String? = null,
    val selftext: String? = null,
    val body: String? = null,
    val subreddit: String? = null,
    val author: String? = null,
    val permalink: String? = null,
    val url: String? = null,
    @SerialName("created_utc") val createdUtc: Double? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("link_title") val linkTitle: String? = null,
    @SerialName("link_permalink") val linkPermalink: String? = null,
    @SerialName("score") val score: Int? = null,
    @SerialName("num_comments") val numComments: Int? = null
)

@Serializable
data class SavedItem(
    val thingId: String,
    val name: String,
    val type: SavedItemType,
    val title: String,
    val subtitle: String,
    val subreddit: String,
    val author: String,
    val permalink: String,
    val externalUrl: String? = null,
    val createdUtc: Double? = null,
    val score: Int? = null,
    val numComments: Int? = null
)

@Serializable
enum class SavedItemType {
    POST,
    COMMENT
}

@Serializable
data class ShelfFolder(
    val id: String,
    val name: String
)

@Serializable
data class ItemAnnotation(
    val thingId: String,
    val folderIds: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val note: String = "",
    val status: ItemStatus = ItemStatus.INBOX
)

@Serializable
enum class ItemStatus {
    INBOX,
    TO_READ,
    RESOLVED,
    ARCHIVED
}

@Serializable
data class ShelfData(
    val folders: List<ShelfFolder> = listOf(
        ShelfFolder(id = "default-read-later", name = "Read Later"),
        ShelfFolder(id = "default-reference", name = "Reference")
    ),
    val annotations: List<ItemAnnotation> = emptyList()
)

@Serializable
data class OAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("scope") val scope: String,
    @SerialName("refresh_token") val refreshToken: String? = null
)

fun SavedThingData.toSavedItem(): SavedItem {
    val isComment = body != null
    return SavedItem(
        thingId = id,
        name = name,
        type = if (isComment) SavedItemType.COMMENT else SavedItemType.POST,
        title = if (isComment) (linkTitle ?: "Comment") else (title ?: "Untitled post"),
        subtitle = if (isComment) (body ?: "") else (selftext ?: ""),
        subreddit = subreddit.orEmpty(),
        author = author.orEmpty(),
        permalink = permalink ?: linkPermalink.orEmpty(),
        externalUrl = url,
        createdUtc = createdUtc,
        score = score,
        numComments = numComments
    )
}
