package com.liam.kaptalismusaufhalter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Settings ADD COLUMN strictness TEXT NOT NULL DEFAULT 'NORMAL'")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ExcludedApp` (`packageName` TEXT NOT NULL, PRIMARY KEY(`packageName`))")
    }
}

@Database(
    entities = [Wish::class, PiggyBankEntry::class, Settings::class, ExcludedApp::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wishDao(): WishDao
    abstract fun piggyBankDao(): PiggyBankDao
    abstract fun settingsDao(): SettingsDao
    abstract fun excludedAppDao(): ExcludedAppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "impulskauf.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }
    }
}
