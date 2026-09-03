package xyz.xszq.bot.audio.voice

import korlibs.io.file.VfsFile

sealed interface Token {
    sealed interface Raw : Token {
        val text: String

        data class Chinese(override val text: String): Raw
        data class English(override val text: String): Raw
        data class Japanese(override val text: String): Raw
    }
    sealed interface Final : Token {
        val file: VfsFile

        data class Char(override val file: VfsFile): Final
        data class Preset(override val file: VfsFile): Final
    }
}