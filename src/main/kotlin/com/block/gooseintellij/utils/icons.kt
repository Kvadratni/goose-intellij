package com.block.gooseintellij.utils

import com.intellij.openapi.util.IconLoader

object GooseIcons {
  @JvmField
  val GooseWindow = IconLoader.getIcon("/icons/toolWindowIcon.svg", javaClass)

  @JvmField
  val GooseAction = IconLoader.getIcon("/icons/actionIcon.svg", javaClass)

  val SendToGoose = IconLoader.getIcon("/icons/send.svg", javaClass)
  val SendToGooseDisabled = IconLoader.getIcon("/icons/send_disabled.svg", javaClass)
}
