package xyz.xszq.shinobu

import korlibs.io.file.VfsFile
import korlibs.io.lang.FileNotFoundException
import nl.adaptivity.xmlutil.serialization.XML

class ThemeManager(
    val themeBaseDir: VfsFile,
    val defaultFont: String = "Simsun"
) {
    lateinit var xml: XML
    lateinit var renderer: Renderer
    suspend fun init() {
        xml = XML(Container.module) {
            indentString = "\t"
        }
        renderer = Renderer(defaultFont)
        renderer.init()
    }
    suspend fun loadTheme(name: String): Theme {
        val baseDir = themeBaseDir[name]
        if (!baseDir.exists()) {
            throw FileNotFoundException()
        }
        val themeFile = baseDir["theme.xml"]
        if (!themeFile.exists()) {
            throw FileNotFoundException()
        }
        val theme = xml.decodeFromString(Theme.serializer(), themeFile.readString()).also {
            it.baseDir = baseDir
            it.renderer = renderer
        }
        theme.loadFonts()
        theme.loadImages()
        return theme
    }
}