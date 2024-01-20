package xyz.xszq.nereides.message

import korlibs.io.file.VfsFile

interface LocalRichMedia: RichMedia {
    val file: VfsFile?
}