package xyz.xszq.bot.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 数据库访问的并发上限
 */
const val DB_PARALLELISM = 8

private val CPU_PARALLELISM =
    Runtime.getRuntime().availableProcessors().coerceAtLeast(2)

/**
 * 数据库访问使用的调度器
 */
val dbDispatcher: CoroutineDispatcher =
    Dispatchers.IO.limitedParallelism(DB_PARALLELISM)

/**
 * CPU 密集任务使用的调度器
 */
val cpuDispatcher: CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(CPU_PARALLELISM)