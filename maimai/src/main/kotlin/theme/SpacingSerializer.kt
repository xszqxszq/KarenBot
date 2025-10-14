package xyz.xszq.bot.theme

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SpacingSerializer: KSerializer<Spacing> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Spacing", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Spacing) {
        encoder.encodeString("${value.top} ${value.bottom} ${value.left} ${value.right}")
    }

    override fun deserialize(decoder: Decoder): Spacing {
        val list = decoder.decodeString().split(" ").map { it.toInt() }
        return when (list.size) {
            0 ->  Spacing(0)
            1 -> Spacing(list[0])
            4 -> Spacing(list[0], list[1], list[2], list[3])
            else -> throw IllegalStateException()
        }
    }

}