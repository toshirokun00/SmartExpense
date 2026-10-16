package com.smartexpense.database

import androidx.room.TypeConverter
import com.smartexpense.database.entity.SyncStatus

class SyncStatusConverter {

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }
}