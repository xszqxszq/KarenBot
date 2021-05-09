@file:Suppress("UNUSED", "MemberVisibilityCanBePrivate")
package tk.xszq.otomadbot.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import tk.xszq.otomadbot.bot
import tk.xszq.otomadbot.configMain

class QNAPApi(
    val addr: String = configMain.qnap.addr, val username: String = configMain.qnap.username,
    val password: String = configMain.qnap.password
): HTTPApi(HttpClient(CIO) {
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 114000
        connectTimeoutMillis = 114000
        socketTimeoutMillis = 114000
    }
}) {
    suspend fun login() {
        val response = client.submitForm<HttpResponse>("$addr/containerstation/api/v1/login",
            formParameters = Parameters.build {
                append("username", username)
                append("password", password)
            }
        )
        bot!!.logger.debug(response.readText())
    }
    suspend fun restart(containerId: String) {
        login()
        val response = client.put<HttpResponse>(
            "$addr/containerstation/api/v1/container/docker/$containerId/restart")
        bot!!.logger.debug(response.readText())
    }
}