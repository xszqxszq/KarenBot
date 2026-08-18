package xyz.xszq.shinobu

import xyz.xszq.shinobu.dom.Div
import xyz.xszq.shinobu.dom.Img
import xyz.xszq.shinobu.dom.Span
import xyz.xszq.shinobu.parse.StyleParser
import xyz.xszq.shinobu.parse.TemplateParser
import xyz.xszq.shinobu.style.*
import kotlin.test.*

class ShinobuTest {

    @Test
    fun templateParse() {
        val xml = """
            <div id="root" style="width:100px;padding:10px 20px;">
                <div id="child" style="height:50px;background-color:#ff0000;"></div>
                <span id="label" style="font-size:16px;color:rgb(0,255,0);">你好</span>
                <img id="pic" src="a.png"/>
            </div>
        """.trimIndent()
        val elements = TemplateParser.parse(xml)
        assertEquals(setOf("root"), elements.keys)
        val root = elements["root"] as Div
        assertEquals(100f, root.style.width)
        assertEquals(10f, root.style.padding.top)
        assertEquals(20f, root.style.padding.right)
        assertEquals(10f, root.style.padding.bottom)
        assertEquals(20f, root.style.padding.left)
        val child = root["child"] as Div
        assertEquals(50f, child.style.height)
        assertEquals(0xFFFF0000.toInt(), child.style.backgroundColor)
        val label = root["label"] as Span
        assertEquals("你好", label.text)
        assertEquals(16f, label.style.textSize)
        assertEquals(0xFF00FF00.toInt(), label.style.textColor)
        val pic = root["pic"] as Img
        assertEquals("a.png", pic.src)
    }

    @Test
    fun templateParseIgnoresUnknownTags() {
        val elements = TemplateParser.parse("""<root><foo id="x"><bar/></foo><div id="ok"></div></root>""")
        assertEquals(setOf("ok"), elements.keys)
    }

    @Test
    fun templateParseNestedPath() {
        val xml = """
            <div id="root">
                <div id="mid">
                    <span id="leaf">text</span>
                </div>
            </div>
        """.trimIndent()
        val root = TemplateParser.parse(xml)["root"]!!
        assertNotNull(root["mid/leaf"])
        assertEquals("text", (root["mid/leaf"] as Span).text)
    }

    @Test
    fun styleParse() {
        val style = StyleParser.parse(
            "width:200px;height:100px;min-width:10px;max-width:300px;" +
                "background-color:#0f0;background-image:url('bg.png');background-size:cover;" +
                "background-position:center;background-opacity:0.5;" +
                "margin:1px 2px 3px 4px;padding:5px;" +
                "flex-direction:column;flex-wrap:wrap;justify-content:center;align-items:center;" +
                "opacity:0.8;color:#123456;font-size:20px;font-family:'Arial','SimHei';font-weight:bold;" +
                "text-stroke:2px #ffffff;text-shadow:1px 2px #000000;text-align:right;white-space:nowrap;" +
                "object-fit:cover;mask-image:url('mask.png')"
        )
        assertEquals(200f, style.width)
        assertEquals(100f, style.height)
        assertEquals(10f, style.minWidth)
        assertEquals(300f, style.maxWidth)
        assertEquals(0xFF00FF00.toInt(), style.backgroundColor)
        assertEquals("bg.png", style.backgroundImage)
        assertEquals(BackgroundSize.COVER, style.backgroundSize)
        assertEquals(BackgroundPosition.CENTER, style.backgroundPosition)
        assertEquals(0.5f, style.backgroundOpacity)
        assertEquals(Spacing(1f, 2f, 3f, 4f), style.margin)
        assertEquals(Spacing(5f, 5f, 5f, 5f), style.padding)
        assertEquals(FlexDirection.COLUMN, style.flexDirection)
        assertEquals(FlexWrap.WRAP, style.flexWrap)
        assertEquals(JustifyContent.CENTER, style.justifyContent)
        assertEquals(AlignItems.CENTER, style.alignItems)
        assertEquals(0.8f, style.opacity)
        assertEquals(0xFF123456.toInt(), style.textColor)
        assertEquals(20f, style.textSize)
        assertEquals(listOf("Arial", "SimHei"), style.fontFamilies)
        assertEquals(700, style.fontWeight)
        assertNotNull(style.textStroke)
        assertEquals(2f, style.textStroke!!.size)
        assertEquals(0xFFFFFFFF.toInt(), style.textStroke!!.color)
        assertNotNull(style.textShadow)
        assertEquals(1f, style.textShadow!!.dx)
        assertEquals(2f, style.textShadow!!.dy)
        assertEquals(0xFF000000.toInt(), style.textShadow!!.color)
        assertEquals(TextAlign.RIGHT, style.textAlign)
        assertEquals(WhiteSpace.NOWRAP, style.whiteSpace)
        assertEquals(ObjectFit.COVER, style.objectFit)
        assertEquals("mask.png", style.maskImage)
    }

    @Test
    fun colorParsing() = with(StyleParser) {
        assertEquals(0xFFFFFFFF.toInt(), "#fff".rgbColor())
        assertEquals(0xFF00FF00.toInt(), "#00ff00".rgbColor())
        assertEquals(0x0080FF00.toInt(), "#80ff0000".rgbColor())
        assertEquals(0xFF00FF00.toInt(), "rgb(0, 255, 0)".rgba())
        assertEquals(0x7FFF0000.toInt(), "rgba(255, 0, 0, 0.5)".rgba())
        assertEquals(0x00000000, StyleParser.parse("background-color:transparent").backgroundColor)
        assertNull(StyleParser.parse("background-color:invalid").backgroundColor)
    }

    @Test
    fun domNavigationAndClone() {
        val root = Div("root")
        val child = Div("child")
        val span = Span("label", "text")
        child.add(span)
        root.add(child)
        assertEquals(root, span.parent?.parent)
        assertEquals(span, root["child/label"])
        assertEquals(span, root.findById("label"))
        assertNull(root["missing"])
        val clone = root.clone()
        assertEquals("root", clone.id)
        val clonedSpan = clone["child/label"]
        assertNotNull(clonedSpan)
        assertTrue(clonedSpan !== span)
        assertEquals("text", (clonedSpan as Span).text)
        assertTrue(clonedSpan.parent?.parent === clone)
    }

    @Test
    fun contentRect() {
        val div = Div("d")
        div.style.padding = Spacing(10f, 20f, 30f, 40f)
        div.measuredWidth = 100f
        div.measuredHeight = 100f
        val rect = div.contentRect
        assertEquals(40f, rect.left)
        assertEquals(10f, rect.top)
        assertEquals(80f, rect.right)
        assertEquals(70f, rect.bottom)
    }

    @Test
    fun deepCopyStyle() {
        val style = Style(
            textStroke = TextStroke(0xFFFF0000.toInt(), 2f),
            textShadow = TextShadow(0xFF000000.toInt(), 1f, 2f),
            margin = Spacing(1f, 2f, 3f, 4f),
            padding = Spacing(5f, 6f, 7f, 8f),
            backgroundColor = 0xFF00FF00.toInt()
        )
        val copy = style.deepCopy()
        assertEquals(style, copy)
        copy.textStroke!!.size = 10f
        assertEquals(2f, style.textStroke!!.size)
    }
}
