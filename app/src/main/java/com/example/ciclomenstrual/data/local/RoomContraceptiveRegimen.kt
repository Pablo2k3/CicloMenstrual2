package com.example.ciclomenstrual.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contraceptive_regimens")
data class RoomContraceptiveRegimen(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long? = null,
    @ColumnInfo(name = "dose_hour") val doseHour: Int = 14,
    @ColumnInfo(name = "dose_minute") val doseMinute: Int = 0,
    @ColumnInfo(name = "active_days") val activeDays: Int = 21,
    @ColumnInfo(name = "placebo_days") val placeboDays: Int = 7,
)
