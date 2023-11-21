package xyz.xszq.nereides

import kotlinx.coroutines.runBlocking

class Bot(
    appId: String,
    clientSecret: String,
    easyToken: String
) : QQClient(appId, clientSecret,
    easyToken) {
    fun launch() {
        runBlocking {
            listen()
        }
    }
}