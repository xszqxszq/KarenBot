package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class QARule(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<QARule>(QARules) {
        const val INCLUDE = 0
        const val EQUAL = 1
        const val REGEX = 2
        const val ANY = 3
        const val ALL = 4
        const val PIC_INCLUDE = -1
        const val PIC_ALL = -2
        const val PIC_ANY = -3
    }
    var openid by QARules.openid
    var type by QARules.type
    var rule by QARules.rule
    var reply by QARules.reply
    var creator by QARules.creator
    var createTime by QARules.createTime
}