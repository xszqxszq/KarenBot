package xyz.xszq.bot.theme

import korlibs.image.bitmap.Bitmap
import korlibs.image.format.readNativeImage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.serialization.XmlIgnoreWhitespace
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
data class Container(
    @XmlValue(true)
    @XmlIgnoreWhitespace(true)
    @XmlPolyChildren([".Text", ".Image", ".Container"])
    val children: MutableList<@Polymorphic Element>,
    override val id: String? = null,
    var width: Int? = null,
    var height: Int? = null,
    @SerialName("min-width")
    var minWidth: Int? = null,
    @SerialName("min-height")
    var minHeight: Int? = null,
    @SerialName("max-width")
    var maxWidth: Int? = null,
    @SerialName("max-height")
    var maxHeight: Int? = null,
    @XmlOtherAttributes
    val direction: Direction = Direction.Row,
    @XmlOtherAttributes
    @SerialName("justify-content")
    val justifyContent: Alignment = Alignment.Start,
    @XmlOtherAttributes
    @SerialName("align-content")
    val alignContent: Alignment = Alignment.Start,
    @XmlOtherAttributes
    val wrap: Wrap = Wrap.NoWrap,
    @XmlOtherAttributes
    override val margin: Spacing = Spacing(0),
    @XmlOtherAttributes
    override val padding: Spacing = Spacing(0),
    var background: String? = null,
    var stretch: Boolean = false,
    var color: String? = null,
    @SerialName("color-opacity")
    var colorOpacity: Double = 1.0
): Element {
    @Transient
    override var parent: Container? = null
    @Transient
    var backgroundImage: Bitmap? = null
//    operator fun get(id: String): Element? = children.firstOrNull { it.id == id } ?: run {
//        children.filterIsInstance<Container>().firstNotNullOfOrNull { it[id] }
//    }
    fun sub(id: String): Container? {
        if (id.contains("/")) {
            var now: Container? = this
            id.split("/").forEach { path ->
                now = now ?.sub(path)
            }
            return now
        }
        return children.filterIsInstance<Container>().firstOrNull { it.id == id }
    }
    fun text(id: String): Text? = children.filterIsInstance<Text>().firstOrNull { it.id == id } ?: run {
        children.filterIsInstance<Container>().firstNotNullOfOrNull { it.text(id) }
    }
    fun image(id: String): Image? = children.filterIsInstance<Image>().firstOrNull { it.id == id } ?: run {
        children.filterIsInstance<Container>().firstNotNullOfOrNull { it.image(id) }
    }
    fun add(child: Element) {
        children.add(child)
    }
    fun addAfter(after: Element, child: Element) {
        children.add(children.indexOf(after) + 1, child)
    }
    fun collectFonts(): Set<String> {
        val fonts = mutableSetOf<String>()
        children.forEach { element ->
            when (element) {
                is Text -> {
                    if (!element.font.isBlank())
                        fonts.add(element.font)
                }
                is Container -> {
                    fonts += element.collectFonts()
                }
                is Image -> {}
            }
        }
        return fonts
    }
    suspend fun modify(block: suspend Container.() -> Unit): Container {
        block(this)
        return this
    }
    suspend fun image(id: String, block: suspend Image.() -> Unit) {
        image(id) ?.let { block(it) }
    }
    suspend fun text(id: String, block: suspend Text.() -> Unit) {
        text(id) ?.let { block(it) }
    }
    suspend fun sub(id: String, block: suspend Container.() -> Unit) {
        sub(id) ?.let { block(it) }
    }
    suspend fun loadImage(theme: Theme): Unit = coroutineScope {
        background ?.let { src ->
            theme.fetchCache(src) ?.let { bg ->
                backgroundImage = bg
            } ?: launch {
                backgroundImage = kotlin.runCatching { theme.baseDir[src].readNativeImage() }.getOrNull()
            }
        }
        children.forEach { child ->
            when (child) {
                is Image -> {
                    child.src ?.let { src ->
                        theme.fetchCache(src) ?.let { img ->
                            child.image = img
                        } ?: launch {
                            child.image = kotlin.runCatching { theme.baseDir[src].readNativeImage() }.getOrNull()
                        }
                    }
                    child.mask ?.let { src ->
                        theme.fetchCache(src) ?.let { image ->
                            child.maskImage = image
                        } ?: launch {
                            child.maskImage = kotlin.runCatching { theme.baseDir[src].readNativeImage() }.getOrNull()
                        }
                    }
                }
                is Container -> launch { child.loadImage(theme) }
                is Text -> Unit
            }
        }
    }

    companion object {
        val module = SerializersModule {
            polymorphic(Element::class) {
                subclass(Text::class)
                subclass(Image::class)
                subclass(Container::class)
            }
        }
    }
}
