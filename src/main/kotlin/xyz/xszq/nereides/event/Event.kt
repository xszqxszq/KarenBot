package xyz.xszq.nereides.event

import xyz.xszq.nereides.QQClient

interface Event {
    val client: QQClient
}