package xyz.xszq.bot.maimai.music

/**
 * Sync 状态
 */
enum class SyncStatus(val id: Int, val value: String) {
    None(0, "none"),
    FullSync(1, "fs"),
    FullSyncPlus(2, "fsp"),
    FullSyncDeluxe(3, "fsd"),
    FullSyncDeluxePlus(4, "fsdp"),
    Sync(5, "sync");

    companion object {
        /**
         * 根据 ID 匹配 Sync 状态
         *
         * @param id ID
         * @return Sync 状态
         */
        fun of(id: Int): SyncStatus = SyncStatus.entries.first { it.id == id }

        /**
         * 根据名称匹配 Sync 状态
         *
         * @param value 名称
         * @return Sync 状态
         */
        fun of(value: String?): SyncStatus = SyncStatus.entries.firstOrNull { it.value == value } ?: None
    }

    /**
     * 是否 Full Sync Deluxe
     */
    fun isFSD() = this == FullSyncDeluxe || this == FullSyncDeluxePlus

    /**
     * 是否 Full Sync
     */
    fun isFS() = this == FullSync || this == FullSyncPlus || isFSD()
}