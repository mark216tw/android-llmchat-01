package com.example.llmchat.ui.debug

import androidx.lifecycle.ViewModel
import com.example.llmchat.debug.DebugLogEntry
import com.example.llmchat.debug.DebugLogStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DebugLogViewModel @Inject constructor(
    private val store: DebugLogStore
) : ViewModel() {

    val entries: StateFlow<List<DebugLogEntry>> = store.entries

    val enabled: StateFlow<Boolean> = store.enabled

    fun setEnabled(value: Boolean) = store.setEnabled(value)

    fun clear() = store.clear()
}
