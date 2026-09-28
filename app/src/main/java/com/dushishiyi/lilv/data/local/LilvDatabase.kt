package com.dushishiyi.lilv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SnapshotEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LilvDatabase : RoomDatabase() {

    abstract fun snapshotDao(): SnapshotDao

    companion object {
        @Volatile private var INSTANCE: LilvDatabase? = null

        fun get(context: Context): LilvDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LilvDatabase::class.java,
                    "lilv.db",
                ).fallbackToDestructiveMigration().build().also {
                    INSTANCE = it
                }
            }
    }
}
