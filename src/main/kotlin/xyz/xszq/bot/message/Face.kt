package xyz.xszq.bot.message

import xyz.xszq.bot.util.json
import xyz.xszq.bot.payload.FaceExt
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * QQ 表情
 * @param type 表情类型
 * @param id 表情 ID
 * @param name 表情名
 */
class Face(
    val type: Int,
    val id: Int,
    val name: String
): MessageElement {
    override val content: String = "[$name]"
    @OptIn(ExperimentalEncodingApi::class)
    override fun toString(): String {
        val ext = Base64.encode(json.encodeToString(FaceExt(name)).toByteArray(charset = Charsets.UTF_8))
        return "<faceType=$type,faceId=\"$id\",ext=\"$ext\">"
    }
}