package com.example.pratica14


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarcadorDao {

    @Insert
    suspend fun insertar(marcador: MarcadorEntity)

    @Query("SELECT * FROM marcadores")
    fun obtenerMarcadores(): Flow<List<MarcadorEntity>>
}