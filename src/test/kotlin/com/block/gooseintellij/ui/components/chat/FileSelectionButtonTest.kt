package com.block.gooseintellij.ui.components.chat

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.mock

class FileSelectionButtonTest {
    private lateinit var project: Project
    private lateinit var fileEditorManager: FileEditorManager
    private lateinit var onFileSelected: (String) -> Unit
    private lateinit var getExistingPills: () -> Map<FilePillComponent, String>
    private lateinit var openFiles: Array<VirtualFile>
    
    @BeforeEach
    fun setup() {
        project = mock(Project::class.java)
        fileEditorManager = mock(FileEditorManager::class.java)
        onFileSelected = mock { }
        getExistingPills = mock { }
        
        // Mock file editor manager instance
        `when`(FileEditorManager.getInstance(project)).thenReturn(fileEditorManager)
        
        // Create test files
        val file1 = mock(VirtualFile::class.java)
        val file2 = mock(VirtualFile::class.java)
        
        `when`(file1.path).thenReturn("/test/path/test1.kt")
        `when`(file1.name).thenReturn("test1.kt")
        `when`(file2.path).thenReturn("/test/path/test2.kt")
        `when`(file2.name).thenReturn("test2.kt")
        
        openFiles = arrayOf(file1, file2)
        `when`(fileEditorManager.openFiles).thenReturn(openFiles)
    }
    
    @Test
    fun `test FileSelectionButton filters out existing files`() {
        // Create a pill with one of the open files
        val existingFile = openFiles[0]
        val existingPill = FilePillComponent(project, existingFile, true) { }
        val existingPills = mapOf(existingPill to existingFile.path)
        
        // Set up getExistingPills to return our test pills
        `when`(getExistingPills.invoke()).thenReturn(existingPills)
        
        // Create button with our mocked dependencies
        val button = FileSelectionButton(project, onFileSelected, getExistingPills)
        
        // Get available files through reflection (since it's private)
        val availableFilesField = button.javaClass.getDeclaredField("availableFiles")
        availableFilesField.isAccessible = true
        val availableFiles = availableFilesField.get(button) as List<VirtualFile>?
        
        // Should only contain the second file
        assertNotNull(availableFiles)
        assertEquals(1, availableFiles!!.size)
        assertEquals(openFiles[1].path, availableFiles[0].path)
    }
    
    @Test
    fun `test FileSelectionButton shows all files when no existing pills`() {
        // Set up getExistingPills to return empty map
        `when`(getExistingPills.invoke()).thenReturn(emptyMap())
        
        // Create button with our mocked dependencies
        val button = FileSelectionButton(project, onFileSelected, getExistingPills)
        
        // Get available files through reflection
        val availableFilesField = button.javaClass.getDeclaredField("availableFiles")
        availableFilesField.isAccessible = true
        val availableFiles = availableFilesField.get(button) as List<VirtualFile>?
        
        // Should contain both files
        assertNotNull(availableFiles)
        assertEquals(2, availableFiles!!.size)
        assertTrue(availableFiles.map { it.path }.containsAll(openFiles.map { it.path }))
    }
    
}
