package xyz.xszq.nereides.message

import xyz.xszq.nereides.payload.user.GuildMember
import xyz.xszq.nereides.payload.user.GuildUser

class GuildAt(val member: GuildMember, val user: GuildUser): At(user.id)