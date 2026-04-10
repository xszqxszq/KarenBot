package xyz.xszq.bot

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.userAgent

suspend fun main() {
    val client = HttpClient {
        followRedirects = false
    }
    println(client.get() {
    }.headers[HttpHeaders.Location])
}