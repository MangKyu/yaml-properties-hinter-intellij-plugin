package com.mangkyu.yamlpropertieslens

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle

class PropertyPathRenderer(
    val paths: List<String>,
    val values: List<String?>,
    private val editor: Editor,
    private val indentChars: Int,
) : EditorCustomElementRenderer {

    private val displayLines: List<String> = paths.map { "# $it" }
    val copyText: String = paths.joinToString("\n")
    val propertiesText: String = paths.zip(values)
        .filter { (_, value) -> value != null }
        .joinToString("\n") { (path, value) -> "$path=$value" }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val fontMetrics = editor.contentComponent.getFontMetrics(editor.colorsScheme.getFont(EditorFontType.PLAIN))
        return fontMetrics.charWidth(' ') * indentChars + displayLines.maxOf { fontMetrics.stringWidth(it) } + 8
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return editor.lineHeight * displayLines.size
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        g.font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        val hintColor = YamlPropertiesLensSettings.getInstance().hintColor
        g.color = JBColor(hintColor, hintColor)
        val fontMetrics = g.fontMetrics
        val lineHeight = editor.lineHeight
        val x = targetRegion.x + fontMetrics.charWidth(' ') * indentChars
        for ((index, line) in displayLines.withIndex()) {
            val y = targetRegion.y + lineHeight * (index + 1) - fontMetrics.descent
            g.drawString(line, x, y)
        }
    }
}
