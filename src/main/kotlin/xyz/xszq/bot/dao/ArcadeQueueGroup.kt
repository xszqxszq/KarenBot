package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ArcadeQueueGroup(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<ArcadeQueueGroup>(ArcadeQueueGroups)
    var name    by ArcadeQueueGroups.name
    val centers by ArcadeCenter referrersOn ArcadeCenters.group
}