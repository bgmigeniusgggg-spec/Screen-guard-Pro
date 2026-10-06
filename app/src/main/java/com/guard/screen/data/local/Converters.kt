package com.guard.screen.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromMap(value: Map<String, String>?): String? {
        return value?.let { json.encodeToString(mapSerializer, it) }
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, String>? {
        return value?.let {
            try {
                json.decodeFromString(mapSerializer, it)
            } catch (_: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromList(value: List<String>?): String? {
        return value?.let { json.encodeToString(listSerializer, it) }
    }

    @TypeConverter
    fun toList(value: String?): List<String>? {
        return value?.let {
            try {
                json.decodeFromString(listSerializer, it)
            } catch (_: Exception) {
                null
            }
        }
    }
}
