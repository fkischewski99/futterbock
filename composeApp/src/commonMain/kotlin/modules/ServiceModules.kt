package modules

import org.koin.dsl.module
import services.SeedDataService
import services.shoppingList.CalculateShoppingList
import services.ChangeDateOfEvent
import services.login.LoginAndRegister
import services.login.DelegatingLoginAndRegister
import services.materiallist.CalculateMaterialList
import services.pdfService.PdfServiceModule
import services.event.ParticipantCanEatRecipe
import services.update.UpdateChecker

val serviceModules = module {
    single<LoginAndRegister> { DelegatingLoginAndRegister(get(), get(), get()) }
    single { SeedDataService(get(), get(), get()) }
    single { CalculateShoppingList(get()) }
    single { CalculateMaterialList(get()) }
    single { PdfServiceModule(get(), get()) }
    single { ChangeDateOfEvent(get()) }
    single { ParticipantCanEatRecipe(get()) }
    single { UpdateChecker() }
}