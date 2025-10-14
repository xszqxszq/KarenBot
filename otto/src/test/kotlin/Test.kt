import com.hankcs.hanlp.HanLP
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.file.std.localCurrentDirVfs
import marytts.LocalMaryInterface
import marytts.MaryInterface
import org.w3c.dom.Element
import xyz.xszq.bot.voice.OttoConfig
import xyz.xszq.bot.voice.TTSParser
import java.util.Locale


@OptIn(ExperimentalHoplite::class)
suspend fun main() {
    val config = ConfigLoaderBuilder.Companion.default()
        .addFileSource("./config/otto.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<OttoConfig>()
    val parser = TTSParser(config, localCurrentDirVfs["data/audio/otto"])
    println(parser.parse("还是很喜欢foge，这也是没办法的事\\n毕竟是第一个接触的直播势，也是第一个走进心坎里的人。一次又一次的洗粉，理智告诉我应该远离她，反应过来身上已遍布荆棘，再也离不开了"))
}