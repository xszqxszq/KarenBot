package xyz.xszq.nereides.payload.websocket

object OpCode {
    const val Dispatch = 0
    const val Heartbeat = 1
    const val Identify = 2
    const val Resume = 6
    const val Reconnect = 7
    const val InvalidSession = 9
    const val Hello = 10
    const val HeartbeatACK = 11
    const val HTTPCallbackACK = 12
}