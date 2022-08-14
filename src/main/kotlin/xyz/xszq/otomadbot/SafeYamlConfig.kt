package xyz.xszq.otomadbot

import com.soywiz.klock.DateTime
import com.soywiz.korio.file.std.toVfs
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.yamlkt.Yaml

@OptIn(InternalSerializationApi::class)
open class SafeYamlConfig<T: Any>(plugin: JvmPlugin, val name: String, var data: T) {
    private val file = plugin.resolveConfigFile("$name.yml").toVfs()
    suspend fun load() {
        if (file.exists())
            data = yaml.decodeFromString(data::class.serializer(), file.readString())
        else
            save()
    }
    suspend fun save() {
        if (!file.exists())
            file.touch(DateTime.now())
        file.writeString(yaml.encodeToString(data))
    }

    companion object {
        val yaml = Yaml {  }
    }
}