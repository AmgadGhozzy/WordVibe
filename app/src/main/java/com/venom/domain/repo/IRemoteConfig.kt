package com.venom.domain.repo

/**
 * Interface for fetching and accessing remote configuration values.
 */
interface IRemoteConfig {
    /**
     * Fetch and activate remote config values.
     */
    suspend fun fetchAndActivate()

    /**
     * Get a boolean value from remote config or the providing default.
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    /**
     * Get a long value from remote config or the providing default.
     */
    fun getLong(key: String, defaultValue: Long): Long

    /**
     * Get a double value from remote config or the providing default.
     */
    fun getDouble(key: String, defaultValue: Double): Double

    /**
     * Get a string value from remote config or the providing default.
     */
    fun getString(key: String, defaultValue: String): String
}
