package xyz.xszq.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.nereides.newTempFile
import java.awt.Color
import java.io.File

object LaTeX {
    suspend fun generateLaTeX(text: String): File {
        return withContext(Dispatchers.IO) {
            newTempFile().let { result ->
                TeXFormula(text).createPNG(
                    TeXConstants.STYLE_DISPLAY, 22.0F, result.absolutePath,
                    Color.WHITE, Color.BLACK)
                result
            }
        }
    }
}