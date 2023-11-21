package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ArcadeCenterQueueGroup(openId: EntityID<String>): Entity<String>(openId) {
    companion object : EntityClass<String, ArcadeCenterQueueGroup>(ArcadeCenterQueueGroups)
    var openId  by ArcadeCenterQueueGroups.id
    var group   by ArcadeCenterQueueGroups.group
}