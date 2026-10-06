package com.guard.screen.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @TypeConverter
    fun fromMap(value: Map<String, String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, String>? {
        return value?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (_: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toList(value: String?): List<String>? {
        return value?.let {
            try {
                json.decodeFromString<List<String>>(it)
            } catch (_: Exception) {
                null
            }
        }
    }
}
