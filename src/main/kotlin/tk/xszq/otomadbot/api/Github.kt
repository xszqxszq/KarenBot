@file:Suppress("unused")
package tk.xszq.otomadbot.api

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import tk.xszq.otomadbot.configMain
import java.net.InetSocketAddress
import java.net.Proxy

data class GithubApiArtifact(val id: Long, val node_id: String, val name: String, val size_in_bytes: Long,
                        val archive_download_url: String, val expired: Boolean, val created_at: String,
                        val updated_at: String, val expires_at: String)
data class GithubApiArtifacts(val total_count: Int, val artifacts: List<GithubApiArtifact>)

data class GithubApi(private val proxyEnabled: Boolean = true) {
    @Suppress("EXPERIMENTAL_API_USAGE")
    private val client = HttpClient(CIO) {
        if (proxyEnabled)
            engine {
                proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(18889))
            }
    }
    val requiredHeaders = listOf(Pair("Authorization", "token ${configMain.github.token}"))
    suspend fun get(apiUrl: String): HttpResponse = client.get(apiUrl) {
        headers {
            requiredHeaders.forEach {
                append(it.first, it.second)
            }
        }
    }
    fun api(path: String) = "https://api.github.com/$path"
    suspend fun getArtifacts(repo: String): GithubApiArtifacts = Gson()
        .fromJson(get(api("repos/$repo/actions/artifacts")).readText(), GithubApiArtifacts::class.java)
}