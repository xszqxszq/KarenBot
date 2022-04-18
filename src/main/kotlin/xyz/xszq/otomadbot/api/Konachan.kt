package xyz.xszq.otomadbot.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class KonachanImage(
    val id: Long, val tags: String, val created_at: Long, val creator_id: Long?, val author: String?, val change: Long?,
    val source: String, val score: Long, val md5: String, val file_size: Long, val file_url: String,
    val is_shown_in_index: Boolean, val preview_url: String, val preview_width: Int, val preview_height: Int,
    val actual_preview_width: Int, val actual_preview_height: Int, val sample_url: String,
    val sample_width: Int, val sample_height: Int, val sample_file_size: Long, val jpeg_url: String,
    val jpeg_width: Int, val jpeg_height: Int, val jpeg_file_size: Long, val rating: String, val has_children: Boolean,
    val parent_id: Long?, val status: String, val width: Int, val height: Int, val is_held: Boolean
)

object Konachan: ApiClient() {
    suspend fun fetchList(): List<KonachanImage> {
        return kotlin.runCatching {
            clientProxy.get(
                "https://konachan.com/post.json?tags=order%3Arandom%20rating:explicit"
            ).body<List<KonachanImage>>()
        }.onFailure {
            it.printStackTrace()
        }.getOrDefault(emptyList())
    }
}