package com.example.llmchat.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores per-profile API keys in an EncryptedSharedPreferences file so that
 * secrets are never written to disk in plain text.
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun getApiKey(profileId: Long): String = withContext(Dispatchers.IO) {
        prefs.getString(keyFor(profileId), "").orEmpty()
    }

    suspend fun setApiKey(profileId: Long, value: String) {
        withContext(Dispatchers.IO) {
            prefs.edit().putString(keyFor(profileId), value).apply()
        }
    }

    suspend fun clearApiKey(profileId: Long) {
        withContext(Dispatchers.IO) {
            prefs.edit().remove(keyFor(profileId)).apply()
        }
    }

    private fun keyFor(profileId: Long): String = "api_key_$profileId"

    private companion object {
        const val PREFS_NAME = "llmchat_secure_prefs"
    }
}
