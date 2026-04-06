package xyz.xszq.bot.image.template

import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import xyz.xszq.bot.image.dom.Element
import xyz.xszq.bot.image.parse.TemplateParser
import java.io.File

class TemplateManager(val basePath: String) {
    private class RawData(val elements: Map<String, Element>, val localRM: ResourceManager)
    private val loadedTemplates = mutableMapOf<String, RawData>()

    lateinit var globalResourceManager: ResourceManager
    lateinit var fontCollection: FontCollection

    fun init() {
        val fontProvider = TypefaceFontProvider()
        fontCollection = FontCollection().apply {
            setDefaultFontManager(FontMgr.default)
            setAssetFontManager(fontProvider)
        }
        initFontAliases(fontProvider)

        globalResourceManager = ResourceManager(
            basePath = File(basePath),
            parent = null,
            preloadLocal = false,
            fontCollection = fontCollection
        )

        var templates: File? = File(basePath, "templates")
        if (!templates!!.exists() || !templates.isDirectory)
            templates = null

        templates ?.listFiles() ?.filter { it.isDirectory } ?.forEach { folder ->
            val templateName = folder.name
            val htmlFile = File(folder, "template.html")

            if (htmlFile.exists()) {
                val topLevelElements = TemplateParser.parse(htmlFile.readText())

                val localRM = ResourceManager(
                    basePath = folder,
                    parent = globalResourceManager,
                    preloadLocal = true,
                    fontCollection = fontCollection
                )

                loadedTemplates[templateName] = RawData(topLevelElements, localRM)
            }
        }
    }

    private fun initFontAliases(fontProvider: TypefaceFontProvider) {
        val fontMgr = FontMgr.default
        for (i in 0 until fontMgr.familiesCount) {
            val familyName = fontMgr.getFamilyName(i)
            val styleSet = fontMgr.makeStyleSet(i) ?: continue
            for (j in 0 until styleSet.count()) {
                val typeface = styleSet.getTypeface(j) ?: continue
                val styleName = styleSet.getStyleName(j)

                val styleIdentifier = when {
                    styleName.contains("Heavy", ignoreCase = true) -> "H"
                    styleName.contains("Bold", ignoreCase = true) -> "B"
                    styleName.contains("Regular", ignoreCase = true) -> "R"
                    styleName.contains("Medium", ignoreCase = true) -> "M"
                    styleName.contains("Light", ignoreCase = true) -> "L"
                    else -> ""
                }
                if (styleIdentifier.isNotEmpty()) {
                    fontProvider.registerTypeface(typeface, "$familyName-$styleIdentifier")
                }
            }
        }
    }

    operator fun get(name: String): Template? {
        val data = loadedTemplates[name] ?: return null
        return Template(data.elements, data.localRM)
    }
}