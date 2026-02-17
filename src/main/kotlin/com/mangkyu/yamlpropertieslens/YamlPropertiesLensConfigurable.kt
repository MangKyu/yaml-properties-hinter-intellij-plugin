package com.mangkyu.yamlpropertieslens

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ColorPanel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class YamlPropertiesLensConfigurable : Configurable {

    private var maxPathsSpinner: JSpinner? = null
    private var colorPanel: ColorPanel? = null

    override fun getDisplayName(): String = "YAML Properties Lens"

    override fun createComponent(): JComponent {
        val settings = YamlPropertiesLensSettings.getInstance()

        maxPathsSpinner = JSpinner(SpinnerNumberModel(settings.state.maxPaths, 1, 50, 1))
        colorPanel = ColorPanel().apply { selectedColor = settings.hintColor }

        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
        }

        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JLabel("Max paths to display:"), gbc)
        gbc.gridx = 1
        panel.add(maxPathsSpinner!!, gbc)

        gbc.gridx = 0; gbc.gridy = 1
        panel.add(JLabel("Hint color:"), gbc)
        gbc.gridx = 1
        panel.add(colorPanel!!, gbc)

        // push content to top-left
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1.0; gbc.weighty = 1.0
        panel.add(JPanel(), gbc)

        return panel
    }

    override fun isModified(): Boolean {
        val settings = YamlPropertiesLensSettings.getInstance()
        return maxPathsSpinner?.value != settings.state.maxPaths ||
            colorPanel?.selectedColor != settings.hintColor
    }

    override fun apply() {
        val settings = YamlPropertiesLensSettings.getInstance()
        settings.state.maxPaths = maxPathsSpinner?.value as? Int ?: 10
        val color = colorPanel?.selectedColor
        if (color != null) {
            settings.state.hintColorHex = "%06X".format(color.rgb and 0xFFFFFF)
        }
    }

    override fun reset() {
        val settings = YamlPropertiesLensSettings.getInstance()
        maxPathsSpinner?.value = settings.state.maxPaths
        colorPanel?.selectedColor = settings.hintColor
    }

    override fun disposeUIResources() {
        maxPathsSpinner = null
        colorPanel = null
    }
}
