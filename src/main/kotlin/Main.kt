import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import driverlogic.driverBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


suspend fun main(args: Array<String>) {

    telegramBotWithBehaviourAndLongPolling(Hack_Taxi_Client_Bot, CoroutineScope(Dispatchers.IO)) {
        println(getMe())
//        Todo logic
    }
    telegramBotWithBehaviourAndLongPolling(Hack_Taxi_Drivers_Bot, CoroutineScope(Dispatchers.Default)) {
        driverBot(this)
    }.second.join()

}