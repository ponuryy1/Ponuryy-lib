package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class Station(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val streamUrl: String,
    val iconUrl: String,
    val genre: String,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
)
