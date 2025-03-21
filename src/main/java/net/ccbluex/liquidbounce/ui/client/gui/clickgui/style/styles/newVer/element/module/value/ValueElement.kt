package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value

import net.ccbluex.liquidbounce.features.value.Value
import net.ccbluex.liquidbounce.utils.MinecraftInstance

import java.awt.Color

abstract class ValueElement<T>(val value: Value<T>) : MinecraftInstance() {

    var valueHeight = 20F

    abstract fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float
    abstract fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float)
    open fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {}

    open fun onKeyPress(typed: Char, keyCode: Int): Boolean = false

    fun isDisplayable(): Boolean = value.stateDisplayable
}