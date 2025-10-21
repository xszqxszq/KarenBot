package xyz.xszq.shinobu

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BorderSerializer: KSerializer<Border> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Border", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Border) {
        encoder.encodeString("${value.size} ${value.color}")
    }

    override fun deserialize(decoder: Decoder): Border {
        val list = decoder.decodeString().split(" ")
        return when (list.size) {
            0 -> Border(0.0, "#ffffff")
            1 -> Border(list[0].toDouble(), "#ffffff")
            2 -> Border(list[0].toDouble(), list[1])
            else -> throw IllegalStateException()
        }
    }

}