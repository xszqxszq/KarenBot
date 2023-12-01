package xyz.xszq.nereides.message

import com.soywiz.korio.file.VfsFile

interface LocalRichMedia: RichMedia {
    val file: VfsFile
}