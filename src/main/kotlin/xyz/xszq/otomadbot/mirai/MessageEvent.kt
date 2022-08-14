package xyz.xszq.otomadbot.mirai

import com.github.houbb.opencc4j.util.ZhConverterUtil.toSimple
import kotlinx.coroutines.withTimeout
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageContent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.isContextIdenticalWith
import xyz.xszq.otomadbot.kotlin.substringAfterPrefix
import xyz.xszq.otomadbot.kotlin.substringBeforeSuffix
import xyz.xszq.otomadbot.kotlin.toSimple

typealias MessageListener<T, R> = @MessageDsl suspend T.(String) -> R
@MessageDsl
internal fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.content(
    filter: M.(String) -> Boolean,
    onEvent: MessageListener<M, RR>
): Ret =
    subscriber(filter) { onEvent(this, it) }

/**
 * equals对于繁体增加支持的版本
 */
@MessageDsl
fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>
        .equalsTo(equals: String, ignoreCase: Boolean = true, trim: Boolean = true,
                  onEvent: MessageListener<M, R>
): Ret
        = (if (trim) equals.trim() else equals).let { toCheck ->
    content({ toSimple(if (trim) it.trim() else it).equals(toCheck, ignoreCase = ignoreCase) }) {
        onEvent(this, this.message.contentToString())
    }
}
/**
 * startsWith对于繁体增加支持的版本的内部实现
 */
internal fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.startsWithSimpleImpl(
    prefix: String,
    removePrefix: Boolean = true,
    trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret {
    return if (trim) {
        val toCheck = prefix.trim()
        content({ it.toSimple().lowercase().trimStart().startsWith(toCheck) }) {
            if (removePrefix) onEvent(message.contentToString().toSimple().lowercase().substringAfter(toCheck).trim(),
                message.contentToString().substringAfterPrefix(toCheck).trim())
            else onEvent(this, message.contentToString().toSimple().lowercase().trim(),
                message.contentToString().trim())
        }
    } else content({ it.toSimple().lowercase().startsWith(prefix) }) {
        if (removePrefix) onEvent(message.contentToString().toSimple().lowercase().removePrefix(prefix),
            message.contentToString().substringAfterPrefix(prefix).trim())
        else onEvent(this, message.contentToString().toSimple().lowercase(),
            message.contentToString().trim())
    }
}

/**
 * endsWith对于繁体增加支持的版本的内部实现
 */
internal fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.endsWithSimpleImpl(
    suffix: String,
    removeSuffix: Boolean = true,
    trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret {
    return if (trim) {
        val toCheck = suffix.trimEnd()
        content({ it.toSimple().lowercase().trimEnd().endsWith(toCheck) }) {
            if (removeSuffix) onEvent(this.message.contentToString().toSimple().lowercase().removeSuffix(toCheck).trim(),
                this.message.contentToString().substringBeforeSuffix(toCheck).trim())
            else onEvent(this, this.message.contentToString().toSimple().lowercase().trim(),
                this.message.contentToString().trim())
        }
    } else content({ it.toSimple().lowercase().endsWith(suffix) }) {
        if (removeSuffix) onEvent(message.contentToString().toSimple().lowercase().removeSuffix(suffix),
            message.contentToString().substringBeforeSuffix(suffix).trim())
        else onEvent(this, message.contentToString().toSimple().lowercase(),
            message.contentToString().trim())
    }

}

/**
 * startsWith对于繁体增加支持的版本
 * M.(简体化且小写后的参数, 原始参数) -> R
 */
@MessageDsl
fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.startsWithSimple(
    prefix: String, removePrefix: Boolean = true, trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret = startsWithSimpleImpl(prefix, removePrefix, trim, onEvent)

/**
 * endsWith对于繁体增加支持的版本
 * M.(简体化且小写后的参数, 原始参数) -> R
 */
@MessageDsl
fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.endsWithSimple(
    suffix: String, removeSuffix: Boolean = true, trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret = endsWithSimpleImpl(suffix, removeSuffix, trim, onEvent)

/**
 * 引用回复消息
 * @param message 欲回复的内容
 */
suspend fun MessageEvent.quoteReply(message: String) {
    subject.sendMessage(source.quote() + message)
}
/**
 * 引用回复消息
 * @param message 欲回复的内容
 */
suspend fun MessageEvent.quoteReply(message: MessageContent) {
    subject.sendMessage(source.quote() + message)
}
/**
 * 引用回复消息
 * @param message 欲回复的内容
 */
suspend fun MessageEvent.quoteReply(message: MessageChain) {
    subject.sendMessage(source.quote() + message)
}


/**
 * 比较两个 GroupMessageEvent 是否来自同一个群
 * @param another 另一个群
 */
fun GroupMessageEvent.isGroupIdenticalWith(another: GroupMessageEvent): Boolean {
    return this.group == another.group
}

@PublishedApi // inline, safe to remove in the future
internal inline fun <reified P : MessageEvent>
        P.createMapper(crossinline filter: suspend P.(P) -> Boolean): suspend (P) -> P? =
    mapper@{ event ->
        if (!event.isContextIdenticalWith(this)) return@mapper null
        if (!filter(event, event)) return@mapper null
        event
    }
@PublishedApi // inline, safe to remove in the future
internal inline fun <reified P : MessageEvent>
        P.createMapperForGroup(crossinline filter: suspend P.(P) -> Boolean): suspend (P) -> P? =
    mapper@{ event ->
        if (event !is GroupMessageEvent) return@mapper null
        if (!event.isGroupIdenticalWith(this as GroupMessageEvent)) return@mapper null
        if (!filter(event, event)) return@mapper null
        event
    }

/**
 * 获得该 sender 在 subject 下的下一条消息
 */
@JvmSynthetic
suspend inline fun <reified P : MessageEvent> P.nextMessageEvent(
    timeoutMillis: Long = -1,
    priority: EventPriority = EventPriority.MONITOR,
    noinline filter: suspend P.(P) -> Boolean = { true }
): MessageEvent {
    val mapper: suspend (P) -> P? = createMapper(filter)

    return (if (timeoutMillis == -1L) {
        GlobalEventChannel.syncFromEvent(priority, mapper)
    } else {
        withTimeout(timeoutMillis) {
            GlobalEventChannel.syncFromEvent(priority, mapper)
        }
    })
}
/**
 * 获得群内下一条消息
 */
@Suppress("FINAL_UPPER_BOUND")
@JvmSynthetic
suspend inline fun <reified P : GroupMessageEvent> P.nextGroupMessageEvent(
    timeoutMillis: Long = -1,
    priority: EventPriority = EventPriority.MONITOR,
    noinline filter: suspend P.(P) -> Boolean = { true }
): GroupMessageEvent {
    val mapper = createMapperForGroup(filter)

    return (if (timeoutMillis == -1L) {
        GlobalEventChannel.syncFromEvent(priority, mapper)
    } else {
        withTimeout(timeoutMillis) {
            GlobalEventChannel.syncFromEvent(priority, mapper)
        }
    })
}