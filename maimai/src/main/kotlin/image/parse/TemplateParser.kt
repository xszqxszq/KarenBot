package xyz.xszq.bot.image.parse

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.xmlStreaming
import xyz.xszq.bot.image.dom.Div
import xyz.xszq.bot.image.dom.Element
import xyz.xszq.bot.image.dom.Img
import xyz.xszq.bot.image.dom.Span

object TemplateParser {
    fun parse(xmlString: String): Map<String, Element> {
        val reader = xmlStreaming.newReader(xmlString)
        val elementStack = mutableListOf<Element>()
        val virtualRoot = Div("virtual_body")

        while (reader.hasNext()) {
            reader.next()
            when (reader.eventType) {
                EventType.START_ELEMENT -> {
                    val tagName = reader.localName.lowercase()
                    val id = reader.getAttributeValue(null, "id")
                    val styleStr = reader.getAttributeValue(null, "style")

                    val element: Element? = when (tagName) {
                        "div" -> Div(id)
                        "span" -> Span(id)
                        "img" -> Img(id, reader.getAttributeValue(null, "src"))
                        else -> null
                    }

                    if (element != null) {
                        if (styleStr != null) {
                            element.style = StyleParser.parse(styleStr)
                        }
                        if (elementStack.isEmpty()) {
                            virtualRoot.add(element)
                        } else {
                            elementStack.last().add(element)
                        }
                        elementStack.add(element)
                    }
                }
                EventType.TEXT -> {
                    val text = reader.text.trim()
                    if (text.isNotEmpty() && elementStack.isNotEmpty()) {
                        val current = elementStack.last()
                        if (current is Span) {
                            current.text += text
                        }
                    }
                }
                EventType.END_ELEMENT -> {
                    val tagName = reader.localName.lowercase()
                    if (tagName in listOf("div", "span", "img")) {
                        if (elementStack.isNotEmpty()) {
                            elementStack.removeLast()
                        }
                    }
                }
                else -> {}
            }
        }

        val topLevelElements = mutableMapOf<String, Element>()
        for (child in virtualRoot.children) {
            child.parent = null
            if (child.id != null) {
                topLevelElements[child.id] = child
            }
        }

        return topLevelElements
    }
}