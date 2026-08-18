package xyz.xszq.bot.meme.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface MemeOption {
    val name: String
    val description: String?
    @SerialName("parser_flags")
    val parserFlags: ParserFlags

    @Serializable
    @SerialName("boolean")
    data class BooleanOption(
        override val name: String,
        override val description: String ?= null,
        @SerialName("parser_flags")
        override val parserFlags: ParserFlags,
        val default: Boolean ?= null
    ): MemeOption
    @Serializable
    @SerialName("string")
    data class StringOption(
        override val name: String,
        override val description: String ?= null,
        @SerialName("parser_flags")
        override val parserFlags: ParserFlags,
        val default: String ?= null,
        val choices: List<String> ?= null,
    ): MemeOption
    @Serializable
    @SerialName("integer")
    data class IntegerOption(
        override val name: String,
        override val description: String ?= null,
        @SerialName("parser_flags")
        override val parserFlags: ParserFlags,
        val default: Int ?= null,
        val minimum: Int ?= null,
        val maximum: Int ?= null,
    ): MemeOption
    @Serializable
    @SerialName("float")
    data class FloatOption(
        override val name: String,
        override val description: String ?= null,
        @SerialName("parser_flags")
        override val parserFlags: ParserFlags,
        val default: Float ?= null,
        val minimum: Float ?= null,
        val maximum: Float ?= null,
    ): MemeOption
    companion object {
        val module = SerializersModule {
            polymorphic(MemeOption::class) {
                subclass(BooleanOption::class)
                subclass(StringOption::class)
                subclass(IntegerOption::class)
                subclass(FloatOption::class)
            }
        }
    }
}