package com.block.gooseintellij.ui.components.chat

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

class FileSelectionButton(
  private val project: Project,
  private val onFileSelected: (VirtualFile) -> Unit,
  private val getExistingPills: () -> Map<FilePillComponent, String> = { emptyMap() }
) : JPanel() {
  private var popup: JBPopup? = null
  private val fileEditorManagerListener = object : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
      refreshPopupIfVisible()
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
      refreshPopupIfVisible()
    }
  }

  companion object {
    private val BACKGROUND_COLOR = JBColor(
      Color(240, 240, 240),  // Light theme
      Color(60, 63, 65)      // Dark theme
    )
    private val HOVER_COLOR = JBColor(
      Color(230, 230, 230),  // Light theme
      Color(70, 73, 75)      // Dark theme
    )
    private const val ARC_SIZE = 12
  }

  private var isHovered = false

  private fun refreshPopupIfVisible() {
    if (popup?.isVisible == true) {
      popup?.cancel()
      showFilePopup()
    }
  }

  init {
    layout = BorderLayout()
    isOpaque = false
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener)

    val label = JBLabel("", AllIcons.General.Add, JBLabel.LEFT).apply {
      border = JBUI.Borders.empty(4, 8)
      foreground = JBColor.foreground()
    }

    add(label, BorderLayout.CENTER)

    addMouseListener(object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent) {
        isHovered = true
        repaint()
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      }

      override fun mouseExited(e: MouseEvent) {
        isHovered = false
        repaint()
        cursor = Cursor.getDefaultCursor()
      }

      override fun mouseClicked(e: MouseEvent) {
        showFilePopup()
      }
    })
  }

  private fun showFilePopup() {
    val openFiles = FileEditorManager.getInstance(project).openFiles
    val existingPills = getExistingPills()

    // Filter out files that are already in pills
    val availableFiles = openFiles.filter { file ->
      !FilePillComponent.hasFilePathInPills(file.path, existingPills)
    }

    if (availableFiles.isEmpty()) {
      return
    }

    val content = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      border = JBUI.Borders.empty(6)  // Increased outer padding
      background = JBColor.background()

      availableFiles.forEachIndexed { index, file ->
        if (index > 0) {
          // Add vertical spacing between pills
          add(Box.createVerticalStrut(4))
        }

        add(FilePillComponent(project, file) { _ ->
          onFileSelected(file)
          popup!!.closeOk(null)
        })
      }
    }

    popup = JBPopupFactory.getInstance()
      .createComponentPopupBuilder(content, null)
      .setRequestFocus(true)
      .setResizable(false)
      .setMovable(false)
      .createPopup()
    popup!!.showUnderneathOf(this)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as Graphics2D

    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.color = if (isHovered) HOVER_COLOR else BACKGROUND_COLOR
      g2.fillRoundRect(0, 0, width, height, ARC_SIZE, ARC_SIZE)
    } finally {
      g2.dispose()
    }
  }
}