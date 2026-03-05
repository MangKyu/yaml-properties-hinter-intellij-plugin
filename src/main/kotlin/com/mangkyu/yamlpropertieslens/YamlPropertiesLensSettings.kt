package com.mangkyu.yamlpropertieslens

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.awt.Color

@State(name = "YamlPropertiesLensSettings", storages = [Storage("YamlPropertiesLens.xml")])
class YamlPropertiesLensSettings : PersistentStateComponent<YamlPropertiesLensSettings.State> {

    data class State(
        var maxPaths: Int = 10,
        var hintColorHex: String = "6A8759",
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    val hintColor: Color
        get() = try {
            Color(myState.hintColorHex.toLong(16).toInt())
        } catch (_: NumberFormatException) {
            Color(0x6A8759)
        }

    companion object {
        fun getInstance(): YamlPropertiesLensSettings {
            return try {
                ApplicationManager.getApplication().getService(YamlPropertiesLensSettings::class.java)
            } catch (_: ClassCastException) {
                // During dynamic plugin reload, the old classloader's instance may conflict
                YamlPropertiesLensSettings()
            }
        }
    }
}
