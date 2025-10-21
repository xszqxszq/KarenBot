package xyz.xszq.shinobu

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Element {
    val id: String?
    val margin: Spacing
    val padding: Spacing
    @Transient
    var parent: Container?
}