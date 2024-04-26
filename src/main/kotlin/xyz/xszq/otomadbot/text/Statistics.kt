package xyz.xszq.otomadbot.text

//object Statistics: CommandModule("", "statistics") {
//    val messageQueue: ConcurrentHashMap<Long, ConcurrentLinkedQueue<Int>> = ConcurrentHashMap()
//    val frequency: ConcurrentHashMap<Long, Int> = ConcurrentHashMap()
//    val lock = Mutex()
//    override suspend fun subscribe() {
//        events.subscribeAlways<MessagePostSendEvent<Contact>> {
//            if (this.isSuccess) {
//                val id = (if (this.target is Group) 1 else -1) * this.target.id
//                existOrCreate(id)
//                insertAndClean(id, this)
//            }
//        }
//    }
//    suspend fun existOrCreate(id: Long) {
//        lock.withLock {
//            if (!messageQueue.containsKey(id)) {
//                messageQueue[id] = ConcurrentLinkedQueue()
//                frequency[id] = 0
//            }
//        }
//    }
//    fun insert(id: Long, event: MessagePostSendEvent<Contact>) {
//        messageQueue[id]!!.add(event.source!!.time)
//    }
//    suspend fun clean(id: Long) {
//        lock.withLock {
//            while (System.currentTimeMillis() / 1000 - messageQueue[id]!!.peek() > 3600) {
//                messageQueue[id]!!.poll()
//            }
//            frequency[id] = max(frequency[id]!!, messageQueue[id]!!.size)
//        }
//    }
//    suspend fun insertAndClean(id: Long, event: MessagePostSendEvent<Contact>) {
//        insert(id, event)
//        clean(id)
//    }
//}