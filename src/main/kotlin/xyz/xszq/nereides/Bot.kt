package xyz.xszq.nereides

import kotlinx.coroutines.runBlocking

class Bot(
    appId: String,
    clientSecret: String,
    easyToken: String,
    sandbox: Boolean
) : QQClient(appId, clientSecret, easyToken, sandbox) {
    init {
        bot = this
    }
    fun launch() {
        runBlocking {
            listen()
        }
    }
}