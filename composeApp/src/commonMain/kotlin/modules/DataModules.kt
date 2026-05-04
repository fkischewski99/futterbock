package modules

import data.DelegatingRepository
import data.EventRepository
import data.FireBaseRepository
import data.local.AppDatabase
import data.local.RoomRepository
import data.local.getDatabaseBuilder
import data.sync.NetworkMonitorImpl
import data.sync.NetworkMonitor
import data.sync.OfflineFirstRepository
import data.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import services.login.FirebaseLoginAndRegister
import services.login.OfflineLoginAndRegister

val dataModules = module {
    single<AppDatabase> {
        getDatabaseBuilder()
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single { FirebaseLoginAndRegister() }
    single { OfflineLoginAndRegister() }
    single { FireBaseRepository(get<FirebaseLoginAndRegister>()) }
    single { RoomRepository(get(), get<OfflineLoginAndRegister>()) }
    single<NetworkMonitor> { NetworkMonitorImpl() }
    single { SyncManager(get(), get(), get()) }
    single { OfflineFirstRepository(get(), get(), get(), get()) }

    single<EventRepository> {
        DelegatingRepository(get(), get(), get(), get())
    }
}