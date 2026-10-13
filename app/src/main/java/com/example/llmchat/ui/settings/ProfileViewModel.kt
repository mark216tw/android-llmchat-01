package com.example.llmchat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llmchat.data.repository.ChatRepository
import com.example.llmchat.data.repository.ProfileRepository
import com.example.llmchat.domain.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Loading : ConnectionTestState
    data class Success(val models: List<String>) : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentProfileId: StateFlow<Long> = profileRepository.observeCurrentProfileId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1L)

    private val _testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val testState: StateFlow<ConnectionTestState> = _testState.asStateFlow()

    suspend fun getProfile(id: Long): Profile? = profileRepository.getProfile(id)

    fun save(profile: Profile, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = profileRepository.save(profile)
            onSaved(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { profileRepository.delete(id) }
    }

    fun selectCurrent(id: Long) {
        viewModelScope.launch { profileRepository.setCurrentProfileId(id) }
    }

    fun testConnection(baseUrl: String, apiKey: String) {
        if (baseUrl.isBlank()) {
            _testState.value = ConnectionTestState.Error("請先輸入 Base URL")
            return
        }
        _testState.value = ConnectionTestState.Loading
        viewModelScope.launch {
            val result = chatRepository.fetchModels(baseUrl, apiKey)
            _testState.value = result.fold(
                onSuccess = { ConnectionTestState.Success(it) },
                onFailure = { ConnectionTestState.Error(it.message ?: "連線失敗") }
            )
        }
    }

    fun resetTestState() {
        _testState.value = ConnectionTestState.Idle
    }
}
