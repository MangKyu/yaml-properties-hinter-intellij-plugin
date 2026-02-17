package com.mangkyu.yamlpropertieslens

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

data class ResolvedPath(val path: String, val value: String?)

object YamlPathResolver {

    fun resolve(editor: Editor, project: Project, offset: Int): String? {
        return resolveWithValue(editor, project, offset)?.path
    }

    fun resolveWithValue(editor: Editor, project: Project, offset: Int): ResolvedPath? {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? YAMLFile ?: return null
        val element = psiFile.findElementAt(offset) ?: return null
        val keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java) ?: return null

        val parts = mutableListOf<String>()
        var current: YAMLKeyValue? = keyValue
        while (current != null) {
            parts.add(0, current.keyText)
            current = PsiTreeUtil.getParentOfType(current, YAMLKeyValue::class.java)
        }
        val path = parts.joinToString(".")
        val value = when (val v = keyValue.value) {
            is YAMLScalar -> v.textValue
            null -> null
            else -> v.text.trim()
        }
        return ResolvedPath(path, value)
    }
}
