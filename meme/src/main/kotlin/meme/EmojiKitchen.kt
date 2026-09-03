package xyz.xszq.bot.meme

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs

/**
 * Emoji 表情合成
 */
class EmojiKitchen {
    val imgDir = localCurrentDirVfs[ASSETS_DIR]
    /**
     * 合成两个 Emoji
     *
     * @return 合成结果
     */
    suspend fun mix(a: String, b: String): VfsFile? {
        val aId = a.toUnicodeId()
        val bId = b.toUnicodeId()
        return listOf(
            imgDir["$aId/$bId.png"], imgDir["$bId/$aId.png"]
        ).firstOrNull { it.exists() }
    }
    companion object {
        private const val ASSETS_DIR = "./data/meme/emoji/"
        private fun String.toUnicodeId() = List(length / 2) {
            "u" + codePointAt(it * 2).toString(16).lowercase()
        }.joinToString("-")
    }
}