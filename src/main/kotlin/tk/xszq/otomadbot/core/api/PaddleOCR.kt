package tk.xszq.otomadbot.core.api

import com.google.gson.Gson
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.core.*
import java.util.*

object PaddleOCR {
    suspend fun getText(image: Image): String {
        val form = FormBody.Builder()
            .add("url", image.queryUrl()).build()
        val request = Request.Builder()
            .url(ApiSettings.list["python_api"]!!.url)
            .post(form)
            .build()
        val response = OkHttpClient().newCall(request).await()
        return if (response.isSuccessful) {
            val result = Gson().fromJson(response.body!!.get(), PythonApiResult::class.java)
            if (result.status) result.data.toLowerCase(Locale.ROOT) else ""
        } else {
            ""
        }
    }
}
