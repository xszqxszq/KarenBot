package xyz.xszq.bot.theme

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ShadowSerializer: KSerializer<Shadow> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Shadow", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Shadow) {
        encoder.encodeString("${value.size} ${value.opacity} ${value.color}")
    }

    override fun deserialize(decoder: Decoder): Shadow {
        val list = decoder.decodeString().split(" ")
        return when (list.size) {
            0 -> Shadow(0.0, 1.0, "#000000")
            1 -> Shadow(list[0].toDouble(), 1.0, "#000000")
            2 -> Shadow(list[0].toDouble(), list[1].toDouble(), "#000000")
            3 -> Shadow(list[0].toDouble(), list[1].toDouble(), list[2])
            else -> throw IllegalStateException()
        }
    }

}