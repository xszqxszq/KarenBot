package tk.xszq.otomadbot.api

import com.soywiz.korio.net.URL
import com.soywiz.krypto.encoding.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.get
import java.time.Duration

object FiveThousandChoyenApi {
    suspend fun generate(top: String, bottom: String): ByteArray {
        val client = OkHttpClient.Builder()
            .readTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(10))
            .connectTimeout(Duration.ofSeconds(10))
            .callTimeout(Duration.ofSeconds(10))
            .build()
        val request = Request.Builder()
            .url(ApiSettings.list["5000choyen"]!!.url + "?top=" + URL.encodeComponent(top, formUrlEncoded = true)
                    + "&bottom=" + URL.encodeComponent(bottom, formUrlEncoded = true))
            .build()
        return Base64.decode(client.newCall(request).await().body!!.get()
            .substringAfter("data:image/png;base64,"))
    }
}