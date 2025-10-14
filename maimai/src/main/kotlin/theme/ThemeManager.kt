package xyz.xszq.bot.theme

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.lang.FileNotFoundException
import kotlinx.coroutines.runBlocking
import nl.adaptivity.xmlutil.serialization.XML

object ThemeManager {
    private const val THEME_DIR = "./data/maimai/theme/"
    var themeBaseDir: VfsFile
    val xml = XML(Container.module) {
        indentString = "\t"
    }
    init {
        runBlocking {
            themeBaseDir = localCurrentDirVfs[THEME_DIR]
        }
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
        }
        theme.loadFonts()
        theme.loadImages()
        return theme
    }
}