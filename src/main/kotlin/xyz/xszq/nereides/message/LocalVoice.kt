package xyz.xszq.nereides.message

import korlibs.io.file.VfsFile

class LocalVoice(
    override val file: VfsFile,
    override val id: String = "",
    override val url: String = ""
) : Voice, LocalRichMedia