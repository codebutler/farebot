package com.codebutler.farebot.persist.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codebutler.farebot.persist.db.dao.SavedCardDao
import com.codebutler.farebot.persist.db.dao.SavedKeyDao
import com.codebutler.farebot.persist.db.model.SavedCard
import com.codebutler.farebot.persist.db.model.SavedKey

private const val DATABASE_NAME = "farebot.db"

@Database(entities = [SavedCard::class, SavedKey::class], version = 2, exportSchema = false)
@TypeConverters(FareBotDbConverters::class)
abstract class FareBotDb : RoomDatabase() {
    abstract fun savedCardDao(): SavedCardDao
    abstract fun savedKeyDao(): SavedKeyDao

    companion object {
        @Volatile private var instance: FareBotDb? = null

        fun getInstance(context: Context): FareBotDb = instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context): FareBotDb =
                Room.databaseBuilder(context, FareBotDb::class.java, DATABASE_NAME)
                        .addMigrations(object : Migration(1, 2) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                // Migration from Sqldelight to Room. Nothing to change.
                            }
                        })
                        .build()
    }
}
