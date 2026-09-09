package xyz.xszq.bot.meme.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * 表情包模板参数
 */
@Serializable
sealed interface MemeOption {
    val name: String
    val description: String?
    @SerialName("parser_flags")
    val parserFlags: ParserFlags

    /**
     * Boolean 参数
     */
    @Serializable
    @SerialName("boolean")
    data class BooleanOption(
        override val name: String,
        override val description: String ?= null,
        @SerialName("parser_flags")
        override val parserFlags: ParserFlags,
        val default: Boolean ?= null
    ): MemeOption
    /**
     * String 参数
     */
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
    /**
     * Int 参数
     */
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
    /**
     * Float 参数
     */
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
        /**
         * 序列化各类型参数
         */
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