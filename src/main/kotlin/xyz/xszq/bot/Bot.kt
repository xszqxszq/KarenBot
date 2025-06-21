package xyz.xszq.bot

class Bot(
    val api: OpenAPI,
    val cos: TencentCos,
    val pluginLoader: PluginLoader
)