package xyz.xszq.bot.image

import korlibs.io.file.VfsFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.nereides.getTempFile
import java.awt.Color

object LaTeX {
    suspend fun generateLaTeX(text: String): VfsFile = MemeGenerator.semaphore.withPermit {
        return withContext(Dispatchers.IO) {
            val result = getTempFile()
            TeXFormula(text).createPNG(
                TeXConstants.STYLE_DISPLAY, 22.0F,
                result.absolutePath,
                Color.WHITE,
                Color.BLACK
            )
            result
        }
    }
}