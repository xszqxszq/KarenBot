package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class MaimaiBinding(openId: EntityID<String>): Entity<String>(openId) {
    companion object : EntityClass<String, MaimaiBinding>(MaimaiBindings)
    var openId      by MaimaiBindings.id
    var type        by MaimaiBindings.type
    var credential  by MaimaiBindings.credential
}