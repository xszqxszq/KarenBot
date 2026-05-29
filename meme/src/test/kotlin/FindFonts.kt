package xyz.xszq.bot

import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.junit.jupiter.api.Test

class FindFonts {
    @Test
    fun findFonts() {
        val mgr = FontMgr.default
        println("Total families: ${mgr.familiesCount}")

        val targets = listOf(
            "RoGSanSrfStd-Bd", "RoGSanSerifStd", "Ro GSan Serif Std", "Ro GSan Serif Std B",
            "Source Han Sans CN Bold", "Source Han Sans", "Source Han Serif CN",
            "Glow Sans SC Normal Heavy", "Glow Sans TC", "未来荧黑"
        )
        println("\n=== matchFamilyStyle results ===")
        targets.forEach { name ->
            val tf = mgr.matchFamilyStyle(name, FontStyle.NORMAL)
            println("  $name -> ${tf?.let { "OK (${it.familyName})" } ?: "null"}")
        }

        println("\n=== Keyword scan for known fonts ===")
        val keywords = listOf("RoG", "SanSrf", "GSan", "Glow", "Source Han", "Han Sans", "Han Serif",
            "Serif CN", "Serif SC", "未来")
        keywords.forEach { kw ->
            val matches = (0 until mgr.familiesCount).mapNotNull { i ->
                val name = mgr.getFamilyName(i)
                if (name.contains(kw, ignoreCase = true)) name else null
            }
            println("  '$kw': ${matches.ifEmpty { listOf("none") }}")
        }
    }
}
