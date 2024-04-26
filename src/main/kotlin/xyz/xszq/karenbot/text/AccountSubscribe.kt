package xyz.xszq.karenbot.text

import xyz.xszq.KarenBot
import xyz.xszq.karenbot.SafeYamlConfig

@kotlinx.serialization.Serializable
enum class SubscribeTaskType {
    Twitter, BiliBili, YouTube
}
@kotlinx.serialization.Serializable
class SubscribeTask(val type: SubscribeTaskType, val targetId: String, val groups: List<Long>, val interval: Long)
@kotlinx.serialization.Serializable
class SubscribeTaskConfigData(val tasks: MutableMap<String, SubscribeTask>)

object SubscribeTaskConfig: SafeYamlConfig<SubscribeTaskConfigData>(KarenBot, "subscribe",
    SubscribeTaskConfigData(mutableMapOf()))

@kotlinx.serialization.Serializable
class TaskFetchedData(val title: String, val description: String, val link: String, val datetime: Long)

//object AccountSubscribe: CommandModule("动态推送", "subscribe") {
//    val tasks = ConcurrentHashMap(mutableMapOf<String, SubscribeTask>())
//    val data = ConcurrentHashMap(mutableMapOf<String, List<TaskFetchedData>>())
//    val parser = RssStandardParser()
//    val fmt = DateFormat("EEE, dd MMM yyyy HH:mm:ss z")
//    override suspend fun subscribe() {
//        SubscribeTaskConfig.data.tasks.forEach { (id, task) ->
//            data[id] = listOf()
//            launch(Dispatchers.IO) {
//                while (true) {
//                    val url = when (task.type) {
//                        SubscribeTaskType.Twitter -> "https://rsshub.app/twitter/user/${task.targetId}"
//                        SubscribeTaskType.BiliBili -> "https://rsshub.app/bilibili/user/dynamic/${task.targetId}"
//                        SubscribeTaskType.YouTube -> "https://rsshub.app/youtube/channel/${task.targetId}"
//                    }
//                    kotlin.runCatching {
//                        val response = NetworkUtils.clientProxy.get(url)
//                        if (response.status == HttpStatusCode.OK) {
//                            val rss = parser.parse(response.bodyAsText())
//                            val newData = rss.items!!.map {
//                                TaskFetchedData(it.title ?: "", it.description ?: "",
//                                    it.link!!, fmt.parse(it.pubDate!!).utc.unixMillis.toLong())
//                            }
//                            data[id]!!.forEach {
//                                println(it.datetime)
//                            }
//                            newData.forEach {
//                                println(it.datetime)
//                            }
//                            // If this is newer
//                            if (data[id]!!.isEmpty() || data[id]!!.first().datetime < newData.first().datetime) {
//                                val firstTime = data[id]!!.isEmpty()
//                                println(firstTime)
//                                data[id] = newData
//                                if (!firstTime) {
//                                    val newest = newData.first()
//                                    task.groups.forEach { group ->
//                                        OtomadBotCore.validator.bots.firstNotNullOfOrNull { it.groups[group] }
//                                            ?.sendMessage(
//                                                "$id 更新了：\n${newest.link}\n${
//                                                    if (newest.description.isEmpty()) newest.description
//                                                    else newest.title
//                                                }\n${DateTimeTz.fromUnix(newest.datetime).format("yyyy年MMM月dd日 HH:mm")}"
//                                            )
//                                    }
//                                }
//                            }
//                        }
//                    }.onFailure {
//                        it.printStackTrace()
//                    }
//                    println("Check finished")
//                    delay(task.interval)
//                }
//            }
//            tasks[id] = task
//        }
//    }
//}