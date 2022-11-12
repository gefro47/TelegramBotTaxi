package clientlogic

import data.TelegramUser
import data.Trip

data class ClientUser(
    val telegramData: TelegramUser,
    val dialogState: String,
    val currentTrip: Trip?
)
