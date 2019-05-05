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

@Database(entities = [SavedCard::class, SavedKey::class], version = 3, exportSchema = true)
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
                        .addMigrations(object : Migration(2, 3) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                // Re-create tables with new NOT NULL `id` column.
                                database.beginTransaction()
                                try {
                                    database.execSQL("""
                                    CREATE TABLE `cards_new` (
                                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                        `type` TEXT NOT NULL,
                                        `serial` TEXT NOT NULL,
                                        `data` TEXT NOT NULL,
                                        `scanned_at` INTEGER NOT NULL
                                    );
                                    """.trimIndent())

                                    database.execSQL("""
                                    CREATE TABLE `keys_new` (
                                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                        `card_id` TEXT NOT NULL,
                                        `card_type` TEXT NOT NULL,
                                        `key_data` TEXT NOT NULL,
                                        `created_at` INTEGER NOT NULL
                                    );
                                    """.trimIndent())

                                    database.execSQL("""
                                    INSERT INTO `cards_new` (type, serial, data, scanned_at)
                                        SELECT type, serial, data, scanned_at FROM cards;
                                    """.trimIndent())

                                    database.execSQL("""
                                    INSERT INTO `keys_new` (card_id, card_type, key_data, created_at)
                                        SELECT card_id, card_type, key_data, created_at FROM keys;
                                    """.trimIndent())

                                    database.execSQL("DROP TABLE `cards`;")
                                    database.execSQL("DROP TABLE `keys`;")

                                    database.execSQL("ALTER TABLE `cards_new` RENAME TO `cards`;")
                                    database.execSQL("ALTER TABLE `keys_new` RENAME TO `keys`;")

                                    database.setTransactionSuccessful()
                                } finally {
                                    database.endTransaction()
                                }
                            }
                        })
                        .build()
    }
}
