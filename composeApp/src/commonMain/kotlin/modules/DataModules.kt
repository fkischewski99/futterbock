package modules

import data.AppModeHolder
import data.AppModePreferences
import data.DelegatingRepository
import data.EventRepository
import data.FireBaseRepository
import data.local.AppDatabase
import data.local.RoomRepository
import data.local.getDatabaseBuilder
import data.sync.NetworkMonitorImpl
import data.sync.NetworkMonitor
import data.sync.InitialSyncService
import data.sync.OfflineFirstRepository
import data.sync.SyncManager
import org.koin.dsl.module
import services.login.FirebaseLoginAndRegister
import services.login.LoginAndRegister
import services.login.OfflineLoginAndRegister

val dataModules = module {
    single { AppModePreferences() }
    single { AppModeHolder(get<AppModePreferences>().getAppMode()) }

    single<AppDatabase> {
        getDatabaseBuilder().build()
    }

    single { FirebaseLoginAndRegister() }
    single { OfflineLoginAndRegister() }
    single { FireBaseRepository(get<FirebaseLoginAndRegister>()) }
    single { RoomRepository(get(), get<LoginAndRegister>()) }
    single<NetworkMonitor> { NetworkMonitorImpl() }
    single { SyncManager(get(), get(), get()) }
    single { InitialSyncService(get(), get(), get(), get(), get()) }
    single { OfflineFirstRepository(get(), get(), get(), get()) }

    single<EventRepository> {
        DelegatingRepository(get(), get(), get(), get())
    }
}