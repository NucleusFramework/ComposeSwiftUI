package dev.nucleusframework.composeswiftui.idea

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * Live literals, like Android Studio's: the edits from the compiled text of a file to its current text when only
 * literals changed (strings, numbers, booleans), shown by the running preview host without a build.
 */
object LiteralDiff {
    private class Token(val type: IElementType, val start: Int, val text: String)

    /**
     * Edits from [compiled] to [current], or null when something else than a literal changed (that needs a build).
     * [functionAt] names the function around an offset of [current].
     */
    fun edits(fileName: String, compiled: String, current: String, functionAt: (Int) -> String?): List<LiteralEdit>? {
        val before = tokens(compiled)
        val after = tokens(current)
        if (before.size != after.size) return null
        val edits = ArrayList<LiteralEdit>()
        for (i in before.indices) {
            val old = before[i]
            val new = after[i]
            if (family(old.type) != family(new.type)) return null
            if (old.text == new.text) continue
            val kind = kind(old.type) ?: return null
            val oldValue = value(kind, old.text) ?: return null
            val newValue = value(kind, new.text) ?: return null
            edits += LiteralEdit(fileName, old.start, functionAt(new.start), kind, oldValue, newValue)
        }
        return edits
    }

    private fun tokens(text: String): List<Token> {
        val lexer = KotlinLexer()
        lexer.start(text)
        val tokens = ArrayList<Token>()
        while (true) {
            val type = lexer.tokenType ?: break
            // Formatting and comments don't change the program.
            if (type != KtTokens.WHITE_SPACE && type !in KtTokens.COMMENTS) {
                tokens += Token(type, lexer.tokenStart, text.substring(lexer.tokenStart, lexer.tokenEnd))
            }
            lexer.advance()
        }
        return tokens
    }

    /** Token kinds a literal edit may switch between: `true` / `false`, integer / decimal. */
    private fun family(type: IElementType): Any = kind(type) ?: type

    private fun kind(type: IElementType): String? = when (type) {
        KtTokens.REGULAR_STRING_PART -> "string"
        KtTokens.INTEGER_LITERAL, KtTokens.FLOAT_LITERAL -> "number"
        KtTokens.TRUE_KEYWORD, KtTokens.FALSE_KEYWORD -> "boolean"
        else -> null
    }

    /** The value the host reads: string parts as written, numbers in decimal. */
    private fun value(kind: String, text: String): String? = when (kind) {
        "number" -> number(text)?.toString()
        else -> text
    }

    private fun number(text: String): Number? {
        val clean = text.replace("_", "").lowercase().removeSuffix("ul").removeSuffix("u").removeSuffix("l")
        return when {
            clean.startsWith("0x") -> clean.substring(2).toLongOrNull(16)
            clean.startsWith("0b") -> clean.substring(2).toLongOrNull(2)
            clean.endsWith("f") -> clean.removeSuffix("f").toDoubleOrNull()
            else -> clean.toLongOrNull() ?: clean.toDoubleOrNull()
        }
    }
}
