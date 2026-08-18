package xyz.xszq.bot.meme

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs

class EmojiKitchen {
    val imgDir = localCurrentDirVfs[ASSETS_DIR]
    suspend fun mix(a: String, b: String): VfsFile? {
        val aId = a.toUnicodeId()
        val bId = b.toUnicodeId()
        return listOf(
            imgDir["$aId/$bId.png"], imgDir["$bId/$aId.png"]
        ).firstOrNull { it.exists() }
    }
    companion object {
        private const val ASSETS_DIR = "./data/meme/emoji/"
        fun String.toUnicodeId() = List(length / 2) {
            "u" + codePointAt(it * 2).toString(16).lowercase()
        }.joinToString("-")
    }
}