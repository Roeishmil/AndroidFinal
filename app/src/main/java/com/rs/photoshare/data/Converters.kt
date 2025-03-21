package com.rs.photoshare.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Converter class to handle conversion between List<String> and JSON String for Room.
class Converters {
    // Gson instance for JSON operations.
    private val gson = Gson()

    // Converts a List<String> to a JSON string.
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    // Converts a JSON string back to a List<String>.
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
