package xyz.xszq.bot.message

import kotlinx.serialization.encodeToString
import xyz.xszq.bot.json
import xyz.xszq.bot.payload.FaceExt
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * QQ Internal Face.
 * @param type Face type.
 * @param id Face ID.
 * @param name Face name.
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