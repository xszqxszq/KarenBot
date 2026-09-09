package xyz.xszq.bot.audio.voice

import korlibs.io.file.VfsFile

/**
 * 活字印刷的文本块
 */
sealed interface Token {
    /**
     * 待处理的原始文本
     */
    sealed interface Raw : Token {
        val text: String

        /**
         * 中文文本
         */
        data class Chinese(override val text: String): Raw
        /**
         * 英文文本
         */
        data class English(override val text: String): Raw
        /**
         * 日文文本
         */
        data class Japanese(override val text: String): Raw
    }
    /**
     * 最终的文本块
     */
    sealed interface Final : Token {
        val file: VfsFile

        /**
         * 单个拼音字
         */
        data class Char(override val file: VfsFile): Final
        /**
         * 预设音频
         */
        data class Preset(override val file: VfsFile): Final
    }
}