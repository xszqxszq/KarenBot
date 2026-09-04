package xyz.xszq.bot

import kotlinx.serialization.json.Json

/**
 * 全局 JSON 对象
 */
val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
}