package com.block.gooseintellij.ui.components.common

import java.awt.Component
import java.awt.Container
import java.awt.FocusTraversalPolicy

class CustomFocusTraversalPolicy(private val components: List<Component>) : FocusTraversalPolicy() {
    override fun getComponentAfter(aContainer: Container, aComponent: Component): Component {
        val index = components.indexOf(aComponent)
        val nextIndex = (index + 1) % components.size
        return components[nextIndex]
    }

    override fun getComponentBefore(aContainer: Container, aComponent: Component): Component {
        val index = components.indexOf(aComponent)
        val prevIndex = if (index - 1 < 0) components.size - 1 else index - 1
        return components[prevIndex]
    }

    override fun getFirstComponent(aContainer: Container): Component = components.first()

    override fun getLastComponent(aContainer: Container): Component = components.last()

    override fun getDefaultComponent(aContainer: Container): Component = components.first()
}
