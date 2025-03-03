package modules

import data.EventRepository
import data.FireBaseRepository
import org.koin.dsl.module

val dataModules = module {
    single<EventRepository> { FireBaseRepository(get()) }
}