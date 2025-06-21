package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable
import xyz.xszq.bot.event.ReplyAble

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
            embeddedEvent: ReplyAble ?= null,
            builder: KeyboardBuilder.() -> Unit
        ) = KeyboardBuilder()
            .apply(builder)
            .build()
            .apply {
                embeddedEvent ?.let {
                    var counter = 2
                    content.rows.forEach { row ->
                        row.buttons.forEach { button ->
                            if (button.action.type == Action.CALLBACK)
                                button.action.data ?.let {
                                    button.action.data = "${embeddedEvent.id}:$counter:${button.action.data}"
                                    counter += 1
                                }
                        }
                    }
                }
            }
    }
}