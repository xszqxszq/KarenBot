package xyz.xszq.shinobu.template

import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import xyz.xszq.shinobu.dom.Element
import xyz.xszq.shinobu.parse.TemplateParser
import java.io.File

@Suppress("unused")
class TemplateManager(val basePath: String) {
    private class RawData(val elements: Map<String, Element>, val localRM: ResourceManager)
    private val loadedTemplates = mutableMapOf<String, RawData>()

    lateinit var globalResourceManager: ResourceManager
    lateinit var fontCollection: FontCollection

    fun init(cacheDir: String = "../.fonts/") {
        val fontProvider = TypefaceFontProvider()
        fontCollection = FontCollection().apply {
            setDefaultFontManager(FontMgr.default)
            setAssetFontManager(fontProvider)
        }

        FontAliasCache(File(basePath, cacheDir)).loadAndRegister(fontProvider)

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

    operator fun get(name: String): Template? {
        val data = loadedTemplates[name] ?: return null
        return Template(data.elements, data.localRM)
    }
}