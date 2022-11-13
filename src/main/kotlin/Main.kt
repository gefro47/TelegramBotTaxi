import clientlogic.ClientBotLogic
import clientlogic.Clients
import database.Drivers
import database.Orders
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import driverlogic.DriverBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


suspend fun main(args: Array<String>) {
    Database.connect("jdbc:h2:file:${DATABASE_PATH}", driver = "org.h2.Driver", user = "root", password = "")
    runBlocking {
        transaction {
            SchemaUtils.create(Drivers)
            SchemaUtils.create(Clients)
            SchemaUtils.create(Orders)
        }
    }

    val clientBot = ClientBotLogic()
    telegramBotWithBehaviourAndLongPolling(Hack_Taxi_Client_Bot, CoroutineScope(Dispatchers.IO)) {
        println(getMe())
        clientBot.context = this
        clientBot.clientBot()
    }

    val driverBot = DriverBot()
    driverBot.clientBot = clientBot
    clientBot.driverBot = driverBot
    telegramBotWithBehaviourAndLongPolling(Hack_Taxi_Drivers_Bot, CoroutineScope(Dispatchers.Default)) {
        driverBot.context = this
        driverBot.driverBot()
    }.second.join()

}