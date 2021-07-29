package tk.xszq.otomadbot.api

import com.google.gson.Gson
import com.soywiz.krypto.encoding.Base64
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.get
import tk.xszq.otomadbot.lowercase

class PythonApiResult(val status: Boolean, val data: String)

object PythonApi: ApiClient() {
    suspend fun ocr(url: String): String {
        val form = FormBody.Builder()
            .add("url", url).build()
        val request = Request.Builder()
            .url(ApiSettings.list["python_api"]!!.url + "/ocr")
            .post(form)
            .build()
        val response = OkHttpClient().newCall(request).await()
        return if (response.isSuccessful) {
            val result = Gson().fromJson(response.body!!.get(), PythonApiResult::class.java)
            if (result.status) result.data.lowercase() else ""
        } else {
            ""
        }
    }
    suspend fun spherize(base64: String): ByteArray? {
        val form = FormBody.Builder()
            .add("image", base64).build()
        val request = Request.Builder()
            .url(ApiSettings.list["python_api"]!!.url + "/spherize")
            .post(form)
            .build()
        val response = OkHttpClient().newCall(request).await()
        return if (response.isSuccessful)
            Base64.decode(Gson().fromJson(response.body!!.get(), PythonApiResult::class.java).data)
        else null
    }
    suspend fun sentiment(text: String): Boolean? {
        val form = FormBody.Builder()
            .add("text", text).build()
        val request = Request.Builder()
            .url(ApiSettings.list["python_api"]!!.url + "/sentiment")
            .post(form)
            .build()
        val response = OkHttpClient().newCall(request).await()
        return if (response.isSuccessful)
            Gson().fromJson(response.body!!.get(), PythonApiResult::class.java).data.toBoolean()
        else null
    }
}