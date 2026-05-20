package xyz.xszq.bot.message

import xyz.xszq.bot.json
import xyz.xszq.bot.payload.FaceExt
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun String.parseFaceElements(): List<MessageElement> {
    val faceRegex = Regex("""<faceType=(\d+),faceId="(\d+)",ext="([^"]+)">""")
    val elements = mutableListOf<MessageElement>()
    var index = 0
    faceRegex.findAll(this).forEach { match ->
        val matchStart = match.range.first
        val matchEnd = match.range.last + 1
        if (index < matchStart) {
            elements.add(PlainText(substring(index, matchStart)))
        }
        val faceType = match.groups[1]?.value?.toInt() ?: 0
        val faceId = match.groups[2]?.value?.toInt() ?: 0
        val base64Ext = match.groups[3]?.value ?: ""
        val ext = json.decodeFromString<FaceExt>(
            Base64.decode(base64Ext).toString(Charsets.UTF_8)
        )
        elements.add(Face(type = faceType, id = faceId, name = ext.text))
        index = matchEnd
    }
    if (index < length) {
        elements.add(PlainText(substring(index)))
    }
    return elements
}
