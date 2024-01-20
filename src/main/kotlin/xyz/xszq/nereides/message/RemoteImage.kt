package xyz.xszq.nereides.message

import korlibs.io.file.VfsFile
import xyz.xszq.nereides.NetworkUtils

class RemoteImage(override val id: String, override val url: String) : Image {
    suspend fun <R> use(block: suspend (VfsFile) -> R) = NetworkUtils.useNetFile(url, block = block)
}