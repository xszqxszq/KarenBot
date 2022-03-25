@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xszq.otomadbot.api

import com.soywiz.krypto.encoding.hex
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.xszq.otomadbot.OtomadBotCore
import xyz.xszq.otomadbot.core.SafeYamlConfig
import java.net.InetSocketAddress
import java.net.Proxy
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


@Serializable
open class PicaResponse<T>(val code: Int, val message: String, val data: T ?= null)
@Serializable
data class PicaLoginResponse(val token: String)
@Serializable
data class PicaRandomResponse(val comics: List<PicaComicListInfo>)
@Serializable
data class PicaPageResponse(val pages: PicaPages, val ep: PicaEp)

@Serializable
data class PicaComicListInfo(
    val _id: String, val title: String, val author: String ?= null, val totalViews: Int, val totalLikes: Int,
    val pagesCount: Int, val epsCount: Int, val finished: Boolean, val categories: List<String>, val thumb: PicaMedia,
    val likesCount: Int
)
@Serializable
data class PicaMedia(val originalName: String, val path: String, val fileServer: String) {
    fun toUrl() = "$fileServer/static/$path"
}
@Serializable
data class PicaComicInfo(
    val _id: String, val _creator: PicaCreator, val title: String, val description: String, val thumb: PicaMedia,
    val author: String, val chineseTeam: String, val categories: List<String>, val tags: List<String>,
    val pagesCount: Int, val epsCount: Int, val finished: Boolean, val updated_at: String, val created_at: String,
    val allowDownload: Boolean, val allowComment: Boolean, val viewsCount: Int, val likesCount: Int,
    val isFavourite: Boolean, val isLiked: Boolean, val commentsCount: Int
)
@Serializable
data class PicaCreator(
    val _id: String, val gender: String, val name: String, val verified: Boolean, val exp: Int, val level: Int,
    val role: String, val avatar: PicaMedia, val characters: List<String>, val title: String, val slogan: String,
    val character: String
)
@Serializable
data class PicaPages(val docs: List<PicaImage>, val total: Int, val limit: Int, val page: Int, val pages: Int)
@Serializable
data class PicaEp(val _id: String, val title: String)
@Serializable
data class PicaImage(val _id: String, val media: PicaMedia, val id: String)

object PicaComic: ApiClient() {
    var config = PicaConfig()
    var apiKey = "C69BAF41DA5ABD1FFEDC6D2FEA56B"
    var nonce = "b1ab87b4800d4d4590a11701b8551afa"
    var token: String ?= null
    private const val site = "https://picaapi.picacomic.com/"
    private fun getSignature(uri: String, method: String, time: String): String {
        val raw = (uri.trim { it == '/' } + time + nonce + method + apiKey)
            .lowercase()
        val key = "~d}\$Q7\$eIni=V)9\\RK/P.RM4;9[7|@/CA}b~OW!3?EV`:<>M7pddUBL5n|0/*Cn"
            .toByteArray()
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        sha256Hmac.init(secretKey)
        return sha256Hmac.doFinal(raw.toByteArray()).hex
    }
    private fun getRequestBuilder(
        path: String, method: String, time: String = (System.currentTimeMillis() / 1000).toString(),
        accept: String = "application/vnd.picacomic.com.v1+json") =
        Request.Builder()
            .url(site + path)
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .addHeader("accept", accept)
            .addHeader("api-key", apiKey)
            .addHeader("app-channel", "2")
            .addHeader("time", time)
            .addHeader("nonce", nonce)
            .addHeader("app-version", "2.2.1.2.3.3")
            .addHeader("app-uuid", "defaultUuid")
            .addHeader("app-platform", "android")
            .addHeader("app-build-version", "44")
            .addHeader("User-Agent", "okhttp/3.8.1")
            .addHeader("image-quality", "medium")
            .addHeader("signature", getSignature(path, method, time))
            .addHeader("authorization", token ?: "")
    private inline fun <reified T> post(path: String, form: Map<String, String> = mapOf()): PicaResponse<T> {
        val oClient = OkHttpClient.Builder()
            .proxy(Proxy(if (ApiSettings.proxy.type == "socks") Proxy.Type.SOCKS else Proxy.Type.HTTP,
                InetSocketAddress(ApiSettings.proxy.addr, ApiSettings.proxy.port)))
            .build()
        val body = OtomadBotCore.json.encodeToString(form).toRequestBody()
        val req = getRequestBuilder(path, "post")
            .post(body)
            .build()
        val result = oClient.newCall(req).execute()
        return OtomadBotCore.json.decodeFromString(result.body!!.string())
    }
    private inline fun <reified T> get(path: String): PicaResponse<T> {
        val oClient = OkHttpClient.Builder()
            .proxy(Proxy(if (ApiSettings.proxy.type == "socks") Proxy.Type.SOCKS else Proxy.Type.HTTP,
                InetSocketAddress(ApiSettings.proxy.addr, ApiSettings.proxy.port)))
            .build()
        val req = getRequestBuilder(path, "get").build()
        val result = oClient.newCall(req).execute()
        return OtomadBotCore.json.decodeFromString(result.body!!.string())
    }
    fun login(username: String = config.username, password: String = config.password) {
        val response = post<PicaLoginResponse?>("auth/sign-in", buildMap {
            put("email", username)
            put("password", password)
        })
        if (response.code == 200)
            token = response.data!!.token
        else
            println(response.code.toString() + ", " + response.message)
    }
    fun random(): List<PicaComicListInfo> = get<PicaRandomResponse>("comics/random").data ?.comics ?: emptyList()
    fun getPage(id: String, ep: Int, page: Int) =
        get<PicaPageResponse>("comics/$id/order/$ep/pages?page=$page").data
    fun saveConfig() {
        OtomadBotCore.configFolderPath.resolve("pica.yml").toFile()
            .writeText(OtomadBotCore.yaml.encodeToString(PicaConfig.serializer(), config))
    }
    fun reloadConfig() {
        val file = OtomadBotCore.configFolderPath.resolve("pica.yml").toFile()
        if (!file.exists())
            saveConfig()
        config = OtomadBotCore.yaml.decodeFromString(PicaConfig.serializer(), file.readText())
    }
}

@Serializable
data class PicaConfig(val username: String = "", val password: String = ""): SafeYamlConfig()
