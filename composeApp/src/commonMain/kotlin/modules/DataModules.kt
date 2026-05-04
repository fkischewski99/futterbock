package modules

import data.DelegatingRepository
import data.EventRepository
import data.local.AppDatabase
import data.local.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val dataModules = module {
    single<AppDatabase> {
        getDatabaseBuilder()
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single<EventRepository> {
        DelegatingRepository(get(), get(), get())
    }
}