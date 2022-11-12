package data

data class TelegramUser(
    val id: Long,
    val username: String?,
    val is_bot: Boolean,
    val first_name: String,
    val last_name: String,
)