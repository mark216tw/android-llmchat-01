package com.example.llmchat.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llmchat.data.datastore.SettingsDataStore
import com.example.llmchat.data.repository.ConversationRepository
import com.example.llmchat.data.repository.ProfileRepository
import com.example.llmchat.domain.model.Conversation
import com.example.llmchat.domain.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> =
        conversationRepository.observeConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentProfile: StateFlow<Profile?> =
        profileRepository.observeCurrentProfileId()
            .flatMapLatest { id ->
                if (id == SettingsDataStore.NO_PROFILE) {
                    flowOf(null)
                } else {
                    flowOf(profileRepository.getProfile(id))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val hasProfile: StateFlow<Boolean> = currentProfile
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun deleteConversation(id: Long) {
        viewModelScope.launch { conversationRepository.deleteConversation(id) }
    }

    fun renameConversation(id: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { conversationRepository.renameConversation(id, trimmed) }
    }
}
