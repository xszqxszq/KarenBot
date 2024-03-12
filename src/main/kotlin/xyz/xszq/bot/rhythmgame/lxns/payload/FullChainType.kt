package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
enum class FullChainType(val value: String) {
    FullChain("fullchain"), FullChain2("fullchain2")
}