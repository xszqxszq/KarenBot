package xyz.xszq

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.rootLocalVfs
import com.soywiz.korio.file.std.tempVfs
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.BlueArchiveLogo
import xyz.xszq.bot.BotConfig
import xyz.xszq.bot.FiveThousandChoyen
import xyz.xszq.bot.maimai.Maimai


suspend fun main() {
    rootLocalVfs["D://Temp//test.png"].writeBytes(BlueArchiveLogo(false).draw("这是", "中文").encode(
        PNG
    ))
}