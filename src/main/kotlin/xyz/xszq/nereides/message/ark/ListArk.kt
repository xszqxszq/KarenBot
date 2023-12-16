package xyz.xszq.nereides.message.ark

import xyz.xszq.nereides.message.Ark
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.message.MessageArkKv
import xyz.xszq.nereides.payload.message.MessageArkObj

class ListArk(
    val desc: String,
    val prompt: String,
    val list: List<ListArkItem>
): Ark(MessageArk(23, buildList {
    add(MessageArkKv("#DESC#", desc))
    add(MessageArkKv("#PROMPT#", prompt))
    add(MessageArkKv("#LIST#", obj = list.map { item ->
        MessageArkObj(buildList {
            add(MessageArkKv("desc", item.text))
            item.link ?.let { link ->
                add(MessageArkKv("link", link))
            }
        })
    }))
})) {
    class Builder {
        private var desc = ""
        private var prompt = ""
        private val list = mutableListOf<ListArkItem>()
        fun desc(block: () -> String) {
            desc = block()
        }
        fun prompt(block: () -> String) {
            prompt = block()
        }
        fun text(block: () -> String) {
            list.add(ListArkItem(block()))
        }
        fun link(link: String, block: () -> String) {
            list.add(ListArkItem(block(), link))
        }
        fun build(): ListArk {
            return ListArk(desc, prompt, list)
        }
    }
    companion object {
        suspend fun build(block: suspend Builder.() -> Unit): ListArk {
            val builder = Builder()
            block(builder)
            return builder.build()
        }
    }
}