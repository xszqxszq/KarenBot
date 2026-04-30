package xyz.xszq.bot.payload.llm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object MessageContentSerializer : KSerializer<MessageContent> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MessageContent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MessageContent) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is MessageContentSingle ->
                jsonEncoder.encodeJsonElement(JsonPrimitive(value.text))
            is MessageContentMulti ->
                jsonEncoder.encodeJsonElement(
                    JsonArray(value.parts.map { Json.encodeToJsonElement(it) })
                )
        }
    }

    override fun deserialize(decoder: Decoder): MessageContent {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> MessageContentSingle(element.content)
            is JsonArray -> MessageContentMulti(
                element.map { Json.decodeFromJsonElement<ContentPart>(it) }
            )
            else -> MessageContentSingle(element.toString())
        }
    }
}
