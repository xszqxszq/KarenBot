package xyz.xszq.shinobu.template

import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import xyz.xszq.shinobu.dom.Element
import xyz.xszq.shinobu.parse.TemplateParser
import java.io.File

/**
 * 模板管理器
 *
 * 负责加载基础目录下的全部模板，并为各模板提供全局资源管理器与
 * 字体集合
 */
@Suppress("unused")
class TemplateManager(val basePath: String) {
    private class RawData(val elements: Map<String, Element>, val localRM: ResourceManager)
    private val loadedTemplates = mutableMapOf<String, RawData>()

    lateinit var globalResourceManager: ResourceManager
    lateinit var fontCollection: FontCollection

    /**
     * 初始化字体与模板数据
     *
     * 注册系统字体与别名，加载 `templates` 目录下各模板目录中的
     * `template.html`
     *
     * @param cacheDir 字体别名缓存的存放目录
     */
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

    /**
     * 按模板目录名取出已加载的模板
     *
     * @param name 模板名，对应 `templates` 下的子目录
     * @return 取出的模板
     */
    operator fun get(name: String): Template? {
        val data = loadedTemplates[name] ?: return null
        return Template(data.elements, data.localRM)
    }
}