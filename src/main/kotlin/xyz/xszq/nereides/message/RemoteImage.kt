package xyz.xszq.nereides.message

import korlibs.io.file.VfsFile
import korlibs.io.file.std.toVfs
import xyz.xszq.nereides.NetworkUtils

class RemoteImage(override val id: String, override val url: String) : Image {
    suspend fun getFile(): VfsFile {
        val file = NetworkUtils.downloadTempFile(url)!!
        file.deleteOnExit()
        return file.toVfs()
    }
}