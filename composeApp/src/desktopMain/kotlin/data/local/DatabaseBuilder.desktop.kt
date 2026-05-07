package data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val osName = System.getProperty("os.name").lowercase()
    val dbDir = when {
        osName.contains("mac") ->
            File(System.getProperty("user.home"), "Library/Application Support/Futterbock")
        osName.contains("win") ->
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Futterbock")
        else ->
            File(
                System.getenv("XDG_DATA_HOME")
                    ?: "${System.getProperty("user.home")}/.local/share",
                "futterbock"
            )
    }
    dbDir.mkdirs()
    val dbFile = File(dbDir, "futterbock.db")
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
}
