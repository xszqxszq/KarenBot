@file:Suppress("unused")

package xyz.xszq.nereides.message

import korlibs.io.file.VfsFile

class LocalImage(
    override val file: VfsFile,
    override val id: String = "",
    override var url: String = ""
) : Image, LocalRichMedia