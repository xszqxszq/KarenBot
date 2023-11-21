package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ArcadeCenter(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<ArcadeCenter>(ArcadeCenters)
    var group       by ArcadeCenters.group
    var name        by ArcadeCenters.name
    var abbr        by ArcadeCenters.abbr
    var value       by ArcadeCenters.value
    var modified    by ArcadeCenters.modified
}