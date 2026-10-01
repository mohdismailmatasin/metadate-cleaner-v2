package com.example.aimetadatacleaner

import android.app.Application
import com.example.aimetadatacleaner.data.database.AppDatabase
import com.example.aimetadatacleaner.data.repository.MetadataCleanerRepository

class MetaCleanApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MetadataCleanerRepository(this, database.cleanedRecordDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
