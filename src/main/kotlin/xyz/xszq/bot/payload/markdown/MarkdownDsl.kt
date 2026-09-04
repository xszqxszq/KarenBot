package xyz.xszq.bot.payload.markdown

import xyz.xszq.bot.message.Markdown

/**
 * Markdown 消息构建 DSL
 */
@Suppress("unused")
class MarkdownDsl {
    private val builder = StringBuilder()
    var keyboard: Keyboard? = null
        private set

    var content: String
        get() = builder.toString()
        set(value) {
            builder.clear()
            builder.append(value)
        }

    fun line(text: String = "") {
        builder.appendLine(text)
    }

    fun bold(text: String) = "**$text**"

    fun italic(text: String) = "*$text*"

    fun a(text: String, url: String) = "[$text]($url)"

    fun image(url: String, alt: String) = "![$alt]($url)"

    fun brief(title: String, body: String) {
        line(bold(title))
        line()
        line(body)
    }

    fun text(value: String) {
        builder.append(value)
    }

    fun keyboard(block: Keyboard.KeyboardBuilder.() -> Unit) {
        keyboard = Keyboard.create(block)
    }

    fun keyboard(keyboard: Keyboard) {
        this.keyboard = keyboard
    }

    fun build(): Markdown {
        val data = MarkdownData(content = builder.toString().trimEnd('\n', '\r'))
        return Markdown(data, keyboard)
    }
}