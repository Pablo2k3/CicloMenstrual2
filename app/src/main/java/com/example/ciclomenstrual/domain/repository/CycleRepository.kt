package com.example.ciclomenstrual.domain.repository

import com.example.ciclomenstrual.domain.model.Cycle

interface CycleRepository {
    suspend fun getAll(): List<Cycle>
    suspend fun insert(cycle: Cycle)
    suspend fun updateEnd(startDate: Long, endDate: Long)
    suspend fun delete(cycle: Cycle)
    suspend fun deleteByStartDate(startDate: Long)
    suspend fun exists(startDate: Long): Boolean
}
