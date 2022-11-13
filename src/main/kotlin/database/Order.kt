package database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

enum class OrderState {
    SEARCHING_DRIVER,
    IN_PROGRESS,
    CANCELED,
    COMPLETED,
}

object Orders : IntIdTable() {
    val orderUuid = uuid("order_uuid")
    val clientChatId = long("client_chat_id")
    val clientMessageId = long("client_message_id")
    val driverChatId = long("driver_chat_id").nullable()
    val orderState = enumeration<OrderState>("order_state")
    val potentialDrivers = integer("potential_drivers")
    val locationMessageId = long("location_message_id").nullable()
}

class Order(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Order>(Orders)

    var orderUuid by Orders.orderUuid
    var clientChatId by Orders.clientChatId
    var driverChatId by Orders.driverChatId
    var clientMessageId by Orders.clientMessageId
    var orderState by Orders.orderState
    var potentialDrivers by Orders.potentialDrivers
    var locationMessageId by Orders.locationMessageId
}

suspend fun addOrder(_orderUuid: UUID, _clientChatId: Long, _clientMessageId: Long, _potentialDrivers: Int): Order {
    return transaction {
        return@transaction Order.new {
            orderUuid = _orderUuid
            clientChatId = _clientChatId
            clientMessageId = _clientMessageId
            driverChatId = null
            orderState = OrderState.SEARCHING_DRIVER
            potentialDrivers = _potentialDrivers
            locationMessageId = null
        }
    }
}

suspend fun getOrder(_orderUuid: UUID): Order? {
    return transaction {
        return@transaction Order.find(Orders.orderUuid eq _orderUuid).firstOrNull()
    }
}

suspend fun getOrderByDriverId(_driverChatId: Long): Order? {
    return transaction {
        return@transaction Order.find(Orders.driverChatId eq _driverChatId).firstOrNull()
    }
}
