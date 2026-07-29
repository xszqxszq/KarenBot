package xyz.xszq.bot.maimai.music

enum class SyncStatus(val id: Int, val value: String) {
    None(0, "none"),
    FullSync(1, "fs"),
    FullSyncPlus(2, "fsp"),
    FullSyncDeluxe(3, "fsd"),
    FullSyncDeluxePlus(4, "fsdp"),
    Sync(5, "sync");

    companion object {
        fun of(id: Int): SyncStatus = SyncStatus.entries.first { it.id == id }
        fun of(value: String?): SyncStatus = SyncStatus.entries.firstOrNull { it.value == value } ?: None
    }

    fun isFSD() = this == FullSyncDeluxe || this == FullSyncDeluxePlus
    fun isFS() = this == FullSync || this == FullSyncPlus || isFSD()
}