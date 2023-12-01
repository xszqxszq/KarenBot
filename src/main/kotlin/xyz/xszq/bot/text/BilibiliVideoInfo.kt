package xyz.xszq.bot.text

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class BilibiliVideoInfo(
    val bvid: String,
    val aid: Long,
    val videos: Int,
    val tid: Int,
    val tname: String,
    val copyright: Int,
    val pic: String,
    val title: String,
    val pubdate: Long,
    val ctime: Long,
    val desc: String,
    val state: Int,
    val duration: Int,
    val owner: BilibiliUser,
    val stat: JsonObject,
    val dynamic: String,
    val cid: Long
)