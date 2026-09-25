package com.venom.data.repo

import com.venom.domain.repo.IRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * NOT STARTED: remote config backend. Returns compiled defaults always; the fetch is a
 * no-op by design until a gated post-MVP decision wires Firebase Remote Config.
 */
class StubRemoteConfig @Inject constructor() : IRemoteConfig {

    private val store = mutableMapOf<String, String>()

    override suspend fun fetchAndActivate() {}

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return store[key]?.toBooleanStrictOrNull() ?: defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return store[key]?.toLongOrNull() ?: defaultValue
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return store[key]?.toDoubleOrNull() ?: defaultValue
    }

    override fun getString(key: String, defaultValue: String): String {
        return store[key] ?: defaultValue
    }
}
