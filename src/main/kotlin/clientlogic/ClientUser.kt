package clientlogic

import data.TelegramUser

data class ClientUser(
    val telegramData: TelegramUser,
    val dialogState: String
)
