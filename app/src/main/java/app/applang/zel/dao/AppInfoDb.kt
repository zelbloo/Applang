package app.applang.zel.dao

import androidx.room.Database
import androidx.room.RoomDatabase

// The table is a disposable "recently opened" cache, so there is no schema history to export.
@Database(entities = [AppInfoEntity::class], version = 1, exportSchema = false)
abstract class AppInfoDb : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
}