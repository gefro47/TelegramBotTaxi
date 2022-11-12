import clientlogic.clientBot
import database.Drivers
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import driverlogic.driverBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


suspend fun main(args: Array<String>) {
    Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver", user = "root", password = "")
    transaction {
        SchemaUtils.create(Drivers)
    }

    telegramBotWithBehaviourAndLongPolling(Hack_Taxi_Client_Bot, CoroutineScope(Dispatchers.IO)) {
        println(getMe())
        clientBot()
    }.second.join()

//    telegramBotWithBehaviourAndLongPolling(Hack_Taxi_Drivers_Bot, CoroutineScope(Dispatchers.Default)) {
//        driverBot()
//    }.second.join()

}