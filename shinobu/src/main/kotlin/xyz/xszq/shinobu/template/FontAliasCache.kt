package xyz.xszq.shinobu.template

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import java.io.File

class FontAliasCache(
    private val cacheDir: File
) {
    /** 持有已注册 Typeface 的强引用，防止 GC 回收原生对象 */
    private val heldTypefaces = mutableListOf<Typeface>()
    @Serializable
    data class FontEntry(
        val weight: Int,
        val aliases: List<String>
    )

    @Serializable
    data class CacheData(
        val familyCount: Int = 0,
        val fonts: Map<String, FontEntry> = emptyMap()
    )

    private val cacheFile = File(cacheDir, "alias-cache.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private var cache: CacheData = CacheData()
    private val aliasMap = mutableMapOf<String, String>()

    fun loadAndRegister(fontProvider: TypefaceFontProvider) {
        cacheDir.mkdirs()
        cache = readCache()

        val fontMgr = FontMgr.default
        val currentCount = fontMgr.familiesCount
        val needScan = currentCount != cache.familyCount
        if (needScan) {
            scanSystemFonts(fontMgr, fontProvider)
        } else {
            registerFromCache(fontMgr, fontProvider)
        }

        buildAliasMap()
        writeCache()
    }

    fun resolve(alias: String): String? = aliasMap[alias]

    private fun readCache(): CacheData = runCatching {
        if (cacheFile.exists()) json.decodeFromString<CacheData>(cacheFile.readText())
        else CacheData()
    }.getOrDefault(CacheData())

    private fun writeCache() = runCatching {
        cacheDir.mkdirs()
        val tmp = File(cacheDir, "alias-cache.json.tmp")
        tmp.writeText(json.encodeToString(cache))
        tmp.renameTo(cacheFile)
    }

    private fun scanSystemFonts(fontMgr: FontMgr, fontProvider: TypefaceFontProvider) {
        val existingKeys = cache.fonts.keys
        val newFonts = mutableMapOf<String, FontEntry>()
        var newCount = 0

        for (i in 0 until fontMgr.familiesCount) {
            newCount++
            val familyName = fontMgr.getFamilyName(i)
            val styleSet = fontMgr.makeStyleSet(i) ?: continue

            for (j in 0 until styleSet.count()) {
                val styleName = styleSet.getStyleName(j)
                val key = "system|$familyName|$styleName"

                if (key in existingKeys) continue

                val typeface = styleSet.getTypeface(j) ?: continue
                val aliases = generateAliases(typeface, familyName, styleName)

                if (aliases.isNotEmpty()) {
                    heldTypefaces.add(typeface)
                    for (alias in aliases) {
                        fontProvider.registerTypeface(typeface, alias)
                    }
                    newFonts[key] = FontEntry(typeface.fontStyle.weight, aliases)
                }
            }
        }

        cache = cache.copy(familyCount = newCount, fonts = newFonts)
    }

    private fun registerFromCache(fontMgr: FontMgr, fontProvider: TypefaceFontProvider) {
        for ((key, entry) in cache.fonts) {
            val parts = parseSystemKey(key) ?: continue
            val (familyName, styleName) = parts

            val typeface = findSystemTypeface(fontMgr, familyName, styleName, entry.weight)
                ?: continue

            heldTypefaces.add(typeface)
            for (alias in entry.aliases) {
                fontProvider.registerTypeface(typeface, alias)
            }
        }
    }

    private fun findSystemTypeface(
        fontMgr: FontMgr, familyName: String, styleName: String, weight: Int
    ): Typeface? {
        val style = FontStyle(weight, FontWidth.NORMAL, FontSlant.UPRIGHT)
        fontMgr.matchFamilyStyle(familyName, style)?.let { return it }

        for (i in 0 until fontMgr.familiesCount) {
            if (fontMgr.getFamilyName(i) != familyName) continue
            val set = fontMgr.makeStyleSet(i) ?: continue
            for (j in 0 until set.count()) {
                if (set.getStyleName(j) == styleName) return set.getTypeface(j)
            }
            break
        }
        return null
    }

    private fun parseSystemKey(key: String): Pair<String, String>? {
        val parts = key.split("|", limit = 3)
        if (parts.size < 3 || parts[0] != "system") return null
        return parts[1] to parts[2]
    }

    private fun generateAliases(
        typeface: Typeface, familyName: String, styleName: String
    ): List<String> {
        val aliases = LinkedHashSet<String>()
        val styleId = styleIdentifier(styleName)

        // 标准别名: "familyName-styleId"
        if (styleId.isNotEmpty()) {
            aliases.add("$familyName-$styleId")
        }

        // 所有本地化别名
        typeface.familyNames.forEach { fn ->
            val name = fn.name.takeIf { it.isNotEmpty() } ?: return@forEach
            // name 本身可能已经包含字重后缀 (如 "阿里巴巴普惠体 H")
            if (styleId.isNotEmpty()) {
                // 标准化: 空格→连字符
                val parts = name.split(" ")
                if (parts.last().length <= 2) {
                    // 名字末尾有字重标记 → 标准化为 name-styleId
                    aliases.add(parts.dropLast(1).joinToString(" ") + "-$styleId")
                } else {
                    aliases.add("$name-$styleId")
                }
            }
            // 也注册不含 style 后缀的原始名
            if (styleId.isNotEmpty()) {
                aliases.add(name)
            }
        }

        return aliases.toList()
    }

    private fun buildAliasMap() {
        aliasMap.clear()
        cache.fonts.forEach { (key, entry) ->
            for (alias in entry.aliases) {
                aliasMap[alias] = key
            }
        }
    }

    companion object {
        fun styleIdentifier(styleName: String): String = when {
            styleName.contains("Heavy", ignoreCase = true) -> "H"
            styleName.contains("Bold", ignoreCase = true) -> "B"
            styleName.contains("Regular", ignoreCase = true) -> "R"
            styleName.contains("Medium", ignoreCase = true) -> "M"
            styleName.contains("Light", ignoreCase = true) -> "L"
            else -> ""
        }
    }
}
