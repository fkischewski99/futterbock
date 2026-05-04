package data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.futterbock.app.MainActivity

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = MainActivity.appContext
    val dbFile = context.getDatabasePath("futterbock.db")
    return Room.databaseBuilder<AppDatabase>(context, dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
}
