package xyz.xszq.bot.payload.markdown

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Keyboard(
    val content: InlineKeyboard
) {
    fun text() = content.rows.joinToString(", ") {
        "[" + it.buttons.joinToString(", ") { button ->
            button.renderData.label
        } + "]"
    }
    class KeyboardRowBuilder {
        val data = mutableListOf<Button>()
        fun button(id: String, renderData: RenderData, action: Action) =
            data.add(Button(id, renderData, action))
        fun build() = InlineKeyboardRow(data)

        fun KeyboardRowBuilder.at(
            label: String,
            data: String,
            enter: Boolean = false,
            anchor: Int? = null,
            style: Int = RenderData.BLUE,
            id: String = UUID.randomUUID().toString().take(8)
        ) = button(
            id = id,
            renderData = RenderData(
                label = label, visitedLabel = label, style = style
            ),
            action = Action(
                type = Action.AT, data = data, permission = Permission(Permission.EVERYONE),
                enter = enter, anchor = anchor
            )
        )

        fun KeyboardRowBuilder.link(
            label: String,
            url: String,
            style: Int = RenderData.BLUE,
            id: String = UUID.randomUUID().toString().take(8)
        ) = button(
            id = id,
            renderData = RenderData(
                label = label, visitedLabel = label, style = style
            ),
            action = Action(
                type = Action.LINK, data = url, permission = Permission(Permission.EVERYONE)
            )
        )

        fun KeyboardRowBuilder.callBack(
            label: String,
            data: String,
            style: Int = RenderData.BLUE,
            id: String = UUID.randomUUID().toString().take(8)
        ) = button(
            id = id,
            renderData = RenderData(
                label = label, visitedLabel = label, style = style
            ),
            action = Action(
                type = Action.CALLBACK, data = data, permission = Permission(Permission.EVERYONE)
            )
        )
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