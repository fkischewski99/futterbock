package modules

import org.koin.dsl.module
import services.shoppingList.CalculateShoppingList
import services.ChangeDateOfEvent
import services.login.LoginAndRegister
import services.login.FirebaseLoginAndRegister
import services.materiallist.CalculateMaterialList
import services.pdfService.PdfServiceModule

val serviceModules = module {
    single<LoginAndRegister> { FirebaseLoginAndRegister() }
    single { CalculateShoppingList(get()) }
    single { CalculateMaterialList(get()) }
    single { PdfServiceModule(get(), get()) }
    single { ChangeDateOfEvent(get()) }
}