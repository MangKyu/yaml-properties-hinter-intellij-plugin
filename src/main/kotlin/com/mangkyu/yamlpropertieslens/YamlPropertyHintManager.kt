package com.mangkyu.yamlpropertieslens

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.ui.awt.RelativePoint
import java.awt.datatransfer.StringSelection

class YamlPropertyHintManager : ProjectActivity {

    companion object {
        private val HINT_INSTALLED = Key.create<Boolean>("YamlPropertyHintInstalled")
    }

    override suspend fun execute(project: Project) {
        val parentDisposable = Disposer.newDisposable("YamlPropertyHintManager")
        Disposer.register(project as Disposable, parentDisposable)

        // Attach to already-open editors
        ApplicationManager.getApplication().invokeLater {
            for (editor in EditorFactory.getInstance().allEditors) {
                if (editor.project == project && isYamlFile(editor)) {
                    attachListeners(editor, project, parentDisposable)
                }
            }
        }

        // Attach to future editors
        EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) {
                val editor = event.editor
                if (editor.project != project) return
                if (!isYamlFile(editor)) return
                attachListeners(editor, project, parentDisposable)
            }
        }, parentDisposable)
    }

    private fun attachListeners(editor: Editor, project: Project, parentDisposable: Disposable) {
        if (editor.getUserData(HINT_INSTALLED) == true) return
        editor.putUserData(HINT_INSTALLED, true)

        val editorDisposable = Disposer.newDisposable("YamlPropertyHint-${editor.hashCode()}")
        Disposer.register(parentDisposable, editorDisposable)

        var currentInlay: Inlay<*>? = null
        val onUpdate = { inlay: Inlay<*>? ->
            currentInlay?.dispose()
            currentInlay = inlay
        }

        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                ApplicationManager.getApplication().invokeLater {
                    if (!editor.isDisposed) {
                        updateHint(editor, project, onUpdate)
                    }
                }
            }
        }, editorDisposable)

        editor.selectionModel.addSelectionListener(object : SelectionListener {
            override fun selectionChanged(e: SelectionEvent) {
                ApplicationManager.getApplication().invokeLater {
                    if (!editor.isDisposed) {
                        updateHint(editor, project, onUpdate)
                    }
                }
            }
        }, editorDisposable)

        var pendingCopyRenderer: PropertyPathRenderer? = null

        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mousePressed(event: EditorMouseEvent) {
                val point = event.mouseEvent.point
                val inlay = currentInlay
                pendingCopyRenderer = if (inlay != null && inlay.bounds?.contains(point) == true) {
                    inlay.renderer as? PropertyPathRenderer
                } else {
                    null
                }
            }

            override fun mouseClicked(event: EditorMouseEvent) {
                val renderer = pendingCopyRenderer ?: return
                pendingCopyRenderer = null

                if (renderer.propertiesText.isEmpty()) {
                    CopyPasteManager.getInstance().setContents(StringSelection(renderer.copyText))
                    val message = if (renderer.paths.size > 1) "Copied! (${renderer.paths.size} keys)" else "Copied!"
                    JBPopupFactory.getInstance()
                        .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                        .setFadeoutTime(1500)
                        .createBalloon()
                        .show(RelativePoint(event.mouseEvent), Balloon.Position.above)
                } else {
                    val items = listOf("Copy keys", "Copy keys with values")
                    JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(items)
                        .setRenderer(object : javax.swing.DefaultListCellRenderer() {
                            override fun getListCellRendererComponent(
                                list: javax.swing.JList<*>?, value: Any?, index: Int,
                                isSelected: Boolean, cellHasFocus: Boolean
                            ): java.awt.Component {
                                val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                                border = javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)
                                return comp
                            }
                        })
                        .setItemChosenCallback { chosen ->
                            val text = if (chosen == "Copy keys with values") renderer.propertiesText else renderer.copyText
                            CopyPasteManager.getInstance().setContents(StringSelection(text))
                        }
                        .createPopup()
                        .show(RelativePoint(event.mouseEvent))
                }
            }
        }, editorDisposable)

        // Show hint for initial caret position
        ApplicationManager.getApplication().invokeLater {
            updateHint(editor, project, onUpdate)
        }
    }

    private fun updateHint(editor: Editor, project: Project, callback: (Inlay<*>?) -> Unit) {
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        val selectionModel = editor.selectionModel
        val resolvedPaths: List<ResolvedPath>
        val anchorOffset: Int

        if (selectionModel.hasSelection()) {
            val startLine = editor.document.getLineNumber(selectionModel.selectionStart)
            val endLine = editor.document.getLineNumber(selectionModel.selectionEnd).let { line ->
                if (selectionModel.selectionEnd == editor.document.getLineStartOffset(line) && line > startLine) line - 1 else line
            }
            val resolved = mutableListOf<ResolvedPath>()
            val seenPaths = mutableSetOf<String>()

            for (line in startLine..endLine) {
                val lineStart = editor.document.getLineStartOffset(line)
                val lineEnd = editor.document.getLineEndOffset(line)
                val lineText = editor.document.getText(TextRange(lineStart, lineEnd))
                val firstNonWhitespace = lineText.indexOfFirst { !it.isWhitespace() }
                if (firstNonWhitespace < 0) continue

                val rp = YamlPathResolver.resolveWithValue(editor, project, lineStart + firstNonWhitespace)
                if (rp != null && seenPaths.add(rp.path)) {
                    resolved.add(rp)
                }
            }

            resolvedPaths = resolved.take(YamlPropertiesLensSettings.getInstance().state.maxPaths)
            anchorOffset = editor.document.getLineStartOffset(startLine)
        } else {
            val offset = editor.caretModel.offset
            val line = editor.document.getLineNumber(offset)
            val lineStart = editor.document.getLineStartOffset(line)
            val lineEnd = editor.document.getLineEndOffset(line)
            val lineText = editor.document.getText(TextRange(lineStart, lineEnd))
            if (lineText.isBlank()) {
                callback(null)
                return
            }
            val firstNonWhitespace = lineText.indexOfFirst { !it.isWhitespace() }
            val resolveOffset = if (firstNonWhitespace >= 0) lineStart + firstNonWhitespace else offset
            val rp = YamlPathResolver.resolveWithValue(editor, project, resolveOffset)
            if (rp == null) {
                callback(null)
                return
            }
            resolvedPaths = listOf(rp)
            anchorOffset = editor.document.getLineStartOffset(editor.document.getLineNumber(offset))
        }

        if (resolvedPaths.isEmpty()) {
            callback(null)
            return
        }

        val paths = resolvedPaths.map { it.path }
        val values = resolvedPaths.map { it.value }

        val anchorLine = editor.document.getLineNumber(anchorOffset)
        val anchorLineEnd = editor.document.getLineEndOffset(anchorLine)
        val anchorLineText = editor.document.getText(TextRange(anchorOffset, anchorLineEnd))
        val indentChars = anchorLineText.takeWhile { it.isWhitespace() }.length

        val inlay = editor.inlayModel.addBlockElement(
            anchorOffset, false, true, 0,
            PropertyPathRenderer(paths, values, editor, indentChars)
        )
        callback(inlay)
    }

    private fun isYamlFile(editor: Editor): Boolean {
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return false
        val ext = virtualFile.extension?.lowercase() ?: return false
        return ext == "yml" || ext == "yaml"
    }
}
