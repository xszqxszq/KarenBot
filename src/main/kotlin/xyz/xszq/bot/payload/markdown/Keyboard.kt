package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable

@Serializable
data class Keyboard(
    val content: InlineKeyboard
) {
    fun text() = content.rows.joinToString(", ") { "[" + it.buttons.joinToString(", ") { it.renderData.label } + "]" }
    class KeyboardRowBuilder {
        val data = mutableListOf<Button>()
        fun button(id: String, renderData: RenderData, action: Action) =
            data.add(Button(id, renderData, action))
        fun build() = InlineKeyboardRow(data)
    }
    class KeyboardBuilder {
        val data = mutableListOf<InlineKeyboardRow>()
        fun row(block: KeyboardRowBuilder.() -> Unit) =
            KeyboardRowBuilder()
                .apply(block).build()
                .also { data.add(it) }
        fun build() = Keyboard(InlineKeyboard(data))
    }
    companion object {
        fun create(
            builder: KeyboardBuilder.() -> Unit
        ) = KeyboardBuilder().apply(builder).build()
    }
}