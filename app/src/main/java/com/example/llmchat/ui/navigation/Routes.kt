package com.example.llmchat.ui.navigation

import android.net.Uri

object Routes {
    const val CONVERSATIONS = "conversations"
    const val PROFILES = "profiles"
    const val PROFILE_EDIT = "profile_edit/{profileId}"
    const val CHAT = "chat/{conversationId}?title={title}"
    const val DEBUG = "debug"
    const val SETTINGS = "settings"

    const val ARG_PROFILE_ID = "profileId"
    const val ARG_CONVERSATION_ID = "conversationId"
    const val ARG_CONVERSATION_TITLE = "title"

    /** Sentinel id for a not-yet-persisted (draft) conversation. */
    const val NEW_CONVERSATION_ID = 0L

    fun profileEdit(profileId: Long): String = "profile_edit/$profileId"

    /**
     * Builds the chat route. [title] is an optional hint so the app bar can show
     * the correct title on the very first frame instead of flashing "新對話".
     */
    fun chat(conversationId: Long, title: String? = null): String =
        if (title.isNullOrBlank()) {
            "chat/$conversationId"
        } else {
            "chat/$conversationId?title=${Uri.encode(title)}"
        }
}
