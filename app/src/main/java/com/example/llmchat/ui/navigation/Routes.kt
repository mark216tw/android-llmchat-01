package com.example.llmchat.ui.navigation

object Routes {
    const val CONVERSATIONS = "conversations"
    const val PROFILES = "profiles"
    const val PROFILE_EDIT = "profile_edit/{profileId}"
    const val CHAT = "chat/{conversationId}"
    const val DEBUG = "debug"
    const val SETTINGS = "settings"

    const val ARG_PROFILE_ID = "profileId"
    const val ARG_CONVERSATION_ID = "conversationId"

    /** Sentinel id for a not-yet-persisted (draft) conversation. */
    const val NEW_CONVERSATION_ID = 0L

    fun profileEdit(profileId: Long): String = "profile_edit/$profileId"
    fun chat(conversationId: Long): String = "chat/$conversationId"
}
