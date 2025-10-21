package xyz.xszq.shinobu

import korlibs.image.bitmap.Bitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes

@Serializable
data class Image(
    val width: Int? = null,
    val height: Int? = null,
    var src: String? = null,
    var mask: String? = null,
    var opacity: Double = 1.0,
    var stretch: Boolean = false,
    override val id: String? = null,
    @XmlOtherAttributes
    override val margin: Spacing = Spacing(0),
    @XmlOtherAttributes
    override val padding: Spacing = Spacing(0)
): Element {
    @Transient
    override var parent: Container? = null
    @Transient
    var image: Bitmap? = null
    @Transient
    var maskImage: Bitmap? = null
}