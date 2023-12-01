package xyz.xszq.nereides.message

import com.soywiz.korio.file.VfsFile

class LocalVoice(
    override val file: VfsFile,
    override val id: String = "",
    override val url: String = ""
) : Voice, LocalRichMedia