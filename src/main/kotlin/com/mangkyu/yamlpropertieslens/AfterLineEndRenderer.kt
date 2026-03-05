package com.mangkyu.yamlpropertieslens

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Graphics
import java.awt.Rectangle

class AfterLineEndRenderer(
    val path: String,
    val value: String?,
    private val editor: Editor,
) : EditorCustomElementRenderer {

    private val displayText: String = "# $path"
    val copyText: String = path
    val propertiesText: String? = if (value != null) "$path=$value" else null

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val fontMetrics = editor.contentComponent.getFontMetrics(editor.colorsScheme.getFont(EditorFontType.PLAIN))
        return fontMetrics.stringWidth(displayText) + 16
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        g.font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        val hintColor = YamlPropertiesLensSettings.getInstance().hintColor
        g.color = JBColor(hintColor, hintColor)
        val y = targetRegion.y + editor.lineHeight - g.fontMetrics.descent
        g.drawString(displayText, targetRegion.x + 8, y)
    }
}
