package driverlogic

import data.TelegramUser

data class DriverUser(
    val telegramUser: TelegramUser,
    val driverState: DriverState,
)
