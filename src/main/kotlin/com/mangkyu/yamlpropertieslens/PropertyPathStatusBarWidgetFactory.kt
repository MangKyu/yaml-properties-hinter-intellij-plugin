package com.mangkyu.yamlpropertieslens

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseEvent

class PropertyPathStatusBarWidgetFactory : StatusBarWidgetFactory {

    companion object {
        const val WIDGET_ID = "com.mangkyu.yamlpropertieslens.statusbar"
    }

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "YAML Property Path"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = PropertyPathStatusBarWidget()
}

class PropertyPathStatusBarWidget : StatusBarWidget, StatusBarWidget.TextPresentation {

    companion object {
        private var currentText: String = ""
        private var currentCopyText: String = ""

        fun updateText(text: String, copyText: String) {
            currentText = text
            currentCopyText = copyText
        }
    }

    private var statusBar: StatusBar? = null

    override fun ID(): String = PropertyPathStatusBarWidgetFactory.WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = currentText

    override fun getTooltipText(): String = if (currentCopyText.isNotEmpty()) "Click to copy: $currentCopyText" else ""

    override fun getAlignment(): Float = Component.LEFT_ALIGNMENT

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        if (currentCopyText.isNotEmpty()) {
            CopyPasteManager.getInstance().setContents(StringSelection(currentCopyText))
        }
    }

    fun update() {
        statusBar?.updateWidget(PropertyPathStatusBarWidgetFactory.WIDGET_ID)
    }

    override fun dispose() {
        statusBar = null
    }
}
