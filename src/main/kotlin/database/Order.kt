package database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

enum class OrderState {
    SEARCHING_DRIVER,
    IN_PROGRESS,
    CANCELED,
    COMPLETED,
}

object Orders : IntIdTable() {
    val clientChatId = long("client_chat_id")
    val driverChatId = long("driver_chat_id").nullable()
    val orderState = enumeration<OrderState>("order_state")
    val potentialDrivers = integer("potential_drivers")
}

class Order(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Order>(Orders)

    var clientChatId by Orders.clientChatId
    var driverChatId by Orders.driverChatId
    var orderState by Orders.orderState
    var potentialDrivers by Orders.potentialDrivers
}

suspend fun addOrder(_clientChatId: Long, _potentialDrivers: Int): Order {
    return transaction {
        return@transaction Order.new {
            clientChatId = _clientChatId
            driverChatId = null
            orderState = OrderState.SEARCHING_DRIVER
            potentialDrivers = _potentialDrivers
        }
    }
}
