package com.ixam97.carStatsViewer.database.batteryHealth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BatteryHealthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: BatteryHealthRecord): Long

    @Update
    fun updateRecord(record: BatteryHealthRecord)

    @Query("SELECT * FROM BatteryHealthRecord WHERE is_valid = 1 ORDER BY epoch_time ASC")
    fun getAllValidRecords(): List<BatteryHealthRecord>

    @Query("SELECT * FROM BatteryHealthRecord ORDER BY epoch_time DESC LIMIT :limit")
    fun getLatestRecords(limit: Int): List<BatteryHealthRecord>

    @Query("SELECT * FROM BatteryHealthRecord ORDER BY epoch_time DESC")
    fun getAllRecords(): List<BatteryHealthRecord>

    @Query("DELETE FROM BatteryHealthRecord WHERE id = :id")
    fun deleteRecordById(id: Long): Int

    @Query("DELETE FROM BatteryHealthRecord")
    fun clearAll(): Int
}
