package com.example.ciclomenstrual.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "pill_intakes",
    primaryKeys = ["regimen_id", "scheduled_date"],
    foreignKeys = [
        ForeignKey(
            entity = RoomContraceptiveRegimen::class,
            parentColumns = ["id"],
            childColumns = ["regimen_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RoomPillIntake(
    @ColumnInfo(name = "regimen_id", index = true) val regimenId: Long,
    @ColumnInfo(name = "scheduled_date") val scheduledDate: Long,
    @ColumnInfo(name = "pill_number") val pillNumber: Int,
    val status: String,
    @ColumnInfo(name = "taken_at") val takenAt: Long? = null,
    val source: String,
)
