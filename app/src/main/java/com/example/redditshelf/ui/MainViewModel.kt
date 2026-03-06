package com.example.redditshelf.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.redditshelf.data.model.AppConfig
import com.example.redditshelf.data.model.ItemAnnotation
import com.example.redditshelf.data.model.ItemStatus
import com.example.redditshelf.data.model.SavedItem
import com.example.redditshelf.data.model.ShelfData
import com.example.redditshelf.data.model.TokenBundle
import com.example.redditshelf.data.model.UserProfile
import com.example.redditshelf.data.repository.RedditShelfRepository
import com.example.redditshelf.util.OAuthUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: RedditShelfRepository
) : ViewModel() {

    private val stateValue = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = combine(
        stateValue,
        repository.configFlow,
        repository.tokenFlow,
        repository.shelfFlow
    ) { ui, config, token, shelf ->
        ui.copy(config = config, token = token, shelfData = shelf)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    private var pagingCursor: String? = null
    private var profile: UserProfile? = null
    private var pendingState: String? = null

    fun saveConfig(clientId: String, redirectUri: String, userAgent: String) {
        viewModelScope.launch {
            repository.saveConfig(
                AppConfig(
                    clientId = clientId.trim(),
                    redirectUri = redirectUri.trim(),
                    userAgent = userAgent.trim()
                )
            )
            stateValue.value = stateValue.value.copy(message = "Configuration saved.")
        }
    }

    fun buildAuthorizeUrl(): String? {
        val config = state.value.config
        if (config.clientId.isBlank() || config.redirectUri.isBlank() || config.userAgent.isBlank()) {
            stateValue.value = stateValue.value.copy(error = "Client ID, redirect URI, and user-agent are required.")
            return null
        }
        val localState = OAuthUtils.randomState()
        pendingState = localState
        return OAuthUtils.buildAuthorizeUri(
            clientId = config.clientId,
            redirectUri = config.redirectUri,
            state = localState,
            scope = "identity history save read"
        ).toString()
    }

    fun handleAuthRedirect(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = android.net.Uri.parse(uriString)
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            stateValue.value = stateValue.value.copy(error = "Authorization failed: $error")
            return
        }
        val returnedState = uri.getQueryParameter("state")
        if (pendingState != null && returnedState != pendingState) {
            stateValue.value = stateValue.value.copy(error = "OAuth state mismatch.")
            return
        }
        val code = uri.getQueryParameter("code") ?: return
        exchangeCode(code)
    }

    private fun exchangeCode(code: String) {
        viewModelScope.launch {
            runCatching {
                stateValue.value = stateValue.value.copy(isLoading = true, error = null, message = "Exchanging authorization code…")
                repository.exchangeCodeForTokens(code)
                profile = repository.getMe()
                pagingCursor = null
                val firstPage = repository.getSavedPage(profile!!.name, null)
                pagingCursor = firstPage.second
                stateValue.value = stateValue.value.copy(
                    isLoading = false,
                    profile = profile,
                    items = firstPage.first,
                    message = "Connected as u/${profile!!.name}"
                )
            }.onFailure { throwable ->
                stateValue.value = stateValue.value.copy(isLoading = false, error = throwable.message ?: "Authentication failed")
            }
        }
    }

    fun refreshShelf() {
        viewModelScope.launch {
            runCatching {
                stateValue.value = stateValue.value.copy(isLoading = true, error = null)
                profile = repository.getMe()
                val firstPage = repository.getSavedPage(profile!!.name, null)
                pagingCursor = firstPage.second
                stateValue.value = stateValue.value.copy(
                    isLoading = false,
                    profile = profile,
                    items = firstPage.first,
                    message = "Shelf refreshed."
                )
            }.onFailure {
                stateValue.value = stateValue.value.copy(isLoading = false, error = it.message ?: "Refresh failed")
            }
        }
    }

    fun loadMore() {
        val username = state.value.profile?.name ?: return
        val cursor = pagingCursor ?: return
        viewModelScope.launch {
            runCatching {
                stateValue.value = stateValue.value.copy(isPaging = true, error = null)
                val nextPage = repository.getSavedPage(username, cursor)
                pagingCursor = nextPage.second
                stateValue.value = stateValue.value.copy(
                    isPaging = false,
                    items = state.value.items + nextPage.first
                )
            }.onFailure {
                stateValue.value = stateValue.value.copy(isPaging = false, error = it.message ?: "Could not load more items")
            }
        }
    }

    fun addFolder(name: String) {
        viewModelScope.launch {
            repository.addFolder(name)
            stateValue.value = stateValue.value.copy(message = "Folder added.")
        }
    }

    fun saveAnnotation(item: SavedItem, folderIds: Set<String>, tags: Set<String>, note: String, status: ItemStatus) {
        viewModelScope.launch {
            repository.updateAnnotation(
                ItemAnnotation(
                    thingId = item.thingId,
                    folderIds = folderIds,
                    tags = tags,
                    note = note,
                    status = status
                )
            )
            stateValue.value = stateValue.value.copy(message = "Saved organization changes.")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.clearSession()
            pagingCursor = null
            profile = null
            stateValue.value = MainUiState(config = state.value.config, shelfData = state.value.shelfData, message = "Signed out.")
        }
    }

    fun clearTransientMessage() {
        stateValue.value = stateValue.value.copy(error = null, message = null)
    }

    class Factory(private val repository: RedditShelfRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
    }
}

data class MainUiState(
    val config: AppConfig = AppConfig(),
    val token: TokenBundle? = null,
    val shelfData: ShelfData = ShelfData(),
    val profile: UserProfile? = null,
    val items: List<SavedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isPaging: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
