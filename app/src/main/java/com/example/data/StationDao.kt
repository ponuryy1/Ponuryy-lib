package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY isFavorite DESC, name ASC")
    fun getAllStations(): Flow<List<Station>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: Station)

    @Update
    suspend fun updateStation(station: Station)

    @Delete
    suspend fun deleteStation(station: Station)

    @Query("SELECT * FROM stations WHERE id = :id")
    suspend fun getStationById(id: Int): Station?
}
