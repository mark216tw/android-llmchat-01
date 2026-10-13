package com.example.llmchat.data.repository

import com.example.llmchat.data.datastore.SettingsDataStore
import com.example.llmchat.data.local.ProfileDao
import com.example.llmchat.data.local.ProfileEntity
import com.example.llmchat.data.security.CryptoManager
import com.example.llmchat.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val cryptoManager: CryptoManager,
    private val settingsDataStore: SettingsDataStore
) {
    fun observeProfiles(): Flow<List<Profile>> =
        profileDao.observeAll().map { entities ->
            val profiles = ArrayList<Profile>(entities.size)
            for (entity in entities) {
                profiles.add(entity.toDomain(cryptoManager.getApiKey(entity.id)))
            }
            profiles
        }

    suspend fun getProfile(id: Long): Profile? {
        val entity = profileDao.getById(id) ?: return null
        return entity.toDomain(cryptoManager.getApiKey(entity.id))
    }

    /**
     * Inserts or updates a profile and persists its API key in encrypted
     * storage. Returns the profile id.
     */
    suspend fun save(profile: Profile): Long {
        val now = System.currentTimeMillis()
        val id = if (profile.id == 0L) {
            val newId = profileDao.insert(
                ProfileEntity(
                    name = profile.name,
                    baseUrl = profile.baseUrl,
                    model = profile.model,
                    systemPrompt = profile.systemPrompt,
                    temperature = profile.temperature,
                    maxTokens = profile.maxTokens,
                    contextWindow = profile.contextWindow,
                    thinkingMode = profile.thinkingMode.name,
                    reasoningEffort = profile.reasoningEffort.name,
                    createdAt = now
                )
            )
            newId
        } else {
            val existingCreatedAt = profileDao.getById(profile.id)?.createdAt ?: now
            profileDao.update(
                ProfileEntity(
                    id = profile.id,
                    name = profile.name,
                    baseUrl = profile.baseUrl,
                    model = profile.model,
                    systemPrompt = profile.systemPrompt,
                    temperature = profile.temperature,
                    maxTokens = profile.maxTokens,
                    contextWindow = profile.contextWindow,
                    thinkingMode = profile.thinkingMode.name,
                    reasoningEffort = profile.reasoningEffort.name,
                    createdAt = existingCreatedAt
                )
            )
            profile.id
        }
        cryptoManager.setApiKey(id, profile.apiKey)
        return id
    }

    suspend fun delete(id: Long) {
        profileDao.deleteById(id)
        cryptoManager.clearApiKey(id)
        if (currentProfileIdBlocking() == id) {
            val next = profileDao.getAll().firstOrNull()
            settingsDataStore.setCurrentProfileId(next?.id ?: SettingsDataStore.NO_PROFILE)
        }
    }

    fun observeCurrentProfileId(): Flow<Long> = settingsDataStore.currentProfileId

    suspend fun setCurrentProfileId(id: Long) = settingsDataStore.setCurrentProfileId(id)

    suspend fun getCurrentProfile(): Profile? {
        val id = currentProfileIdBlocking()
        if (id == SettingsDataStore.NO_PROFILE) return null
        return getProfile(id)
    }

    private suspend fun currentProfileIdBlocking(): Long =
        settingsDataStore.currentProfileId.first()
}
